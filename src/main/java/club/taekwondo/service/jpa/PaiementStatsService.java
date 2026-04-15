package club.taekwondo.service.jpa;

import club.taekwondo.dto.DashboardStatsDTO;
import club.taekwondo.dto.DaySumDTO;
import club.taekwondo.dto.MembreRetardDTO;
import club.taekwondo.entity.jpa.Echeance;
import club.taekwondo.entity.jpa.Paiement;
import club.taekwondo.repository.jpa.PaiementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Calcul des statistiques dashboard.
 * Extrait de PaiementService pour isoler la responsabilité reporting.
 */
@Service
public class PaiementStatsService {

    private static final Logger log = LoggerFactory.getLogger(PaiementStatsService.class);

    private final PaiementRepository paiementRepository;
    private final EcheanceService echeanceService;

    public PaiementStatsService(PaiementRepository paiementRepository,
                                 EcheanceService echeanceService) {
        this.paiementRepository = paiementRepository;
        this.echeanceService = echeanceService;
    }

    public DashboardStatsDTO buildDashboardStats(Long clubId) {
        try {
            LocalDate today = LocalDate.now();
            LocalDate firstDayMonth = today.withDayOfMonth(1);
            LocalDate minus30 = today.minusDays(30);

            List<Paiement> paiements = (clubId != null)
                    ? paiementRepository.findByClubIdAny(clubId)
                    : paiementRepository.findAll();

            double totalPayes = 0.0;
            double totalAttente = 0.0;
            double totalAnnules = 0.0;

            for (Paiement paiement : paiements) {
                double montantPaye = 0.0;
                double montantRestant = 0.0;

                if (paiement.getEcheances() != null && !paiement.getEcheances().isEmpty()) {
                    for (Echeance e : paiement.getEcheances()) {
                        if ("payé".equalsIgnoreCase(e.getStatut())) {
                            montantPaye += safe(e.getMontant());
                        } else if ("en attente".equalsIgnoreCase(e.getStatut())) {
                            montantRestant += safe(e.getMontant());
                        } else if ("annulé".equalsIgnoreCase(e.getStatut())) {
                            totalAnnules += safe(e.getMontant());
                        }
                    }
                } else {
                    if ("payé".equalsIgnoreCase(paiement.getStatut())) {
                        montantPaye = safe(paiement.getMontantTotal());
                    } else if ("en attente".equalsIgnoreCase(paiement.getStatut())) {
                        montantRestant = safe(paiement.getMontantRestant());
                        montantPaye = safe(paiement.getMontantTotal()) - montantRestant;
                    } else if ("annulé".equalsIgnoreCase(paiement.getStatut())) {
                        totalAnnules += Math.max(0.0, safe(paiement.getMontantTotal()) - safe(paiement.getMontantPaye()));
                        montantPaye = safe(paiement.getMontantPaye());
                    }
                }

                if ("payé".equalsIgnoreCase(paiement.getStatut())) {
                    totalPayes += montantPaye;
                } else if ("en attente".equalsIgnoreCase(paiement.getStatut())) {
                    totalPayes += montantPaye;
                    totalAttente += montantRestant;
                } else if ("annulé".equalsIgnoreCase(paiement.getStatut())) {
                    totalPayes += montantPaye;
                }
            }

            double montantTotalMois;
            double montantPayesMois;
            List<DaySumDTO> courbe;

            if (clubId == null) {
                // SUPER_ADMIN : requêtes SQL directes (plus performantes)
                Double mois = paiementRepository.sumByDatePaiementBetween(firstDayMonth, today);
                Double payes = paiementRepository.sumByStatutAndDatePaiementBetween("payé", firstDayMonth, today);
                montantTotalMois = (mois != null) ? mois : 0.0;
                montantPayesMois = (payes != null) ? payes : 0.0;
                List<DaySumDTO> raw = paiementRepository.sumByDay(minus30, today);
                courbe = (raw != null) ? raw : new ArrayList<>();
            } else {
                // ADMIN : calcul depuis la liste filtrée
                montantTotalMois = paiements.stream()
                        .filter(p -> p.getDatePaiement() != null
                                && !p.getDatePaiement().isBefore(firstDayMonth)
                                && !p.getDatePaiement().isAfter(today))
                        .mapToDouble(p -> safe(p.getMontantTotal()))
                        .sum();
                montantPayesMois = paiements.stream()
                        .filter(p -> "payé".equalsIgnoreCase(p.getStatut())
                                && p.getDatePaiement() != null
                                && !p.getDatePaiement().isBefore(firstDayMonth)
                                && !p.getDatePaiement().isAfter(today))
                        .mapToDouble(p -> safe(p.getMontantTotal()))
                        .sum();
                Map<LocalDate, Double> dayMap = new TreeMap<>();
                for (Paiement p : paiements) {
                    if (p.getDatePaiement() != null
                            && !p.getDatePaiement().isBefore(minus30)
                            && !p.getDatePaiement().isAfter(today)) {
                        dayMap.merge(p.getDatePaiement(), safe(p.getMontantTotal()), Double::sum);
                    }
                }
                courbe = dayMap.entrySet().stream()
                        .map(e -> new DaySumDTO(e.getKey(), e.getValue()))
                        .collect(Collectors.toList());
            }

            double pctMois = montantTotalMois == 0 ? 0 : (montantPayesMois / montantTotalMois) * 100;

            List<MembreRetardDTO> allRetards = echeanceService.getMembresEnRetard();
            List<MembreRetardDTO> top;
            if (clubId != null) {
                Set<Long> clubUserIds = paiements.stream()
                        .filter(p -> p.getUtilisateur() != null)
                        .map(p -> p.getUtilisateur().getId())
                        .collect(Collectors.toSet());
                top = allRetards.stream()
                        .filter(r -> r.getUtilisateurId() != null && clubUserIds.contains(r.getUtilisateurId()))
                        .collect(Collectors.toList());
            } else {
                top = allRetards;
            }

            return new DashboardStatsDTO(
                    totalPayes,
                    totalAttente,
                    totalAnnules,
                    pctMois,
                    courbe,
                    top != null ? top : new ArrayList<>()
            );
        } catch (Exception e) {
            log.error("Erreur buildDashboardStats", e);
            return new DashboardStatsDTO(0, 0, 0, 0, new ArrayList<>(), new ArrayList<>());
        }
    }

    private double safe(Double v) {
        return v != null ? v : 0.0;
    }
}
