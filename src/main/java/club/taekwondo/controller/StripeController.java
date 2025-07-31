package club.taekwondo.controller;

import club.taekwondo.entity.jpa.Echeance;
import club.taekwondo.entity.jpa.Paiement;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.entity.jpa.Membre;
import club.taekwondo.security.JwtUtil;
import club.taekwondo.service.StripeService;
import club.taekwondo.service.jpa.PaiementService;
import club.taekwondo.service.jpa.UtilisateurService;
import club.taekwondo.service.jpa.MembreService;
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
    private final MembreService membreService;

    public StripeController(StripeService stripeService, UtilisateurService utilisateurService,
                            PaiementService paiementService, JwtUtil jwtUtil, MembreService membreService) {
        this.stripeService = stripeService;
        this.utilisateurService = utilisateurService;
        this.paiementService = paiementService;
        this.jwtUtil = jwtUtil;
        this.membreService = membreService;
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
        System.out.println("🔎 Token reçu : " + token);
        System.out.println("🔎 Payload reçu : " + request);

        try {
            if (token == null || !token.startsWith("Bearer ")) {
                System.out.println("❌ Token manquant ou invalide.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Token manquant ou invalide."));
            }

            String jwt = token.substring(7);
            String email = jwtUtil.extractEmail(jwt);
            System.out.println("🔎 Email extrait du token : " + email);

            Utilisateur utilisateur = utilisateurService.getUtilisateurEntityByEmail(email)
                    .orElseThrow(() -> {
                        System.out.println("❌ Utilisateur introuvable.");
                        return new RuntimeException("Utilisateur introuvable.");
                    });

            double montantTotal = Double.parseDouble(request.get("amount").toString());
            String currency = request.get("currency").toString().toLowerCase();

            System.out.println("🔎 Montant : " + montantTotal);
            System.out.println("🔎 Devise : " + currency);

            if (!currency.matches("eur|usd")) {
                System.out.println("❌ Devise non autorisée : " + currency);
                return ResponseEntity.badRequest().body(Map.of("error", "Devise non autorisée"));
            }

            String typePaiement = request.get("typePaiement").toString();
            String modePaiement = request.getOrDefault("modePaiement", "inconnu").toString();
            int nombreEcheances = request.containsKey("nombreEcheances")
                    ? Integer.parseInt(request.get("nombreEcheances").toString()) : 1;

            System.out.println("🔎 typePaiement : " + typePaiement);
            System.out.println("🔎 modePaiement : " + modePaiement);
            System.out.println("🔎 nombreEcheances : " + nombreEcheances);

            Long membreId = null;
            if (request.containsKey("membreId")) {
                try {
                    membreId = Long.parseLong(request.get("membreId").toString());
                    System.out.println("🔎 membreId reçu : " + membreId);
                } catch (Exception ex) {
                    System.out.println("❌ ID membre invalide.");
                    return ResponseEntity.badRequest().body(Map.of("error", "ID membre invalide."));
                }
            }
            if (membreId == null && request.containsKey("enfantId")) {
                try {
                    membreId = Long.parseLong(request.get("enfantId").toString());
                    System.out.println("🔎 enfantId reçu : " + membreId);
                } catch (Exception ex) {
                    System.out.println("❌ ID enfant/membre invalide.");
                    return ResponseEntity.badRequest().body(Map.of("error", "ID enfant/membre invalide."));
                }
            }
            if (membreId == null || membreId <= 0) {
                System.out.println("❌ Membre non sélectionné.");
                return ResponseEntity.badRequest().body(Map.of("error", "Membre non sélectionné."));
            }
            Membre membre = membreService.getMembreEntityById(membreId)
                .orElseThrow(() -> {
                    System.out.println("❌ Membre non trouvé.");
                    return new RuntimeException("Membre non trouvé");
                });

            System.out.println("🎯 Reçu typePaiement = " + typePaiement);
            System.out.println("🎯 Reçu modePaiement = " + modePaiement);

            Paiement paiement = new Paiement();
            paiement.setModePaiement(modePaiement);
            paiement.setType(typePaiement);
            paiement.setUtilisateur(utilisateur);
            paiement.setDatePaiement(LocalDate.now());
            paiement.setMembre(membre);

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

            request.put("amount", montantTotal);
            request.put("modePaiement", modePaiement);
            request.put("typePaiement", typePaiement);
            request.put("nombreEcheances", nombreEcheances);

            System.out.println("🟢 Envoi à StripeService.executeStripePayment avec : " + request);

            PaymentIntent paymentIntent = stripeService.executeStripePayment(jwt, request);
            System.out.println("🟢 Stripe clientSecret reçu : " + paymentIntent.getClientSecret());
            return ResponseEntity.ok(Map.of("clientSecret", paymentIntent.getClientSecret()));

        } catch (StripeException se) {
            System.out.println("❌ Erreur Stripe : " + se.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Erreur Stripe : " + se.getMessage()));
        } catch (Exception e) {
            System.out.println("❌ Erreur interne : " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur interne : " + e.getMessage()));
        }
    }
}