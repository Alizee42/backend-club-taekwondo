package club.taekwondo.controller;

import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.security.JwtUtil;
import club.taekwondo.service.StripeService;
import club.taekwondo.service.jpa.PaiementService;
import club.taekwondo.service.jpa.UtilisateurService;
import club.taekwondo.service.jpa.MembreService;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/stripe")
public class StripeController {

    private final StripeService stripeService;
    private final UtilisateurService utilisateurService;
    private final JwtUtil jwtUtil;

    public StripeController(StripeService stripeService,
                            UtilisateurService utilisateurService,
                            PaiementService paiementService,
                            JwtUtil jwtUtil,
                            MembreService membreService) {
        this.stripeService = stripeService;
        this.utilisateurService = utilisateurService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/public-key")
    public ResponseEntity<Map<String, String>> getPublicKey() {
        return ResponseEntity.ok(Map.of("publicKey", stripeService.getPublicKey()));
    }

    /**
     * OPTION A : ICI on ne crée PAS de Paiement.
     * On présume que le front a déjà créé le Paiement en BDD (statut "en attente") et nous envoie paiementId.
     * Notre rôle : créer un PaymentIntent Stripe avec metadata.paiementId et renvoyer clientSecret.
     */
    @PostMapping("/create-payment-intent")
    public ResponseEntity<Map<String, String>> createPaymentIntent(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> request) {

        System.out.println("🚀 [StripeController] createPaymentIntent déclenché");
        System.out.println("🔎 Token reçu : " + token);
        System.out.println("🔎 Payload reçu : " + request);

        try {
            if (token == null || !token.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Token manquant ou invalide."));
            }

            final String jwt = token.substring(7);
            final String email = jwtUtil.extractEmail(jwt);
            Utilisateur utilisateur = utilisateurService.getUtilisateurEntityByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Utilisateur introuvable."));

            // ====== Lecture & normalisation des champs ======
            // paiementId est OBLIGATOIRE en Option A
            if (!request.containsKey("paiementId")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "paiementId manquant (Option A: créer le Paiement en BDD avant)."));
            }
            final Long paiementId = Long.valueOf(String.valueOf(request.get("paiementId")));

            // amount en EUROS côté front -> conversion en CENTIMES ici
            if (!request.containsKey("amount")) {
                return ResponseEntity.badRequest().body(Map.of("error", "amount manquant."));
            }
            final long amountInCents;
            try {
                double montantEuros = Double.parseDouble(String.valueOf(request.get("amount")));
                amountInCents = Math.round(montantEuros * 100);
            } catch (NumberFormatException ex) {
                return ResponseEntity.badRequest().body(Map.of("error", "amount invalide."));
            }

            final String currency = String.valueOf(request.getOrDefault("currency", "eur")).toLowerCase();
            if (!currency.matches("eur|usd")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Devise non autorisée"));
            }

            final String typePaiement = String.valueOf(request.getOrDefault("typePaiement", "")); // 'UNIQUE' | 'ECHELONNE'

            // compat: on accepte "modePaiement" ou "mode"
            final String modePaiement = request.containsKey("modePaiement")
                    ? String.valueOf(request.get("modePaiement"))
                    : String.valueOf(request.getOrDefault("mode", "CB"));

            final Integer nombreEcheances = request.containsKey("nombreEcheances")
                    ? Integer.valueOf(String.valueOf(request.get("nombreEcheances")))
                    : 1;

            Long enfantId = null;
            if (request.containsKey("membreId")) {
                enfantId = Long.valueOf(String.valueOf(request.get("membreId")));
            } else if (request.containsKey("enfantId")) {
                enfantId = Long.valueOf(String.valueOf(request.get("enfantId")));
            }

            // (Optionnel) reçu par email
            final String receiptEmail = request.containsKey("receiptEmail")
                    ? String.valueOf(request.get("receiptEmail"))
                    : null;

            // ====== Construire la requête pour StripeService (no Map.of avec null !) ======
            final Map<String, Object> piReq = new HashMap<>();
            piReq.put("amount", amountInCents);         // en CENTIMES
            piReq.put("currency", currency);
            piReq.put("paiementId", paiementId);        // ✅ clé pour le webhook
            piReq.put("typePaiement", typePaiement);
            piReq.put("nombreEcheances", nombreEcheances);
            piReq.put("utilisateurId", utilisateur.getId());
            piReq.put("modePaiement", modePaiement);    // utile pour logs / debug
            if (enfantId != null)      piReq.put("enfantId", enfantId);
            if (receiptEmail != null && !receiptEmail.isBlank()) {
                piReq.put("receiptEmail", receiptEmail);
            }

            System.out.println("🟢 Appel StripeService#createPaymentIntentWithMetadata : " + piReq);

            PaymentIntent paymentIntent = stripeService.createPaymentIntentWithMetadata(piReq);
            System.out.println("🟢 Stripe clientSecret reçu : " + paymentIntent.getClientSecret());

            return ResponseEntity.ok(Map.of("clientSecret", paymentIntent.getClientSecret()));

        } catch (StripeException se) {
            System.out.println("❌ Erreur Stripe : " + se.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Erreur Stripe : " + se.getMessage()));
        } catch (Exception e) {
            System.out.println("❌ Erreur interne : " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur interne : " + e.getMessage()));
        }
    }
}

