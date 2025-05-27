package club.taekwondo.controller.jpa;

import club.taekwondo.entity.jpa.Membre;
import club.taekwondo.entity.jpa.Paiement;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.security.JwtUtil;
import club.taekwondo.service.jpa.MembreService;
import club.taekwondo.service.jpa.PaiementService;
import club.taekwondo.service.jpa.UtilisateurService; // Import manquant

import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/paiements")
public class PaiementController {

    private final PaiementService paiementService;
    private final MembreService membreService;
    private final UtilisateurService utilisateurService; // Ajout de UtilisateurService
    private final JwtUtil jwtUtil;

    public PaiementController(PaiementService paiementService, 
                              MembreService membreService, 
                              UtilisateurService utilisateurService, // Ajout dans le constructeur
                              JwtUtil jwtUtil) {
        this.paiementService = paiementService;
        this.membreService = membreService;
        this.utilisateurService = utilisateurService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping
    public List<Paiement> getAll() {
        return paiementService.getAll();
    }

    @PostMapping("/create-payment-intent")
    public ResponseEntity<Map<String, String>> createPaymentIntent(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> request) {
        try {
            Long membreId = extractMembreIdFromToken(token);
            Optional<Membre> membreOptional = membreService.getMembreById(membreId);
            if (membreOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Membre non trouvé."));
            }
            Membre membre = membreOptional.get();

            Optional<Utilisateur> utilisateurOptional = utilisateurService.getUtilisateurById(membreId);
            if (utilisateurOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Utilisateur non trouvé."));
            }
            Utilisateur utilisateur = utilisateurOptional.get();

            Double amount = Double.valueOf(request.get("amount").toString());
            String currency = request.get("currency").toString();

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount((long) (amount * 100))
                    .setCurrency(currency)
                    .build();

            PaymentIntent paymentIntent = PaymentIntent.create(params);

            Paiement paiement = new Paiement();
            paiement.setMontant(amount);
            paiement.setDatePaiement(LocalDate.now());
            paiement.setStatut("en attente");
            paiement.setModePaiement("carte");
            paiement.setType("Cotisation");
            paiement.setMembre(membre);
            paiement.setUtilisateur(utilisateur); // Définir l'utilisateur
            paiementService.save(paiement);

            Map<String, String> response = new HashMap<>();
            response.put("clientSecret", paymentIntent.getClientSecret());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Une erreur inattendue est survenue."));
        }
    }

    private Long extractMembreIdFromToken(String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            Optional<Membre> membre = membreService.getMembreByEmail(email);
            if (membre.isPresent()) {
                return membre.get().getId();
            }
            throw new RuntimeException("Membre non trouvé pour l'e-mail : " + email);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'extraction de l'ID du membre depuis le token.", e);
        }
    }
}