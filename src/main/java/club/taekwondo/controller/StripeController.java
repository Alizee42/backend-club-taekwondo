package club.taekwondo.controller;

import club.taekwondo.entity.jpa.Paiement;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.security.JwtUtil;
import club.taekwondo.service.StripeService;
import club.taekwondo.service.jpa.PaiementService;
import club.taekwondo.service.jpa.UtilisateurService;
import com.stripe.model.PaymentIntent;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/stripe")
public class StripeController {

    private final StripeService stripeService;
    private final UtilisateurService utilisateurService;
    private final PaiementService paiementService;
    private final JwtUtil jwtUtil;

    public StripeController(StripeService stripeService,
                            UtilisateurService utilisateurService,
                            PaiementService paiementService,
                            JwtUtil jwtUtil) {
        this.stripeService = stripeService;
        this.utilisateurService = utilisateurService;
        this.paiementService = paiementService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/public-key")
    public ResponseEntity<Map<String, String>> getPublicKey() {
        return ResponseEntity.ok(Map.of("publicKey", stripeService.getPublicKey()));
    }

    @PostMapping("/create-payment-intent")
    public ResponseEntity<Map<String, String>> createPaymentIntent(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> request) {

        System.out.printf("%n==============================%n");
        System.out.println("🚀 [StripeController] createPaymentIntent déclenché");
        System.out.printf("Requête brute reçue: %s%n", request);

        try {
            // --- Auth ---
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Token manquant ou invalide."));
            }
            final String jwt = authHeader.substring(7);
            final String email = jwtUtil.extractEmail(jwt);
            System.out.printf("🔑 JWT décodé -> email: %s%n", email);

            Optional<Utilisateur> optUser = utilisateurService.getUtilisateurEntityByEmail(email);
            if (optUser.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Utilisateur introuvable."));
            }
            Utilisateur utilisateur = optUser.get();
            System.out.printf("👤 Utilisateur trouvé: %s (id=%d)%n",
                    utilisateur.getEmail(), utilisateur.getId());

            // --- Param ---
            if (!request.containsKey("paiementId")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "paiementId manquant (créez d’abord le Paiement en BDD)."));
            }
            final Long paiementId = Long.valueOf(String.valueOf(request.get("paiementId")));
            System.out.printf("📦 paiementId reçu: %d%n", paiementId);

            Optional<Paiement> optPaiement = paiementService.getById(paiementId);
            if (optPaiement.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Paiement introuvable."));
            }
            Paiement p = optPaiement.get();
            System.out.printf("✅ Paiement trouvé: montantTotal=%s, type=%s%n",
                    p.getMontantTotal(), p.getType());

