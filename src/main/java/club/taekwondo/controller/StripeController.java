package club.taekwondo.controller;

import club.taekwondo.entity.jpa.Echeance;
import club.taekwondo.entity.jpa.Paiement;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.service.StripeService;
import club.taekwondo.service.jpa.EmailService;
import club.taekwondo.service.jpa.PaiementAccessService;
import club.taekwondo.service.jpa.PaiementService;
import com.stripe.exception.IdempotencyException;
import com.stripe.model.Charge;
import com.stripe.model.PaymentIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/stripe")
public class StripeController {

    private static final Logger log = LoggerFactory.getLogger(StripeController.class);

    private static final Set<String> CONFIRMABLE_STATUSES = Set.of(
            "requires_payment_method", "requires_confirmation", "requires_action"
    );

    private final PaiementService paiementService;
    private final PaiementAccessService paiementAccessService;
    private final StripeService stripeService;
    private final EmailService emailService;

    @Value("${stripe.public.key:}")
    private String publishableKey;

    public StripeController(PaiementService paiementService,
                            PaiementAccessService paiementAccessService,
                            StripeService stripeService,
                            EmailService emailService) {
        this.paiementService = paiementService;
        this.paiementAccessService = paiementAccessService;
        this.stripeService = stripeService;
        this.emailService = emailService;
    }

