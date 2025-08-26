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

    /**
     * Crée (ou réutilise) un PaymentIntent Stripe en calculant le montant côté serveur.
     * Reçoit uniquement { paiementId } depuis le front.
     */
    @PostMapping("/create-payment-intent")
    public ResponseEntity<Map<String, String>> createPaymentIntent(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> request) {
        System.out.println("🚀 [StripeController] createPaymentIntent déclenché");
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Token manquant ou invalide."));
            }

            final String jwt = authHeader.substring(7);
            final String email = jwtUtil.extractEmail(jwt);
            Optional<Utilisateur> optUser = utilisateurService.getUtilisateurEntityByEmail(email);
            if (optUser.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Utilisateur introuvable."));
            }
            Utilisateur utilisateur = optUser.get();

            if (!request.containsKey("paiementId")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "paiementId manquant (créez d’abord le Paiement en BDD)."));
            }
            final Long paiementId = Long.valueOf(String.valueOf(request.get("paiementId")));

            Optional<Paiement> optPaiement = paiementService.getById(paiementId);
            if (optPaiement.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Paiement introuvable."));
            }
            Paiement p = optPaiement.get();

            // Contrôle d’appartenance (le parent ne peut payer que ses enfants / ou son propre paiement)
            boolean autorise = false;
            try {
                autorise = (p.getUtilisateur() != null && p.getUtilisateur().getId().equals(utilisateur.getId()))
                        || (p.getMembre() != null && p.getMembre().getParent() != null
                        && p.getMembre().getParent().getId().equals(utilisateur.getId()));
            } catch (Exception ignored) {}
            if (!autorise) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Non autorisé pour ce paiement."));
            }

            // Idempotence simple : si un PaymentIntent est déjà associé, renvoyer le même client_secret
            if (p.getPaymentIntentId() != null && !p.getPaymentIntentId().isBlank()) {
                String clientSecret = stripeService.retrieveClientSecret(p.getPaymentIntentId());
                if (clientSecret != null && !clientSecret.isBlank()) {
                    System.out.println("↩️ Réutilisation PaymentIntent existant: " + p.getPaymentIntentId());
                    return ResponseEntity.ok(Map.of("clientSecret", clientSecret));
                }
            }

            // Calcul montant côté serveur (EUR)
            long amountInCents;
            if ("ECHELONNE".equalsIgnoreCase(p.getType()) && p.getEcheances() != null && !p.getEcheances().isEmpty()) {
                var firstUnpaid = p.getEcheances().stream()
                        .filter(e -> !"payé".equalsIgnoreCase(e.getStatut()))
                        .sorted(Comparator.comparingInt(e -> e.getNumero()))
                        .findFirst();
                if (firstUnpaid.isEmpty()) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Aucune échéance à payer."));
                }
                amountInCents = Math.round(firstUnpaid.get().getMontant() * 100.0);
            } else {
                if (p.getMontantTotal() == null || p.getMontantTotal() <= 0) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Montant du paiement invalide."));
                }
                amountInCents = Math.round(p.getMontantTotal() * 100.0);
            }

            // Création PaymentIntent côté Stripe (toujours EUR) + metadata paiementId
            Map<String, Object> piReq = new HashMap<>();
            piReq.put("amount", amountInCents);
            piReq.put("currency", "eur");
            piReq.put("paiementId", paiementId);

            System.out.println("🟢 Création PaymentIntent Stripe: " + piReq);
            PaymentIntent paymentIntent = stripeService.createPaymentIntentWithMetadata(piReq);

            // (Optionnel mais recommandé) persister l’ID pour idempotence future
            try {
                p.setPaymentIntentId(paymentIntent.getId());
                paiementService.save(p);
            } catch (Exception ignored) {}

            return ResponseEntity.ok(Map.of("clientSecret", paymentIntent.getClientSecret()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }
}

