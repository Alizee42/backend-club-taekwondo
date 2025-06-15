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

import club.taekwondo.entity.jpa.Echeance;
import club.taekwondo.entity.jpa.Paiement;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.repository.jpa.EcheanceRepository;
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
    private final EcheanceRepository echeanceRepository;

    public StripeService(PaiementService paiementService, UtilisateurService utilisateurService, JwtUtil jwtUtil, EcheanceRepository echeanceRepository) {
        this.paiementService = paiementService;
        this.utilisateurService = utilisateurService;
        this.jwtUtil = jwtUtil;
        this.echeanceRepository = echeanceRepository;
    }

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }

    public PaymentIntent createPaymentIntent(Double amount, String currency) throws StripeException {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Le montant fourni pour le PaymentIntent est invalide.");
        }

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount((long) (amount * 100))
                .setCurrency(currency)
                .addPaymentMethodType("card")
                .build();

        return PaymentIntent.create(params);
    }

    public String getPublicKey() {
        return stripePublicKey;
    }

    public PaymentIntent executeStripePayment(String token, Map<String, Object> request) throws StripeException {
        Long utilisateurId = extractUtilisateurIdFromToken(token);

        Optional<Utilisateur> utilisateurOptional = utilisateurService.getUtilisateurById(utilisateurId);
        if (utilisateurOptional.isEmpty()) {
            throw new RuntimeException("Utilisateur non trouvé pour l'ID : " + utilisateurId);
        }
        Utilisateur utilisateur = utilisateurOptional.get();

        Double amount = Double.valueOf(request.get("amount").toString());
        String currency = request.get("currency").toString();

        String typePaiement = request.get("typePaiement").toString(); // "unique" ou "echeances"
        Object modePaiementObj = request.get("modePaiement");
        String modePaiement = modePaiementObj != null ? modePaiementObj.toString() : "inconnu";
        Integer nombreEcheances = request.containsKey("nombreEcheances")
                ? Integer.parseInt(request.get("nombreEcheances").toString())
                : 1;

        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Le montant du paiement est invalide.");
        }

        if ("echeances".equalsIgnoreCase(typePaiement) && nombreEcheances > 1) {
            Double montantTotal = amount * nombreEcheances;

            // Création du paiement global
            Paiement paiement = new Paiement();
            paiement.setMontant(montantTotal);
            paiement.setDatePaiement(LocalDate.now());
            paiement.setStatut("en attente");
            paiement.setModePaiement(modePaiement); 
            paiement.setType("Cotisation");
            paiement.setUtilisateur(utilisateur);

            paiementService.save(paiement);

            // Création des échéances
            for (int i = 0; i < nombreEcheances; i++) {
                Echeance echeance = new Echeance();
                echeance.setPaiement(paiement);
                echeance.setNumero(i + 1);
                echeance.setMontant(amount);
                echeance.setDateEcheance(LocalDate.now().plusMonths(i));
                echeance.setStatut(i == 0 ? "payé" : "en attente");
                echeanceRepository.save(echeance);
            }

            // Paiement Stripe uniquement pour la première échéance
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount((long) (amount * 100))
                    .setCurrency(currency)
                    .build();

            PaymentIntent paymentIntent = PaymentIntent.create(params);
            return paymentIntent;
        }

        // Cas par défaut : paiement unique
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount((long) (amount * 100))
                .setCurrency(currency)
                .build();

        PaymentIntent paymentIntent = PaymentIntent.create(params);
        creerPaiement(amount, "carte", "Cotisation", utilisateur);
        return paymentIntent;
    }

    private Long extractUtilisateurIdFromToken(String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            Optional<Utilisateur> utilisateur = utilisateurService.getUtilisateurEntityByEmail(email);
            if (utilisateur.isPresent()) {
                return utilisateur.get().getId();
            }
            throw new RuntimeException("Membre non trouvé pour l'e-mail : " + email);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'extraction de l'ID du membre depuis le token.", e);
        }
    }

    public void creerPaiement(Double montant, String modePaiement, String type, Utilisateur utilisateur) {
        if (montant == null || montant <= 0) {
            throw new IllegalArgumentException("Montant invalide pour la création du paiement.");
        }

        Optional<Paiement> paiementExistant = paiementService.findPaiementByUtilisateurAndMontantAndStatut(
                utilisateur.getId(), montant, modePaiement, "en attente");
        if (paiementExistant.isPresent()) {
            System.out.println("Un paiement similaire existe déjà. Aucun nouveau paiement créé.");
            return;
        }

        Paiement paiement = new Paiement();
        paiement.setMontant(montant);
        paiement.setDatePaiement(LocalDate.now());
        paiement.setStatut("en attente");
        paiement.setModePaiement(modePaiement);
        paiement.setType(type);
        paiement.setUtilisateur(utilisateur);

        paiementService.save(paiement);
    }
}