    @GetMapping("/public-key")
    public ResponseEntity<?> getPublicKey() {
        log.info("[STRIPE] /public-key");
        if (publishableKey == null || publishableKey.isBlank() || publishableKey.toLowerCase().contains("dummy")) {
            log.warn("[STRIPE] Clé publique Stripe non configurée");
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(Map.of("publicKey", publishableKey));
    }

    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','PARENT','MEMBRE')")
    @PostMapping({"/payment-intent", "/create-payment-intent"})
    public ResponseEntity<?> createPaymentIntent(@RequestBody Map<String, Object> body,
                                                 Authentication authentication) {
        log.info("==================================================");
        log.info("🚀 [STRIPE] createPaymentIntent déclenché");
        log.info("[STRIPE] Payload: {}", body);

        try {
            // Court-circuit si Stripe n’est pas configuré
            if (!stripeService.isConfigured()) {
                log.warn("[STRIPE] API Key non configurée (ou factice). Abandon de la création du PaymentIntent.");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(Map.of("error", "Stripe non configuré côté serveur. Contactez l’administrateur."));
            }
            // --------- Auth via Spring Security ---------
            Utilisateur utilisateur = paiementAccessService.requireAuthenticatedUser(authentication);
            log.info("[STRIPE] Auth OK → {}", authentication.getName());

            Long paiementId = asLong(body.get("paiementId"));
            final Long reqEcheanceId = asLong(body.get("echeanceId"));
            String customerEmail = asString(body.get("customerEmail"));
            Boolean sendReceiptEmail = asBoolean(body.get("sendReceiptEmail")); // null = non fourni

            if (paiementId == null) {
                log.warn("[STRIPE] paiementId manquant");
                return ResponseEntity.badRequest().body(Map.of("error", "paiementId manquant"));
            }

            Optional<Paiement> optPaiement = paiementService.getById(paiementId);
            if (optPaiement.isEmpty()) {
                log.warn("[STRIPE] Paiement introuvable id={}", paiementId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Paiement introuvable"));
            }
            Paiement p = optPaiement.get();

            // --------- Autorisation via PaiementAccessService ---------
            paiementAccessService.assertCanAccessPaiement(authentication, p);
            log.info("[STRIPE] Autorisation OK pour paiementId={}", paiementId);

            // --------- Compte Stripe connecte du club (repli sur le compte plateforme si absent/non active) ---------
            String stripeAccountId = (p.getClub() != null && p.getClub().isStripeChargesEnabled())
                    ? p.getClub().getStripeAccountId()
                    : null;
            log.info("[STRIPE] stripeAccountId retenu pour paiementId={} : {}", paiementId, stripeAccountId);

            // --------- Déterminer montant à payer (centimes) ---------
            Echeance cible = null;
            Integer echeanceNumero = null;
            long amountInCents;
            String typeCourant = (p.getType() == null) ? "UNIQUE" : p.getType().trim().toUpperCase(Locale.ROOT);

            if ("ECHELONNE".equalsIgnoreCase(typeCourant)) {
                List<Echeance> echeances = (p.getEcheances() == null) ? List.of() : p.getEcheances();
                if (echeances.isEmpty()) {
                    log.warn("[STRIPE] Aucune échéance définie pour le paiement id={}", paiementId);
                    return ResponseEntity.badRequest().body(Map.of("error", "Aucune échéance définie pour ce paiement."));
                }

                if (reqEcheanceId != null) {
                    cible = echeances.stream()
                            .filter(e -> Objects.equals(e.getId(), reqEcheanceId))
                            .findFirst().orElse(null);
                    if (cible == null) {
                        log.warn("[STRIPE] Échéance précisée introuvable: {}", reqEcheanceId);
                        return ResponseEntity.badRequest().body(Map.of("error", "Échéance spécifiée introuvable."));
                    }
                } else {
                    Optional<Echeance> firstUnpaid = echeances.stream()
                            .filter(e -> !"payé".equalsIgnoreCase(safe(e.getStatut())))
                            .sorted(Comparator.comparingInt(Echeance::getNumero))
                            .findFirst();
                    if (firstUnpaid.isEmpty()) {
                        log.info("[STRIPE] Aucune échéance impayée pour paiement id={}", paiementId);
                        return ResponseEntity.badRequest().body(Map.of("error", "Aucune échéance à payer."));
                    }
                    cible = firstUnpaid.get();
                }

                echeanceNumero = cible.getNumero();
                amountInCents = Math.round(safeDouble(cible.getMontant()) * 100.0);
                log.info("[STRIPE] Montant échéance #{} = {} cents", echeanceNumero, amountInCents);
            } else {
                if (p.getMontantTotal() == null || p.getMontantTotal() <= 0) {
                    log.warn("[STRIPE] Montant total invalide sur paiement id={}", paiementId);
                    return ResponseEntity.badRequest().body(Map.of("error", "Montant du paiement invalide."));
                }
                amountInCents = Math.round(p.getMontantTotal() * 100.0);
                log.info("[STRIPE] Montant total (unique) = {} cents", amountInCents);
            }

            final Long finalEcheanceId = ("ECHELONNE".equalsIgnoreCase(typeCourant) && cible != null)
                    ? (reqEcheanceId != null ? reqEcheanceId : cible.getId())
                    : null;

            // --------- Réutilisation éventuelle d’un PI confirmable ---------
            PaymentIntent existing = null;
            if (cible != null && cible.getReference() != null && !cible.getReference().isBlank()) {
                try {
                    existing = (stripeAccountId != null)
                            ? PaymentIntent.retrieve(cible.getReference(), com.stripe.net.RequestOptions.builder().setStripeAccount(stripeAccountId).build())
                            : PaymentIntent.retrieve(cible.getReference());
                    String status = existing.getStatus();
                    log.info("[STRIPE] PI existant pour échéance={}, status={}", cible.getId(), status);
                    if (status != null && CONFIRMABLE_STATUSES.contains(status)) {
                        log.info("[STRIPE] Réutilisation du PaymentIntent confirmable: {}", existing.getId());
                        Map<String, Object> reuseBody = new HashMap<>();
                        reuseBody.put("clientSecret", existing.getClientSecret());
                        reuseBody.put("paymentIntentId", existing.getId());
                        reuseBody.put("stripeAccountId", stripeAccountId);
                        return ResponseEntity.ok(reuseBody);
                    }
                } catch (Exception ex) {
                    log.warn("[STRIPE] Impossible de récupérer le PI existant (échéance={}) → ignore. Cause: {}",
                            cible.getId(), ex.getMessage());
                }
            }

            // --------- E-mail pour reçu Stripe natif ---------
            if (customerEmail == null || customerEmail.isBlank()) {
                if (p.getUtilisateur() != null && p.getUtilisateur().getEmail() != null) {
                    String e = p.getUtilisateur().getEmail().trim();
                    if (!e.isEmpty()) customerEmail = e;
                }
                if ((customerEmail == null || customerEmail.isBlank()) && utilisateur != null && utilisateur.getEmail() != null) {
                    String e = utilisateur.getEmail().trim();
                    if (!e.isEmpty()) customerEmail = e;
                }
            }

            // --------- Metadata (lien fort vers la BDD) ---------
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("paiementId", String.valueOf(paiementId));
            metadata.put("type", typeCourant);
            if (finalEcheanceId != null) metadata.put("echeanceId", String.valueOf(finalEcheanceId));
            metadata.put("membreId", p.getMembre() != null ? String.valueOf(p.getMembre().getId()) : "");
            metadata.put("utilisateurId", p.getUtilisateur() != null ? String.valueOf(p.getUtilisateur().getId()) : "");

            // --------- Corps du PaymentIntent ---------
            Map<String, Object> piReq = new HashMap<>();
            piReq.put("amount", amountInCents);         // ⚠️ toujours en CENTIMES
            piReq.put("currency", "eur");
            piReq.put("metadata", metadata);
            piReq.put("description", "Cotisation / Paiement #" + paiementId);
            piReq.put("automatic_payment_methods", Map.of("enabled", true));
            // Respecte la préférence d'envoi du reçu (par défaut: true si non précisé)
            boolean wantReceiptEmail = sendReceiptEmail == null ? true : Boolean.TRUE.equals(sendReceiptEmail);
            if (wantReceiptEmail && customerEmail != null && !customerEmail.isBlank()) {
                piReq.put("receipt_email", customerEmail);
            }
            // trace légère de la préférence
            Map<String, Object> mdForPi = new HashMap<>(metadata);
            mdForPi.put("sendReceiptEmail", String.valueOf(wantReceiptEmail));
            piReq.put("metadata", mdForPi);

            // --------- Idempotency-Key stable ---------
            String idemKeySuffix = "ECHELONNE".equalsIgnoreCase(typeCourant)
                    ? ("ech-" + (echeanceNumero != null ? echeanceNumero : "x") + "-" + (finalEcheanceId != null ? finalEcheanceId : "x") + "-" + amountInCents)
                    : ("unique-" + amountInCents);
            String idempotencyKey = "paiement-" + paiementId + "-" + idemKeySuffix;

            if (existing != null) {
                String status = existing.getStatus();
                if (status != null && (status.equals("succeeded")
                        || status.equals("canceled")
                        || status.equals("requires_capture")
                        || status.equals("processing"))) {
                    idempotencyKey = idempotencyKey + "-v" + System.currentTimeMillis();
                    log.info("[STRIPE] Ancien PI non réutilisable (status={}) → nouvelle Idempotency-Key: {}", status, idempotencyKey);
                }
            }

            log.info("[STRIPE] Création/MAJ PaymentIntent → amount={} cents, idemKey={}", amountInCents, idempotencyKey);
            log.debug("[STRIPE] Payload PI: {}", piReq);

            // --------- Création Stripe (avec idempotence) ---------
            PaymentIntent paymentIntent;
            try {
                paymentIntent = stripeService.createPaymentIntentWithMetadata(piReq, idempotencyKey, stripeAccountId);
            } catch (IdempotencyException idemEx) {
                log.warn("[STRIPE] IdempotencyException: {} → retry avec clé versionnée", idemEx.getMessage());
                String retryKey = idempotencyKey + "-v" + System.currentTimeMillis();
                paymentIntent = stripeService.createPaymentIntentWithMetadata(piReq, retryKey, stripeAccountId);
            }

            // --------- Persister l’ID du PI côté BDD ---------
            try {
                if ("ECHELONNE".equalsIgnoreCase(typeCourant) && finalEcheanceId != null) {
                    paiementService.saveEcheanceReference(finalEcheanceId, paymentIntent.getId());
                    log.info("[STRIPE] PaymentIntent ID sauvegardé sur l'échéance {} → {}", finalEcheanceId, paymentIntent.getId());
                } else {
                    p.setPaymentIntentId(paymentIntent.getId());
                    paiementService.save(p);
                    log.info("[STRIPE] PaymentIntent ID sauvegardé sur le paiement (unique) → {}", paymentIntent.getId());
                }
            } catch (Exception ex) {
                log.warn("[STRIPE] Erreur de sauvegarde PaymentIntent en BDD: {}", ex.getMessage());
            }

            log.info("[STRIPE] ✅ ClientSecret renvoyé au front");
            log.info("==================================================");
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("clientSecret", paymentIntent.getClientSecret());
            responseBody.put("paymentIntentId", paymentIntent.getId());
            responseBody.put("stripeAccountId", stripeAccountId);
            return ResponseEntity.ok(responseBody);

        } catch (Exception e) {
            log.error("[STRIPE] ❌ Exception dans createPaymentIntent: {}", e.getMessage(), e);
            log.info("==================================================");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/config-status")
    public ResponseEntity<?> getConfigStatus() {
        boolean pkConfigured = publishableKey != null && !publishableKey.isBlank() && !publishableKey.toLowerCase().contains("dummy");
        boolean skConfigured = stripeService.isConfigured();
        return ResponseEntity.ok(Map.of(
                "publishableKeyConfigured", pkConfigured,
                "secretKeyConfigured", skConfigured
        ));
    }

    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','PARENT','MEMBRE')")
    @PostMapping("/sync-payment")
    public ResponseEntity<?> syncPayment(@RequestBody Map<String, Object> body,
                                         Authentication authentication) {
        try {
            log.info("==================================================");
            log.info("🔁 [STRIPE] sync-payment déclenché: {}", body);
            String piId = asString(body.get("paymentIntentId"));
            if (piId == null || piId.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "paymentIntentId manquant"));
            }

            PaymentIntent pi = PaymentIntent.retrieve(piId);
            if (!"succeeded".equalsIgnoreCase(pi.getStatus())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "PaymentIntent non succeeded",
                        "status", pi.getStatus()
                ));
            }

            Map<String, String> md = pi.getMetadata();
            if (md == null) return ResponseEntity.badRequest().body(Map.of("error", "metadata manquantes"));
            Long paiementId = asLong(md.get("paiementId"));
            Long echeanceId = asLong(md.get("echeanceId"));
            boolean optInReceipt = Boolean.parseBoolean(String.valueOf(md.getOrDefault("sendReceiptEmail","true")));

            if (paiementId == null)
                return ResponseEntity.badRequest().body(Map.of("error", "paiementId manquant dans metadata"));

            Optional<Paiement> opt = paiementService.getById(paiementId);
            if (opt.isEmpty())
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Paiement introuvable"));

            Paiement paiement = opt.get();

            // --------- Autorisation ---------
            paiementAccessService.assertCanAccessPaiement(authentication, paiement);

            // 🔁 Idempotence côté BDD : si déjà payé et plus de restant, on ne refait rien
            if ("payé".equalsIgnoreCase(paiement.getStatut())
                    && (paiement.getMontantRestant() == null || paiement.getMontantRestant() <= 0.0)) {
                return ResponseEntity.ok(Map.of("status", "already-paid"));
            }

            paiement.setModePaiement("CB");

        double montantPaye = 0.0;

        if ("ECHELONNE".equalsIgnoreCase(paiement.getType())
                    && paiement.getEcheances() != null && !paiement.getEcheances().isEmpty()) {

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
                try { montantPaye = Optional.ofNullable(cible.getMontant()).orElse(0.0); } catch (Exception ignored) {}

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
                montantPaye = Optional.ofNullable(paiement.getMontantTotal()).orElse(0.0);
            }

        paiementService.save(paiement);
        log.info("[STRIPE] ✅ sync OK → paiementId={} statut={} restant={} nbEchRestantes={} ",
            paiement.getId(), paiement.getStatut(), paiement.getMontantRestant(), paiement.getEcheancesRestantes());
        
        // ✉️ Envoi email de reçu (fallback club) si opt-in
        try {
            if (optInReceipt) {
                String receiptUrl = null;
                String latestChargeId = pi.getLatestCharge();
                if (latestChargeId != null && !latestChargeId.isBlank()) {
                    Charge charge = Charge.retrieve(latestChargeId);
                    if (charge != null) {
                        // 🔗 Persiste chargeId et receiptUrl
                        paiement.setChargeId(latestChargeId);
                        receiptUrl = charge.getReceiptUrl();
                        if (receiptUrl == null || receiptUrl.isBlank()) {
                            Object latestObj = pi.getLatestChargeObject();
                            if (latestObj instanceof Charge) {
                                // Assure le chargeId et l'URL depuis l'objet latest
                                paiement.setChargeId(((Charge) latestObj).getId());
                                receiptUrl = ((Charge) latestObj).getReceiptUrl();
                            }
                        }
                    }
                }
                // Sauvegarde des infos Stripe utiles
                try {
                    if (paiement.getPaymentIntentId() == null || paiement.getPaymentIntentId().isBlank()) {
                        paiement.setPaymentIntentId(piId);
                    }
                    if (receiptUrl != null && !receiptUrl.isBlank()) {
                        paiement.setReceiptUrl(receiptUrl);
                    }
                    if (pi.getStatus() != null) {
                        paiement.setStripeStatus(pi.getStatus());
                    }
                    paiementService.save(paiement);
                } catch (Exception ignored) { }

                String destinataire = null;
                try {
                    if (pi.getReceiptEmail() != null && !pi.getReceiptEmail().isBlank()) destinataire = pi.getReceiptEmail();
                } catch (Exception ignored) {}
                if (destinataire == null && paiement.getUtilisateur() != null) {
                    destinataire = paiement.getUtilisateur().getEmail();
                }

                if (destinataire != null && receiptUrl != null && !receiptUrl.isBlank()) {
                    String description = ("ECHELONNE".equalsIgnoreCase(paiement.getType()))
                            ? ("Échéance du paiement #" + paiement.getId())
                            : ("Paiement unique #" + paiement.getId());
                    var club = paiement.getUtilisateur() != null ? paiement.getUtilisateur().getClub() : null;
                    emailService.envoyerRecuPaiement(club, destinataire, montantPaye, description, receiptUrl);
                    log.info("[STRIPE] ✉️ Reçu envoyé par email à {} (fallback club)", destinataire);
                }
            }
        } catch (Exception exMail) {
            log.warn("[STRIPE] ⚠️ Impossible d'envoyer le reçu email (fallback): {}", exMail.getMessage());
        }
        log.info("==================================================");
        return ResponseEntity.ok(Map.of("status", "synced"));

        } catch (Exception e) {
            log.error("[STRIPE] ❌ sync-payment error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/receipt/{paiementId}")
    public ResponseEntity<?> redirectToStripeReceipt(@PathVariable Long paiementId,
                                                     Authentication authentication) {
        try {
            log.info("🧾 [STRIPE] receipt pour paiementId={}", paiementId);
            Paiement p = paiementService.getById(paiementId)
                    .orElseThrow(() -> new IllegalArgumentException("Paiement introuvable"));

            if (authentication != null && authentication.isAuthenticated()
                    && !"anonymousUser".equals(authentication.getPrincipal())) {
                paiementAccessService.assertCanAccessPaiement(authentication, p);
            }

            String receiptUrl = stripeService.getReceiptUrl(p).orElse(null);
            if (receiptUrl == null || receiptUrl.isBlank()) {
                log.warn("[STRIPE] Aucun reçu disponible pour paiementId={}", paiementId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Aucun reçu disponible"));
            }
            log.info("[STRIPE] ✅ receiptUrl trouvée pour paiementId={}", paiementId);
            return ResponseEntity.ok(Map.of("receiptUrl", receiptUrl));

        } catch (Exception e) {
            log.error("[STRIPE] ❌ receipt error: {}", e.getMessage(), e);
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

    private static Boolean asBoolean(Object o) {
        if (o == null) return null;
        if (o instanceof Boolean b) return b;
        String s = String.valueOf(o).trim().toLowerCase();
        if (s.equals("true") || s.equals("1") || s.equals("yes") || s.equals("oui")) return true;
        if (s.equals("false") || s.equals("0") || s.equals("no") || s.equals("non")) return false;
        return null;
    }

    private static String safe(String s) {
        return (s == null) ? "" : s;
    }

    private static double safeDouble(Double d) {
        return (d == null) ? 0.0 : d;
    }
}
