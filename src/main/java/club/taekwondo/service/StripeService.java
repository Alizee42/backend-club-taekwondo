package club.taekwondo.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;

import club.taekwondo.dto.PaiementRequestDTO;
import club.taekwondo.security.JwtUtil;
import club.taekwondo.service.jpa.UtilisateurService;

@Service
public class StripeService {

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @Value("${stripe.public.key}")
    private String stripePublicKey;

    // ✅ Devise par défaut si non fournie (on force EUR)
    @Value("${stripe.default.currency:eur}")
    private String defaultCurrency;

    private final UtilisateurService utilisateurService;
    private final JwtUtil jwtUtil;

    public StripeService(UtilisateurService utilisateurService, JwtUtil jwtUtil) {
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

    /* ---------- Surcharge 1 : Entrée de type Map (compat legacy) ---------- */
    public PaymentIntent executeStripePayment(String token, Map<String, Object> request) throws StripeException {
        Long utilisateurId = extractUtilisateurIdFromToken(token);

        // Montant total (obligatoire)
        double montantTotal = Double.parseDouble(Objects.toString(request.get("amount"), "0"));
        if (montantTotal <= 0) throw new IllegalArgumentException("Montant invalide.");

        // Devise (si non fournie → EUR)
        String currency = Objects.toString(request.get("currency"), defaultCurrency).toLowerCase();
        if (!List.of("eur", "usd").contains(currency)) {
            throw new IllegalArgumentException("Devise non supportée : " + currency);
        }

        String typePaiement = Objects.toString(request.get("typePaiement"), "UNIQUE");
        String modePaiement = Objects.toString(request.get("modePaiement"), "CB");
        String nombreEcheances = Objects.toString(request.get("nombreEcheances"), "1");
        String membreId = Objects.toString(request.get("membreId"), ""); // facultatif

        PaymentIntentCreateParams.Builder builder = PaymentIntentCreateParams.builder()
            .setAmount(Math.round(montantTotal * 100)) // en cents
            .setCurrency(currency)
            .setAutomaticPaymentMethods(
                PaymentIntentCreateParams.AutomaticPaymentMethods.builder().setEnabled(true).build()
            )
            .putMetadata("utilisateurId", String.valueOf(utilisateurId))
            .putMetadata("typePaiement", typePaiement)
            .putMetadata("modePaiement", modePaiement)
            .putMetadata("nombreEcheances", nombreEcheances);

        if (!membreId.isBlank()) {
            builder.putMetadata("membreId", membreId);
        }

        return PaymentIntent.create(builder.build());
    }

    /* ---------- Surcharge 2 : Entrée de type PaiementRequestDTO (NOUVEAU DTO) ---------- */
    public PaymentIntent executeStripePayment(String token, PaiementRequestDTO dto) throws StripeException {
        Long utilisateurId = extractUtilisateurIdFromToken(token);

        // ✅ On lit le NOUVEAU DTO
        Double montantTotal = dto.getMontantTotal(); // remplace l'ancien amount
        if (montantTotal == null || montantTotal <= 0) {
            throw new IllegalArgumentException("Montant invalide.");
        }

        // On force la devise à EUR (tu peux rendre ça configurable si besoin)
        String currency = defaultCurrency.toLowerCase(); // "eur"

        String typePaiement = (dto.getTypePaiement() == null || dto.getTypePaiement().isBlank())
                ? "UNIQUE" : dto.getTypePaiement().toUpperCase();
        String modePaiement = (dto.getModePaiement() == null || dto.getModePaiement().isBlank())
                ? "CB" : dto.getModePaiement().toUpperCase();
        int nbEcheances = (dto.getNombreEcheances() > 0) ? dto.getNombreEcheances() : 1;

        PaymentIntentCreateParams.Builder builder = PaymentIntentCreateParams.builder()
            .setAmount(Math.round(montantTotal * 100))
            .setCurrency(currency)
            .setAutomaticPaymentMethods(
                PaymentIntentCreateParams.AutomaticPaymentMethods.builder().setEnabled(true).build()
            )
            .putMetadata("utilisateurId", String.valueOf(utilisateurId))
            .putMetadata("typePaiement", typePaiement)
            .putMetadata("modePaiement", modePaiement)
            .putMetadata("nombreEcheances", String.valueOf(nbEcheances));

        // Optionnel : si ton DTO contient un membre cible
        if (dto.getMembreId() != null) {
            builder.putMetadata("membreId", String.valueOf(dto.getMembreId()));
        }

        return PaymentIntent.create(builder.build());
    }

    /* ---------- Helpers ---------- */

    private Long extractUtilisateurIdFromToken(String bearerToken) {
        try {
            String token = bearerToken.replace("Bearer ", "");
            String email = jwtUtil.extractEmail(token);
            return utilisateurService.getUtilisateurEntityByEmail(email)
                .orElseThrow(() -> new RuntimeException("Membre non trouvé pour l'e-mail : " + email))
                .getId();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'extraction de l'ID utilisateur depuis le token.", e);
        }
    }
}

