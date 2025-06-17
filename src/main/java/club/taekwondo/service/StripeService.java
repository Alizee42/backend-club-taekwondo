package club.taekwondo.service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;

import club.taekwondo.dto.PaiementRequestDTO;
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

    public StripeService(
        PaiementService paiementService,
        UtilisateurService utilisateurService,
        JwtUtil jwtUtil,
        EcheanceRepository echeanceRepository
    ) {
        this.paiementService = paiementService;
        this.utilisateurService = utilisateurService;
        this.jwtUtil = jwtUtil;
        this.echeanceRepository = echeanceRepository;
    }

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }

    public String getPublicKey() {
        return stripePublicKey;
    }

    public PaymentIntent executeStripePayment(String token, Map<String, Object> request) throws StripeException {
        Long utilisateurId = extractUtilisateurIdFromToken(token);

        Utilisateur utilisateur = utilisateurService.getUtilisateurById(utilisateurId)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé pour l'ID : " + utilisateurId));

        Double amount = Double.valueOf(request.get("amount").toString());
        String currency = request.get("currency").toString();
        String typePaiement = request.get("typePaiement").toString();
        String modePaiement = request.getOrDefault("modePaiement", "inconnu").toString();
        Integer nombreEcheances = request.containsKey("nombreEcheances")
            ? Integer.parseInt(request.get("nombreEcheances").toString())
            : 1;

        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Le montant ne peut pas être nul ou négatif.");
        }

        // Création de l'entité Paiement
        Paiement paiement = new Paiement();
        paiement.setMontant(amount);
        paiement.setMontantTotal("echeances".equalsIgnoreCase(typePaiement) ? amount * nombreEcheances : amount);
        paiement.setDatePaiement(LocalDate.now());
        paiement.setStatut("en attente");
        paiement.setModePaiement(modePaiement);
        paiement.setType("Cotisation");
        paiement.setUtilisateur(utilisateur);
        paiement.setEcheancesTotales(nombreEcheances);
        paiement.setEcheancesRestantes(nombreEcheances);

        paiementService.save(paiement);

        // Création des échéances si applicable
        if ("echeances".equalsIgnoreCase(typePaiement) && nombreEcheances > 1) {
            for (int i = 0; i < nombreEcheances; i++) {
                Echeance echeance = new Echeance();
                echeance.setPaiement(paiement);
                echeance.setNumero(i + 1);
                echeance.setMontant(amount);
                echeance.setDateEcheance(LocalDate.now().plusMonths(i));
                echeance.setStatut(i == 0 ? "payé" : "en attente");
                echeanceRepository.save(echeance);
            }
        }

        // Création du PaymentIntent Stripe
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
            .setAmount((long) (amount * 100)) // Montant en centimes
            .setCurrency(currency)
            .setAutomaticPaymentMethods(
                PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                    .setEnabled(true)
                    .build()
            )
            .putMetadata("utilisateurId", String.valueOf(utilisateur.getId()))
            .putMetadata("typePaiement", typePaiement)
            .putMetadata("modePaiement", modePaiement)
            .putMetadata("paiementId", String.valueOf(paiement.getId()))
            .build();

        return PaymentIntent.create(params);
    }

    public PaymentIntent executeStripePayment(String token, PaiementRequestDTO dto) throws StripeException {
        // Validation manuelle minimale (au cas où l’annotation @Valid ne suffit pas)
        if (dto.getAmount() == null || dto.getAmount() <= 0) {
            throw new IllegalArgumentException("Montant invalide.");
        }
        if (dto.getCurrency() == null || dto.getCurrency().isEmpty()) {
            throw new IllegalArgumentException("Devise requise.");
        }
        if (dto.getTypePaiement() == null || dto.getTypePaiement().isEmpty()) {
            throw new IllegalArgumentException("Type de paiement requis.");
        }

        Map<String, Object> map = new HashMap<>();
        map.put("amount", dto.getAmount());
        map.put("currency", dto.getCurrency());
        map.put("typePaiement", dto.getTypePaiement());
        map.put("modePaiement", dto.getModePaiement());
        map.put("nombreEcheances", dto.getNombreEcheances());

        return executeStripePayment(token, map);
    }

    private Long extractUtilisateurIdFromToken(String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            return utilisateurService.getUtilisateurEntityByEmail(email)
                .orElseThrow(() -> new RuntimeException("Membre non trouvé pour l'e-mail : " + email))
                .getId();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'extraction de l'ID utilisateur depuis le token.", e);
        }
    }
}


