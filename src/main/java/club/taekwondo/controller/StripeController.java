package club.taekwondo.controller;

import club.taekwondo.entity.jpa.Echeance;
import club.taekwondo.entity.jpa.Paiement;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.security.JwtUtil;
import club.taekwondo.service.StripeService;
import club.taekwondo.service.jpa.PaiementService;
import club.taekwondo.service.jpa.UtilisateurService;
import com.stripe.exception.IdempotencyException;
import com.stripe.model.Charge;
import com.stripe.model.PaymentIntent;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/stripe")
@CrossOrigin(origins = "*")
public class StripeController {

    private final PaiementService paiementService;
    private final UtilisateurService utilisateurService;
    private final JwtUtil jwtUtil;
    private final StripeService stripeService;

    public StripeController(PaiementService paiementService,
                            UtilisateurService utilisateurService,
                            JwtUtil jwtUtil,
                            StripeService stripeService) {
        this.paiementService = paiementService;
        this.utilisateurService = utilisateurService;
        this.jwtUtil = jwtUtil;
        this.stripeService = stripeService;
    }

    /**
     * Création (ou réutilisation) d'un PaymentIntent Stripe.
     * Mappings conservés pour compat : /payment-intent et /create-payment-intent
     */
    @PostMapping({"/payment-intent", "/create-payment-intent"})
    public ResponseEntity<?> createPaymentIntent(@RequestBody Map<String, Object> body,
                                                 @RequestHeader HttpHeaders headers,
                                                 HttpServletRequest request) {
        System.out.println("=============================");
        System.out.println("🚀 [StripeController] createPaymentIntent déclenché");
        System.out.println("Requête brute reçue: " + body);

        try {
            // --------- Auth ---------
            String auth = headers.getFirst(HttpHeaders.AUTHORIZATION);
            String jwt = (auth != null && auth.startsWith("Bearer ")) ? auth.substring(7) : null;

            Utilisateur utilisateur = null;
            if (jwt != null) {
                String email = jwtUtil.extractEmail(jwt);
                System.out.printf("🔑 JWT décodé -> email: %s%n", email);
                utilisateur = utilisateurService.findByEmail(email).orElse(null);
            }

            Long paiementId = asLong(body.get("paiementId"));
            final Long reqEcheanceId = asLong(body.get("echeanceId"));
            String customerEmail = asString(body.get("customerEmail"));
            if (paiementId == null) return ResponseEntity.badRequest().body(Map.of("error", "paiementId manquant"));

            Optional<Paiement> optPaiement = paiementService.getById(paiementId);
            if (optPaiement.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error","Paiement introuvable"));
            Paiement p = optPaiement.get();

            // --- Autorisation ---
            boolean autorise = false;
            try {
                autorise = (utilisateur != null) && (
                        (p.getUtilisateur() != null && Objects.equals(p.getUtilisateur().getId(), utilisateur.getId())) ||
                        (p.getMembre() != null && p.getMembre().getParent() != null &&
                         Objects.equals(p.getMembre().getParent().getId(), utilisateur.getId()))
                );
            } catch (Exception ignored) {}
            System.out.printf("🔒 Vérif autorisation -> %s%n", autorise ? "OK" : "REFUSÉ");
            if (!autorise) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error","Non autorisé pour ce paiement."));

            // ------------ Déterminer l’échéance cible + montant ------------
            Echeance cible = null;
            Integer echeanceNumero = null;
            long amountInCents;
            String typeCourant = (p.getType() == null) ? "UNIQUE" : p.getType().toUpperCase(Locale.ROOT);

            if ("ECHELONNE".equalsIgnoreCase(typeCourant)) {
                List<Echeance> echeances = (p.getEcheances() == null) ? List.of() : p.getEcheances();
                if (echeances.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error","Aucune échéance définie pour ce paiement."));

                if (reqEcheanceId != null) {
                    cible = echeances.stream().filter(e -> Objects.equals(e.getId(), reqEcheanceId)).findFirst().orElse(null);
                    if (cible == null) return ResponseEntity.badRequest().body(Map.of("error","Échéance spécifiée introuvable."));
                } else {
                    Optional<Echeance> firstUnpaid = echeances.stream()
                            .filter(e -> !"payé".equalsIgnoreCase(safe(e.getStatut())))
                            .sorted(Comparator.comparingInt(Echeance::getNumero))
                            .findFirst();
                    if (firstUnpaid.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error","Aucune échéance à payer."));
                    cible = firstUnpaid.get();
                }
                echeanceNumero = cible.getNumero();
                amountInCents = Math.round(safeDouble(cible.getMontant()) * 100.0);
                System.out.printf("💰 Montant calculé pour échéance #%d = %d cents%n", echeanceNumero, amountInCents);
            } else {
                if (p.getMontantTotal() == null || p.getMontantTotal() <= 0)
                    return ResponseEntity.badRequest().body(Map.of("error","Montant du paiement invalide."));
                amountInCents = Math.round(p.getMontantTotal() * 100.0);
                System.out.printf("💰 Montant total calculé = %d cents%n", amountInCents);
            }

            final Long finalEcheanceId = ("ECHELONNE".equalsIgnoreCase(typeCourant) && cible != null)
                    ? (reqEcheanceId != null ? reqEcheanceId : cible.getId())
                    : null;

            // ------------ Réutilisation éventuelle d’un PI existant (confirmable) ------------
            PaymentIntent existing = null;
            if (cible != null && cible.getReference() != null && !cible.getReference().isBlank()) {
                try {
                    existing = PaymentIntent.retrieve(cible.getReference());
                    String status = existing.getStatus();
                    System.out.printf("ℹ️ PI existant (échéance)=%s, status=%s%n", existing.getId(), status);
                    boolean confirmable = "requires_payment_method".equals(status)
                            || "requires_confirmation".equals(status)
                            || "requires_action".equals(status);
                    if (confirmable) {
                        System.out.printf("↩️ Réutilisation du PI confirmable: %s%n", existing.getId());
                        System.out.printf("==============================%n%n");
                        return ResponseEntity.ok(Map.of(
                                "clientSecret", existing.getClientSecret(),
                                "paymentIntentId", existing.getId() // ✅ exposé au front
                        ));
                    }
                } catch (Exception ex) {
                    System.out.println("⚠️ Récupération PI échéance échouée, on ignore. Cause: " + ex.getMessage());
                }
            }

            // ------------ Idempotency key (inclut echeanceId) ------------
            String idemKeySuffix = "ECHELONNE".equalsIgnoreCase(typeCourant)
                    ? ("ech-" + echeanceNumero + "-" + (finalEcheanceId != null ? finalEcheanceId : "x") + "-" + amountInCents)
                    : ("unique-" + amountInCents);
            String idempotencyKey = "paiement-" + paiementId + "-" + idemKeySuffix;

            if (existing != null) {
                String status = existing.getStatus();
                if ("succeeded".equals(status) || "canceled".equals(status) || "requires_capture".equals(status) || "processing".equals(status)) {
                    idempotencyKey = idempotencyKey + "-v" + System.currentTimeMillis();
                    System.out.printf("🔁 Ancien PI non réutilisable (status=%s) → nouvelle Idempotency-Key: %s%n", status, idempotencyKey);
                }
            }

            // ------------ Email pour reçu Stripe natif ------------
            if (customerEmail == null || customerEmail.isBlank()) {
                // 1) email du propriétaire du paiement si dispo
                if (p.getUtilisateur() != null && p.getUtilisateur().getEmail() != null) {
                    String e = p.getUtilisateur().getEmail().trim();
                    if (!e.isEmpty()) customerEmail = e;
                }
                // 2) sinon email de l'utilisateur authentifié
                if ((customerEmail == null || customerEmail.isBlank()) && utilisateur != null && utilisateur.getEmail() != null) {
                    String e = utilisateur.getEmail().trim();
                    if (!e.isEmpty()) customerEmail = e;
                }
            }

            // ------------ Construction de la requête Stripe ------------
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("paiementId", String.valueOf(paiementId));
            metadata.put("type", typeCourant);
            if (finalEcheanceId != null) metadata.put("echeanceId", String.valueOf(finalEcheanceId));
            metadata.put("membreId", p.getMembre() != null ? String.valueOf(p.getMembre().getId()) : "");
            metadata.put("utilisateurId", p.getUtilisateur() != null ? String.valueOf(p.getUtilisateur().getId()) : "");

            Map<String, Object> piReq = new HashMap<>();
            piReq.put("amount", amountInCents);
            piReq.put("currency", "eur");
            piReq.put("metadata", metadata);
            piReq.put("description", "Cotisation / Paiement #" + paiementId);

            // ✅ 3DS automatique
            piReq.put("automatic_payment_methods", Map.of("enabled", true));

            // 💌 Reçu email Stripe (si dispo)
            if (customerEmail != null && !customerEmail.isBlank()) {
                piReq.put("receipt_email", customerEmail);
            }

            // 👉 Pour FORCER 3DS en test, décommente ce bloc :
            // Map<String, Object> cardOpts = Map.of("request_three_d_secure", "any");
            // Map<String, Object> pmo = Map.of("card", cardOpts);
            // piReq.put("payment_method_options", pmo);

            System.out.printf("🟢 Envoi création PaymentIntent à Stripe avec: %s%n", piReq);
            System.out.printf("🔁 Idempotency-Key utilisée: %s%n", idempotencyKey);

            PaymentIntent paymentIntent;
            try {
                paymentIntent = stripeService.createPaymentIntentWithMetadata(piReq, idempotencyKey);
            } catch (IdempotencyException idemEx) {
                System.out.println("⚠️ IdempotencyException: " + idemEx.getMessage() + " → retry avec clé versionnée");
                String retryKey = idempotencyKey + "-v" + System.currentTimeMillis();
                paymentIntent = stripeService.createPaymentIntentWithMetadata(piReq, retryKey);
            }

            // ------------ Persister l’ID du PI ------------
            try {
                if ("ECHELONNE".equalsIgnoreCase(typeCourant) && finalEcheanceId != null) {
                    paiementService.saveEcheanceReference(finalEcheanceId, paymentIntent.getId());
                    System.out.printf("💾 PaymentIntent ID sauvegardé sur l'échéance %d: %s%n", finalEcheanceId, paymentIntent.getId());
                } else {
                    p.setPaymentIntentId(paymentIntent.getId());
                    paiementService.save(p);
                    System.out.printf("💾 PaymentIntent ID sauvegardé sur le paiement (unique): %s%n", paymentIntent.getId());
                }
            } catch (Exception ex) {
                System.out.println("⚠️ Erreur sauvegarde PaymentIntent en BDD: " + ex.getMessage());
            }

            System.out.println("✅ ClientSecret renvoyé au front");
            System.out.printf("==============================%n%n");
            return ResponseEntity.ok(Map.of(
                    "clientSecret", paymentIntent.getClientSecret(),
                    "paymentIntentId", paymentIntent.getId() // ✅ pour sync côté front
            ));

        } catch (Exception e) {
            System.out.println("❌ Exception attrapée dans createPaymentIntent:");
            e.printStackTrace();
            System.out.printf("==============================%n%n");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 🆕 Endpoint de synchronisation (utile en DEV local quand le webhook ne peut pas appeler ton localhost).
     * Body attendu: { "paymentIntentId": "pi_xxx" }
     */
    @PostMapping("/sync-payment")
    public ResponseEntity<?> syncPayment(@RequestBody Map<String, Object> body,
                                         @RequestHeader HttpHeaders headers) {
        try {
            String piId = asString(body.get("paymentIntentId"));
            if (piId == null || piId.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "paymentIntentId manquant"));
            }

            PaymentIntent pi = PaymentIntent.retrieve(piId);
            if (!"succeeded".equalsIgnoreCase(pi.getStatus())) {
                return ResponseEntity.badRequest().body(Map.of("error", "PaymentIntent non succeeded", "status", pi.getStatus()));
            }

            Map<String, String> md = pi.getMetadata();
            if (md == null) return ResponseEntity.badRequest().body(Map.of("error", "metadata manquantes"));
            Long paiementId = asLong(md.get("paiementId"));
            Long echeanceId = asLong(md.get("echeanceId"));

            if (paiementId == null) return ResponseEntity.badRequest().body(Map.of("error","paiementId manquant dans metadata"));

            Optional<Paiement> opt = paiementService.getById(paiementId);
            if (opt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error","Paiement introuvable"));
            Paiement paiement = opt.get();

            // Déjà soldé ? on renvoie OK
            if ("payé".equalsIgnoreCase(paiement.getStatut()) &&
                (paiement.getMontantRestant() == null || paiement.getMontantRestant() <= 0.0)) {
                return ResponseEntity.ok(Map.of("status","already-paid"));
            }

            // Marquer payé
            paiement.setModePaiement("CB");
            if ("ECHELONNE".equalsIgnoreCase(paiement.getType()) &&
                paiement.getEcheances() != null && !paiement.getEcheances().isEmpty()) {

                // si echeanceId présent -> cible précise, sinon 1ère impayée
                Echeance cible = null;
                if (echeanceId != null) {
                    for (Echeance e : paiement.getEcheances()) {
                        if (Objects.equals(e.getId(), echeanceId)) { cible = e; break; }
                    }
                }
                if (cible == null) {
                    Optional<Echeance> firstUnpaid = paiement.getEcheances().stream()
                            .filter(e -> !"payé".equalsIgnoreCase(e.getStatut()))
                            .sorted(Comparator.comparingInt(Echeance::getNumero))
                            .findFirst();
                    if (firstUnpaid.isEmpty()) return ResponseEntity.ok(Map.of("status","no-unpaid-installment"));
                    cible = firstUnpaid.get();
                }

                if (!"payé".equalsIgnoreCase(cible.getStatut())) {
                    cible.setStatut("payé");
                    cible.setDatePaiementReel(LocalDate.now());
                    cible.setModePaiement("CB");
                }

                long nbRestantes = paiement.getEcheances().stream()
                        .filter(e -> !"payé".equalsIgnoreCase(e.getStatut())).count();
                double restant = paiement.getEcheances().stream()
                        .filter(e -> !"payé".equalsIgnoreCase(e.getStatut()))
                        .mapToDouble(e -> Optional.ofNullable(e.getMontant()).orElse(0.0)).sum();

                paiement.setMontantRestant(restant);
                paiement.setEcheancesRestantes((int) nbRestantes);
                paiement.setStatut(nbRestantes == 0 ? "payé" : "en attente");
            } else {
                paiement.setStatut("payé");
                paiement.setMontantRestant(0.0);
                paiement.setDatePaiement(LocalDate.now());
            }

            paiementService.save(paiement);
            return ResponseEntity.ok(Map.of("status","synced"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 🧾 Redirection vers le reçu Stripe hébergé pour un paiement donné.
     * On tente d'abord la dernière échéance PAYÉE (référence = PI), sinon le PI stocké sur le paiement.
     */
    @GetMapping("/receipt/{paiementId}")
    public ResponseEntity<?> redirectToStripeReceipt(@PathVariable Long paiementId) {
        try {
            Paiement p = paiementService.getById(paiementId)
                    .orElseThrow(() -> new IllegalArgumentException("Paiement introuvable"));

            // 1) Trouver le PaymentIntent ID (échéance payée en priorité, sinon paiement unique)
            String piId = null;
            if (p.getEcheances() != null && !p.getEcheances().isEmpty()) {
                for (int i = p.getEcheances().size() - 1; i >= 0; i--) {
                    var e = p.getEcheances().get(i);
                    if ("payé".equalsIgnoreCase(e.getStatut())
                            && e.getReference() != null && !e.getReference().isBlank()) {
                        piId = e.getReference();
                        break;
                    }
                }
            }
            if (piId == null) piId = p.getPaymentIntentId();
            if (piId == null || piId.isBlank()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Aucun PaymentIntent associé"));
            }

            // 2) Récupérer le PI (sans getCharges())
            PaymentIntent pi = PaymentIntent.retrieve(piId);

            String receiptUrl = null;

            // a) Chemin principal : latest_charge → Charge → receipt_url
            String latestChargeId = pi.getLatestCharge();
            if (latestChargeId != null && !latestChargeId.isBlank()) {
                Charge charge = Charge.retrieve(latestChargeId);
                if (charge != null) receiptUrl = charge.getReceiptUrl();
            }

            // b) Optionnel : si latest_charge a été étendu ailleurs
            if ((receiptUrl == null || receiptUrl.isBlank())) {
                Object latestObj = pi.getLatestChargeObject();
                if (latestObj instanceof Charge) {
                    receiptUrl = ((Charge) latestObj).getReceiptUrl();
                }
            }

            if (receiptUrl == null || receiptUrl.isBlank()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Aucun reçu disponible"));
            }
            return ResponseEntity.status(302).location(URI.create(receiptUrl)).build();

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    // ------------------- utils -------------------
    private static Long asLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        try { return Long.parseLong(String.valueOf(o)); } catch (Exception ignored) { return null; }
    }

    private static String asString(Object o) {
        return (o == null) ? null : String.valueOf(o);
    }

    private static String safe(String s) {
        return (s == null) ? "" : s;
    }

    private static double safeDouble(Double d) {
        return (d == null) ? 0.0 : d;
    }
}