            // --- Autorisation ---
            boolean autorise = false;
            try {
                autorise = (p.getUtilisateur() != null && p.getUtilisateur().getId().equals(utilisateur.getId()))
                        || (p.getMembre() != null && p.getMembre().getParent() != null
                        && p.getMembre().getParent().getId().equals(utilisateur.getId()));
            } catch (Exception ignored) {}
            System.out.printf("🔒 Vérif autorisation -> %s%n", autorise ? "OK" : "REFUSÉ");
            if (!autorise) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Non autorisé pour ce paiement."));
            }

            // --- Réutilisation éventuelle d’un PI existant (seulement s’il est confirmable) ---
            if (p.getPaymentIntentId() != null && !p.getPaymentIntentId().isBlank()) {
                try {
                    PaymentIntent existing = PaymentIntent.retrieve(p.getPaymentIntentId());
                    String status = existing.getStatus(); // requires_payment_method | requires_confirmation | requires_action | processing | requires_capture | canceled | succeeded
                    System.out.printf("ℹ️ PaymentIntent existant=%s, status=%s%n", existing.getId(), status);

                    boolean confirmable =
                            "requires_payment_method".equals(status) ||
                            "requires_confirmation".equals(status) ||
                            "requires_action".equals(status);

                    if (confirmable) {
                        System.out.printf("↩️ Réutilisation du PI existant (encore confirmable): %s%n", existing.getId());
                        System.out.printf("==============================%n%n");
                        return ResponseEntity.ok(Map.of("clientSecret", existing.getClientSecret()));
                    } else {
                        // PI terminé / non réutilisable -> le "oublier" pour forcer une création propre
                        System.out.printf("🔁 PI %s non réutilisable (status=%s), on en créera un nouveau.%n", existing.getId(), status);
                        p.setPaymentIntentId(null);
                        paiementService.save(p);
                    }
                } catch (Exception ex) {
                    System.out.println("⚠️ Récupération PI échouée, on considère qu’il n’existe pas. Cause: " + ex.getMessage());
                    p.setPaymentIntentId(null);
                    paiementService.save(p);
                }
            }

            // --- Calcul montant & idempotency ---
            long amountInCents;
            String idemKeySuffix;
            Integer echeanceNumero = null;

            if ("ECHELONNE".equalsIgnoreCase(p.getType())
                    && p.getEcheances() != null && !p.getEcheances().isEmpty()) {

                var firstUnpaid = p.getEcheances().stream()
                        .filter(e -> !"payé".equalsIgnoreCase(e.getStatut()))
                        .sorted(Comparator.comparingInt(e -> e.getNumero()))
                        .findFirst();

                if (firstUnpaid.isEmpty()) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Aucune échéance à payer."));
                }

                amountInCents = Math.round(firstUnpaid.get().getMontant() * 100.0);
                echeanceNumero = firstUnpaid.get().getNumero();
                idemKeySuffix = "ech-" + echeanceNumero + "-" + amountInCents;

                System.out.printf("💰 Montant calculé pour échéance #%d = %d cents%n",
                        echeanceNumero, amountInCents);

            } else {
                if (p.getMontantTotal() == null || p.getMontantTotal() <= 0) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Montant du paiement invalide."));
                }
                amountInCents = Math.round(p.getMontantTotal() * 100.0);
                idemKeySuffix = "unique-" + amountInCents;

                System.out.printf("💰 Montant total calculé = %d cents%n", amountInCents);
            }

            String idempotencyKey = "paiement-" + paiementId + "-" + idemKeySuffix;

            // --- Construction de la requête Stripe ---
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("paiementId", String.valueOf(paiementId));
            metadata.put("type", String.valueOf(p.getType()));
            if (echeanceNumero != null) {
                metadata.put("echeanceNumero", String.valueOf(echeanceNumero));
            }

            Map<String, Object> piReq = new HashMap<>();
            piReq.put("amount", amountInCents);
            piReq.put("currency", "eur");
            piReq.put("metadata", metadata);

            System.out.printf("🟢 Envoi création PaymentIntent à Stripe avec: %s%n", piReq);
            System.out.printf("🔁 Idempotency-Key utilisée: %s%n", idempotencyKey);

            PaymentIntent paymentIntent = stripeService.createPaymentIntentWithMetadata(piReq, idempotencyKey);

            // --- Persister l’ID du PI pour un éventuel re-tentative ---
            try {
                p.setPaymentIntentId(paymentIntent.getId());
                paiementService.save(p);
                System.out.printf("💾 PaymentIntent ID sauvegardé en base: %s%n", paymentIntent.getId());
            } catch (Exception ex) {
                System.out.println("⚠️ Erreur sauvegarde PaymentIntent en BDD: " + ex.getMessage());
            }

            System.out.println("✅ ClientSecret renvoyé au front");
            System.out.printf("==============================%n%n");
            return ResponseEntity.ok(Map.of("clientSecret", paymentIntent.getClientSecret()));

        } catch (Exception e) {
            System.out.println("❌ Exception attrapée dans createPaymentIntent:");
            e.printStackTrace();
            System.out.printf("==============================%n%n");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }
}

