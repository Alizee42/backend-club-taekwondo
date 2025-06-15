package club.taekwondo.controller;

import club.taekwondo.entity.jpa.Echeance;
import club.taekwondo.entity.jpa.Paiement;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.service.StripeService;
import club.taekwondo.service.jpa.UtilisateurService;
import club.taekwondo.service.jpa.PaiementService; // Import PaiementService
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/stripe")
public class StripeController {

    private final StripeService stripeService;
    private final UtilisateurService utilisateurService; // Ajout de UtilisateurService
    private final PaiementService paiementService; // Ajout de PaiementService

    public StripeController(StripeService stripeService, UtilisateurService utilisateurService, PaiementService paiementService) {
        this.stripeService = stripeService;
        this.utilisateurService = utilisateurService; // Injection de UtilisateurService
        this.paiementService = paiementService; // Injection de PaiementService
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
            // Validation minimale
            if (!request.containsKey("amount") || !request.containsKey("currency") || !request.containsKey("typePaiement")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Champs 'amount', 'currency' et 'typePaiement' requis."));
            }
    
            String typePaiement = request.get("typePaiement").toString();
            double amount = Double.parseDouble(request.get("amount").toString());
            String modePaiement = request.get("modePaiement").toString();
            int nombreEcheances = request.containsKey("nombreEcheances") ? Integer.parseInt(request.get("nombreEcheances").toString()) : 1;
            Long utilisateurId = Long.parseLong(request.get("utilisateurId").toString());
    
            // Récupérer l'utilisateur
            Optional<Utilisateur> utilisateurOptional = utilisateurService.getUtilisateurById(utilisateurId);
            if (utilisateurOptional.isEmpty()) {
                throw new RuntimeException("Utilisateur non trouvé pour l'ID : " + utilisateurId);
            }
            Utilisateur utilisateur = utilisateurOptional.get();
    
            // Vérifiez si un paiement similaire existe déjà
            List<Paiement> paiementsExistants = paiementService.getByMembreId(utilisateurId);
            for (Paiement p : paiementsExistants) {
                if (p.getMontantTotal() != null && p.getMontantTotal().equals(amount) &&
                    p.getModePaiement().equals(modePaiement) &&
                    p.getStatut().equals("en attente")) {
                    throw new RuntimeException("Un paiement similaire existe déjà.");
                }
            }
    
            // Créer le paiement
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
                    echeance.setPaiement(paiement);
                    echeances.add(echeance);
                }
                paiement.setEcheances(echeances);
            }
    
            paiementService.save(paiement);
    
            // Créer le PaymentIntent Stripe
            PaymentIntent paymentIntent = stripeService.executeStripePayment(token, request);
    
            Map<String, String> response = new HashMap<>();
            response.put("clientSecret", paymentIntent.getClientSecret());
            return ResponseEntity.ok(response);
    
        } catch (StripeException se) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", se.getCode()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Une erreur inattendue est survenue."));
        }
    }
}