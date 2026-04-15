package club.taekwondo.service.jpa;

import club.taekwondo.entity.jpa.Commande;
import club.taekwondo.entity.jpa.Echeance;
import club.taekwondo.entity.jpa.Paiement;
import club.taekwondo.repository.jpa.CommandeRepository;
import club.taekwondo.repository.jpa.PaiementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Opérations BDD déclenchées par le flux Stripe (webhook + controller).
 * Extrait de PaiementService pour isoler les mutations liées au paiement en ligne.
 */
@Service
public class PaiementStripeService {

    private static final Logger log = LoggerFactory.getLogger(PaiementStripeService.class);

    private final PaiementRepository paiementRepository;
    private final EcheanceService echeanceService;
    private final CommandeRepository commandeRepository;

    public PaiementStripeService(PaiementRepository paiementRepository,
                                  EcheanceService echeanceService,
                                  CommandeRepository commandeRepository) {
        this.paiementRepository = paiementRepository;
        this.echeanceService = echeanceService;
        this.commandeRepository = commandeRepository;
    }

    /** Persiste l'ID du PaymentIntent Stripe sur l'échéance (avant confirmation). */
    @Transactional
    public void saveEcheanceReference(Long echeanceId, String paymentIntentId) {
        if (echeanceId == null || paymentIntentId == null || paymentIntentId.isBlank()) {
            throw new IllegalArgumentException("Paramètres invalides pour saveEcheanceReference");
        }
        Echeance e = echeanceService.getEcheanceEntityById(echeanceId)
                .orElseThrow(() -> new NoSuchElementException("Échéance introuvable id=" + echeanceId));
        e.setReference(paymentIntentId);
        echeanceService.save(e);
        log.info("Référence PaymentIntent {} enregistrée sur échéance {}", paymentIntentId, echeanceId);
    }

    /** Marque une échéance (ou un paiement unique) comme payé suite à un événement Stripe. */
    @Transactional
    public void marquerEcheancePayeeParStripe(Long paiementId, Long echeanceId,
                                              String paymentIntentId, Long amountCents) {
        if (echeanceId == null) {
            if (paiementId == null) return;
            Paiement p = paiementRepository.findById(paiementId)
                    .orElseThrow(() -> new NoSuchElementException("Paiement introuvable id=" + paiementId));
            p.setModePaiement("CB");
            p.setStatut("payé");
            p.setMontantPaye(safe(p.getMontantTotal()));
            p.setMontantRestant(0.0);
            p.setDatePaiement(LocalDate.now());
            p.setPaymentIntentId(paymentIntentId);
            paiementRepository.save(p);
            marquerCommandePayee(p.getCommande(), "CB");
            log.info("Paiement unique {} marqué payé via Stripe (PI={})", p.getId(), paymentIntentId);
            return;
        }

        Echeance e = echeanceService.getEcheanceEntityById(echeanceId)
                .orElseThrow(() -> new NoSuchElementException("Échéance introuvable id=" + echeanceId));
        Paiement p = e.getPaiement();
        if (paiementId != null && (p == null || !Objects.equals(p.getId(), paiementId))) {
            log.warn("Incohérence webhook: paiementId={} ne matche pas la paiement de l'échéance ({}).",
                    paiementId, (p != null ? p.getId() : null));
        }

        if (amountCents != null && e.getMontant() != null) {
            long expected = Math.round(e.getMontant() * 100.0);
            if (!Objects.equals(expected, amountCents)) {
                log.warn("Montant Stripe ({}) différent du montant échéance attendu ({}).", amountCents, expected);
            }
        }

        e.setStatut("payé");
        e.setDatePaiementReel(LocalDate.now());
        e.setReference(paymentIntentId);
        e.setModePaiement("CB");
        echeanceService.save(e);

        if (p != null) {
            p.setModePaiement("CB");
            recomputeAggregates(p);
            paiementRepository.save(p);
            if ("payé".equalsIgnoreCase(p.getStatut())
                    || p.getMontantRestant() == null
                    || p.getMontantRestant() <= 0.0) {
                marquerCommandePayee(p.getCommande(), "CB");
            }
            log.info("Échéance {} du paiement {} marquée payée (PI={})", echeanceId, p.getId(), paymentIntentId);
        }
    }

    // ---- helpers privés ----

    private void marquerCommandePayee(Commande cmd, String mode) {
        if (cmd == null) return;
        cmd.setStatut("PAYEE");
        cmd.setModePaiement(mode != null ? mode.toUpperCase() : "CB");
        cmd.setDatePaiement(LocalDate.now());
        commandeRepository.save(cmd);
    }

    private void recomputeAggregates(Paiement p) {
        double paye = 0.0;
        double restant = 0.0;
        int restantes = 0;

        if (p.getEcheances() != null && !p.getEcheances().isEmpty()) {
            for (Echeance e : p.getEcheances()) {
                double m = safe(e.getMontant());
                if ("payé".equalsIgnoreCase(e.getStatut())) {
                    paye += m;
                } else if (!"annulé".equalsIgnoreCase(e.getStatut())) {
                    restant += m;
                    restantes++;
                }
            }
            p.setEcheancesTotales(p.getEcheances().size());
            p.setEcheancesRestantes(restantes);
            p.setMontantPaye(paye);
            p.setMontantRestant(restant);
            p.setStatut(restant <= 0.0 ? "payé" : "en attente");
        } else {
            double total = safe(p.getMontantTotal());
            if ("payé".equalsIgnoreCase(p.getStatut())) {
                p.setMontantPaye(total);
                p.setMontantRestant(0.0);
            } else {
                p.setMontantPaye(0.0);
                p.setMontantRestant(total);
                if (!"annulé".equalsIgnoreCase(p.getStatut())) {
                    p.setStatut("en attente");
                }
            }
            p.setEcheancesTotales(0);
            p.setEcheancesRestantes(0);
        }
    }

    private double safe(Double v) {
        return v != null ? v : 0.0;
    }
}
