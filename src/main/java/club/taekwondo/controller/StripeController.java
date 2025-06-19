package club.taekwondo.controller;

import club.taekwondo.entity.jpa.Echeance;
import club.taekwondo.entity.jpa.Paiement;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.security.JwtUtil;
import club.taekwondo.service.StripeService;
import club.taekwondo.service.jpa.PaiementService;
import club.taekwondo.service.jpa.UtilisateurService;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/stripe")
public class StripeController {

    private final StripeService stripeService;
    private final UtilisateurService utilisateurService;
    private final PaiementService paiementService;
    private final JwtUtil jwtUtil;

    public StripeController(StripeService stripeService, UtilisateurService utilisateurService,
                            PaiementService paiementService, JwtUtil jwtUtil) {
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
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> request) {

        try {
            if (token == null || !token.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Token manquant ou invalide."));
            }

            String jwt = token.substring(7);
            String email = jwtUtil.extractEmail(jwt);
            Utilisateur utilisateur = utilisateurService.getByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Utilisateur introuvable."));

            double amount = Double.parseDouble(request.get("amount").toString());
            String currency = request.get("currency").toString();
            if (!currency.matches("(?i)eur|usd")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Devise non autorisée"));
            }

            String typePaiement = request.get("typePaiement").toString();
            String modePaiement = request.get("modePaiement").toString();
            int nombreEcheances = request.containsKey("nombreEcheances")
                    ? Integer.parseInt(request.get("nombreEcheances").toString()) : 1;

            // Vérifie les doublons
            
            List<Paiement> paiementsExistants = paiementService.getByMembreId(utilisateur.getId());
            for (Paiement p : paiementsExistants) {
                if (p.getMontantTotal() != null && p.getMontantTotal().equals(amount)
                        && p.getModePaiement().equals(modePaiement)
                        && p.getStatut().equals("en attente")) {
                    return ResponseEntity.status(HttpStatus.CONFLICT)
                            .body(Map.of("error", "Un paiement similaire existe déjà."));
                }
            }
            
            // Création du Paiement
            Paiement paiement = new Paiement();
            paiement.setMontantTotal(amount);
            paiement.setModePaiement(modePaiement);
            paiement.setType(typePaiement);
            paiement.setStatut("en attente");
            paiement.setUtilisateur(utilisateur);

            if ("echeances".equalsIgnoreCase(typePaiement)) {
                List<Echeance> echeances = new ArrayList<>();
                double montantParEcheance = amount / nombreEcheances;
                for (int i = 0; i < nombreEcheances; i++) {
                    Echeance echeance = new Echeance();
                    echeance.setMontant(montantParEcheance);
                    echeance.setDateEcheance(LocalDate.now().plusMonths(i + 1));
                    echeance.setStatut("en attente");
                    echeance.setNumero(i + 1);
                    echeance.setPaiement(paiement);
                    echeances.add(echeance);
                }
                paiement.setEcheances(echeances);
            }

            paiementService.save(paiement);

            PaymentIntent paymentIntent = stripeService.executeStripePayment(jwt, request);
            return ResponseEntity.ok(Map.of("clientSecret", paymentIntent.getClientSecret()));

        } catch (StripeException se) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Erreur Stripe : " + se.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur interne : " + e.getMessage()));
        }
    }
}
