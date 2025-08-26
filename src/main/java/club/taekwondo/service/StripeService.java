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
import com.stripe.net.RequestOptions;
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

    // =====================================================================
    // 🚀 NOUVELLE MÉTHODE — Option A (recommandée)
    // Crée le PaymentIntent avec metadata.paiementId + idempotency key
    // =====================================================================
    public PaymentIntent createPaymentIntentWithMetadata(Map<String, Object> req) throws StripeException {
        // amount attendu en CENTIMES ici (le controller a déjà converti)
        long amount = parseLongStrict(req.get("amount"), "amount (centimes)");
        if (amount <= 0) throw new IllegalArgumentException("Montant invalide (centimes).");

        String currency = Objects.toString(req.getOrDefault("currency", defaultCurrency), defaultCurrency).toLowerCase();
        if (!List.of("eur", "usd").contains(currency)) {
            throw new IllegalArgumentException("Devise non supportée : " + currency);
        }

        String paiementId = Objects.toString(req.get("paiementId"), "").trim();
        if (paiementId.isEmpty()) {
            throw new IllegalArgumentException("paiementId manquant (Option A: créer en BDD avant).");
        }

        String typePaiement    = toStringOrEmpty(req.get("typePaiement"));
        String nombreEcheances = toStringOrEmpty(req.get("nombreEcheances"));
        String utilisateurId   = toStringOrEmpty(req.get("utilisateurId"));
        String enfantId        = toStringOrEmpty(req.get("enfantId"));
        String modePaiement    = toStringOrEmpty(req.get("modePaiement"));

        PaymentIntentCreateParams.Builder builder = PaymentIntentCreateParams.builder()
            .setAmount(amount)
            .setCurrency(currency)
            .setAutomaticPaymentMethods(
                PaymentIntentCreateParams.AutomaticPaymentMethods.builder().setEnabled(true).build()
            )
            // ✅ Metadata indispensables
            .putMetadata("paiementId", paiementId);

        // Metadata additionnelles (facultatives)
        if (!isBlank(typePaiement))    builder.putMetadata("type", typePaiement);
        if (!isBlank(nombreEcheances)) builder.putMetadata("nombreEcheances", nombreEcheances);
        if (!isBlank(utilisateurId))   builder.putMetadata("utilisateurId", utilisateurId);
        if (!isBlank(enfantId))        builder.putMetadata("enfantId", enfantId);
        if (!isBlank(modePaiement))    builder.putMetadata("modePaiement", modePaiement);

        // ✅ Idempotency key liée au paiement en BDD (empêche PI en double sur double-clic)
        RequestOptions opts = RequestOptions.builder()
            .setIdempotencyKey("paiement-" + paiementId)
            .build();

        return PaymentIntent.create(builder.build(), opts);
    }

    // =====================================================================
    // 🔁 Méthodes "legacy" conservées pour compat si utilisées ailleurs
    // (éviter pour le flux principal)
    // =====================================================================

    /** Legacy Map overload */
    @Deprecated
    public PaymentIntent executeStripePayment(String token, Map<String, Object> request) throws StripeException {
        Long utilisateurId = extractUtilisateurIdFromToken(token);

        double montantTotal = Double.parseDouble(Objects.toString(request.get("amount"), "0"));
        if (montantTotal <= 0) throw new IllegalArgumentException("Montant invalide.");

        String currency = Objects.toString(request.get("currency"), defaultCurrency).toLowerCase();
        if (!List.of("eur", "usd").contains(currency)) {
            throw new IllegalArgumentException("Devise non supportée : " + currency);
        }

        String typePaiement    = Objects.toString(request.get("typePaiement"), "UNIQUE").toUpperCase();
        String modePaiement    = Objects.toString(request.get("modePaiement"), Objects.toString(request.get("mode"), "CB")).toUpperCase();
        String nombreEcheances = Objects.toString(request.get("nombreEcheances"), "1");
        String membreId        = Objects.toString(request.get("membreId"), Objects.toString(request.get("enfantId"), "")); // compat

        PaymentIntentCreateParams.Builder builder = PaymentIntentCreateParams.builder()
            .setAmount(Math.round(montantTotal * 100)) // en centimes
            .setCurrency(currency)
            .setAutomaticPaymentMethods(
                PaymentIntentCreateParams.AutomaticPaymentMethods.builder().setEnabled(true).build()
            )
            .putMetadata("utilisateurId", String.valueOf(utilisateurId))
            .putMetadata("type", typePaiement)
            .putMetadata("modePaiement", modePaiement)
            .putMetadata("nombreEcheances", nombreEcheances);

        if (!membreId.isBlank()) {
            builder.putMetadata("enfantId", membreId);
        }

        // ⚠️ Pas d'idempotency key ici, et pas de paiementId → éviter pour Option A
        return PaymentIntent.create(builder.build());
    }

    /** Legacy DTO overload */
    @Deprecated
    public PaymentIntent executeStripePayment(String token, PaiementRequestDTO dto) throws StripeException {
        Long utilisateurId = extractUtilisateurIdFromToken(token);

        Double montantTotal = dto.getMontantTotal(); // euros
        if (montantTotal == null || montantTotal <= 0) {
            throw new IllegalArgumentException("Montant invalide.");
        }

        String currency     = defaultCurrency.toLowerCase(); // "eur"
        String typePaiement = (dto.getTypePaiement() == null || dto.getTypePaiement().isBlank())
                ? "UNIQUE" : dto.getTypePaiement().toUpperCase();
        String modePaiement = (dto.getModePaiement() == null || dto.getModePaiement().isBlank())
                ? "CB" : dto.getModePaiement().toUpperCase();
        int nbEcheances     = (dto.getNombreEcheances() > 0) ? dto.getNombreEcheances() : 1;

        PaymentIntentCreateParams.Builder builder = PaymentIntentCreateParams.builder()
            .setAmount(Math.round(montantTotal * 100))
            .setCurrency(currency)
            .setAutomaticPaymentMethods(
                PaymentIntentCreateParams.AutomaticPaymentMethods.builder().setEnabled(true).build()
            )
            .putMetadata("utilisateurId", String.valueOf(utilisateurId))
            .putMetadata("type", typePaiement)
            .putMetadata("modePaiement", modePaiement)
            .putMetadata("nombreEcheances", String.valueOf(nbEcheances));

        if (dto.getMembreId() != null) {
            builder.putMetadata("enfantId", String.valueOf(dto.getMembreId()));
        }

        // ⚠️ Pas d'idempotency key ni paiementId → éviter pour Option A
        return PaymentIntent.create(builder.build());
    }

    // =====================================================================
    // ✅ NOUVELLE MÉTHODE — Récupérer le client_secret d’un PaymentIntent existant
    // =====================================================================
    /**
     * Récupère le clientSecret d'un PaymentIntent existant (par ID).
     * Utile pour idempotence si le front redemande un paiement déjà initié.
     */
    public String retrieveClientSecret(String paymentIntentId) {
        try {
            if (isBlank(paymentIntentId)) return null;
            PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);
            return intent != null ? intent.getClientSecret() : null;
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la récupération du PaymentIntent " + paymentIntentId + " : " + e.getMessage());
            return null;
        }
    }

    // =====================================================================
    // Helpers
    // =====================================================================

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

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String toStringOrEmpty(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private static long parseLongStrict(Object o, String fieldName) {
        try {
            if (o instanceof Number n) return n.longValue();
            return Long.parseLong(String.valueOf(o));
        } catch (Exception e) {
            throw new IllegalArgumentException("Champ invalide '" + fieldName + "': " + o);
        }
    }
}
