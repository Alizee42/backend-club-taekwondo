package club.taekwondo.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;

import club.taekwondo.dto.PaiementRequestDTO;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.security.JwtUtil;
import club.taekwondo.service.jpa.UtilisateurService;

@Service
public class StripeService {

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @Value("${stripe.public.key}")
    private String stripePublicKey;

    private final UtilisateurService utilisateurService;
    private final JwtUtil jwtUtil;

    public StripeService(
        UtilisateurService utilisateurService,
        JwtUtil jwtUtil
    ) {
        this.utilisateurService = utilisateurService;
        this.jwtUtil = jwtUtil;
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

        Utilisateur utilisateur = utilisateurService.getUtilisateurEntityById(utilisateurId)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé pour l'ID : " + utilisateurId));

        // 🔹 On récupère le montant total (pas une échéance)
        Double montantTotal = Double.valueOf(request.get("amount").toString());
        String currency = request.get("currency").toString().toLowerCase();

        if (!List.of("eur", "usd").contains(currency)) {
            throw new IllegalArgumentException("Devise non supportée : " + currency);
        }

        String typePaiement = request.get("typePaiement").toString();
        String modePaiement = request.getOrDefault("modePaiement", "inconnu").toString();
        String nombreEcheances = request.containsKey("nombreEcheances") ? request.get("nombreEcheances").toString() : "1";

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
            .setAmount((long) (montantTotal * 100)) // ✅ Montant total, pas d'erreur ici
            .setCurrency(currency)
            .setAutomaticPaymentMethods(
                PaymentIntentCreateParams.AutomaticPaymentMethods.builder().setEnabled(true).build()
            )
            .putMetadata("utilisateurId", String.valueOf(utilisateur.getId()))
            .putMetadata("typePaiement", typePaiement)
            .putMetadata("modePaiement", modePaiement)
            .putMetadata("nombreEcheances", nombreEcheances) // utile si besoin en traitement
            .build();

        return PaymentIntent.create(params);
    }


    public PaymentIntent executeStripePayment(String token, PaiementRequestDTO dto) throws StripeException {
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

