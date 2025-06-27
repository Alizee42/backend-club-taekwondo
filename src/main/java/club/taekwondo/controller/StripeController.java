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

        System.out.println("🚀 [StripeController] createPaymentIntent déclenché");

        try {
            if (token == null || !token.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Token manquant ou invalide."));
            }

            String jwt = token.substring(7);
            String email = jwtUtil.extractEmail(jwt);
            Utilisateur utilisateur = utilisateurService.getByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Utilisateur introuvable."));

            double montantTotal = Double.parseDouble(request.get("amount").toString()); // ✅ ne pas écraser plus bas
            String currency = request.get("currency").toString();
            if (!currency.matches("(?i)eur|usd")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Devise non autorisée"));
            }

            String typePaiement = request.get("typePaiement").toString();
            String modePaiement = request.get("modePaiement").toString();
            int nombreEcheances = request.containsKey("nombreEcheances")
                    ? Integer.parseInt(request.get("nombreEcheances").toString()) : 1;

            System.out.println("🎯 Reçu typePaiement = " + typePaiement);
            System.out.println("🎯 Reçu modePaiement = " + modePaiement);

            List<Paiement> paiementsExistants = paiementService.getByMembreId(utilisateur.getId());
            for (Paiement p : paiementsExistants) {
                if (p.getMontantTotal() != null && p.getMontantTotal().equals(montantTotal)
                        && p.getModePaiement().equalsIgnoreCase(modePaiement)
                        && p.getStatut().equalsIgnoreCase("en attente")) {
                    return ResponseEntity.status(HttpStatus.CONFLICT)
                            .body(Map.of("error", "Un paiement similaire existe déjà."));
                }
            }

            Paiement paiement = new Paiement();
            paiement.setModePaiement(modePaiement);
            paiement.setType(typePaiement);
            paiement.setUtilisateur(utilisateur);
            paiement.setDatePaiement(LocalDate.now());

            boolean isEcheances = "echeances".equalsIgnoreCase(typePaiement);

            if (isEcheances) {
                System.out.println("🟡 Paiement échelonné détecté");

                double montantParEcheance = montantTotal / nombreEcheances;
                double montantRestant = 0.0;
                int echeancesRestantes = 0;

                List<Echeance> echeances = new ArrayList<>();

                for (int i = 0; i < nombreEcheances; i++) {
                    Echeance echeance = new Echeance();
                    echeance.setMontant(montantParEcheance);
                    echeance.setDateEcheance(LocalDate.now().plusMonths(i));
                    echeance.setNumero(i + 1);
                    echeance.setPaiement(paiement);

                    if (i == 0) {
                        echeance.setStatut("payé");
                    } else {
                        echeance.setStatut("en attente");
                        montantRestant += montantParEcheance;
                        echeancesRestantes++;
                    }

                    echeances.add(echeance);
                }

            
                paiement.setMontantTotal(montantTotal);
                paiement.setMontantRestant(montantRestant);
                paiement.setEcheancesTotales(nombreEcheances);
                paiement.setEcheancesRestantes(echeancesRestantes);
                paiement.setStatut("en attente");
                paiement.setEcheances(echeances);

            } else {
                System.out.println("✅ Paiement unique détecté");

                paiement.setMontantTotal(montantTotal);
                paiement.setMontantRestant(0.0);
                paiement.setStatut("payé");
                paiement.setEcheancesTotales(1);
                paiement.setEcheancesRestantes(0);
            }

            paiementService.save(paiement);
            System.out.println("📌 Paiement enregistré avec statut = " + paiement.getStatut());

            // ✅ Très important : on s’assure d’envoyer le montant total au service Stripe
            request.put("amount", montantTotal);

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
