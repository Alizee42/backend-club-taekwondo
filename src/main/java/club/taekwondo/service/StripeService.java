package club.taekwondo.service;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;

import club.taekwondo.entity.jpa.Paiement;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.security.JwtUtil;
import club.taekwondo.service.jpa.PaiementService;
import club.taekwondo.service.jpa.UtilisateurService;

@Service
public class StripeService {

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @Value("${stripe.public.key}")
    private String stripePublicKey;

    private final PaiementService paiementService;
    private final UtilisateurService utilisateurService; 
    private final JwtUtil jwtUtil; 
    
    public StripeService(PaiementService paiementService, UtilisateurService utilisateurService, JwtUtil jwtUtil) {
    	this.paiementService = paiementService;

    	this.utilisateurService = utilisateurService;
		this.jwtUtil = jwtUtil;
    }
    
    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }

    public PaymentIntent createPaymentIntent(Double amount, String currency) throws StripeException {
        System.out.println("Création d'un PaymentIntent avec le montant : " + amount + " et la devise : " + currency);

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount((long) (amount * 100)) // Montant en centimes
                .setCurrency(currency)
                .addPaymentMethodType("card") // Méthode de paiement acceptée
                .build();

        PaymentIntent paymentIntent = PaymentIntent.create(params);
        System.out.println("PaymentIntent créé avec succès : " + paymentIntent.getId());
        return paymentIntent;
    }

    public String getPublicKey() {
        return stripePublicKey;
    }
    
    public PaymentIntent executeStripePayment(String token, Map<String, Object> request) throws StripeException {
    	
    	
        Long utilisateurId = extractUtilisateurIdFromToken(token);

        Optional<Utilisateur> utilisateurOptional = utilisateurService.getUtilisateurById(utilisateurId);
        if (utilisateurOptional.isEmpty()) {
          //  return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                //    .body(Map.of("error", "Utilisateur non trouvé."));
           	// TODO AJOUTER une exception ici 
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
        paiement.setUtilisateur(utilisateur); 
        paiementService.save(paiement);
        
        return paymentIntent;
    }
    
    private Long extractUtilisateurIdFromToken(String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            Optional<Utilisateur> utilisateur = utilisateurService.getUtilisateurByEmail(email);
            if (utilisateur.isPresent()) {
                return utilisateur.get().getId();
            }
            throw new RuntimeException("Membre non trouvé pour l'e-mail : " + email);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'extraction de l'ID du membre depuis le token.", e);
        }
    }
}