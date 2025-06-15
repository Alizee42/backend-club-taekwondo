package club.taekwondo.service.jpa;

import club.taekwondo.dto.*;
import club.taekwondo.entity.jpa.Echeance;
import club.taekwondo.entity.jpa.Paiement;
import club.taekwondo.repository.jpa.PaiementRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class PaiementService {

    private final PaiementRepository paiementRepository;

    public PaiementService(PaiementRepository paiementRepository) {
        this.paiementRepository = paiementRepository;
    }

    public List<PaiementDTO> getAllWithEcheances() {
        List<PaiementDTO> paiementsDTO = new ArrayList<>();
        List<Paiement> paiements = paiementRepository.findAllWithEcheances();
        for (Paiement paiement : paiements) {
            paiementsDTO.add(toPaiementDto(paiement));
        }
        return paiementsDTO;
    }

    public Optional<Paiement> getById(Long id) {
        return paiementRepository.findById(id);
    }

    public List<Paiement> getByMembreId(Long membreId) {
        return paiementRepository.findByUtilisateurId(membreId);
    }

    public Optional<Paiement> findPaiementByUtilisateurAndMontantAndStatut(Long utilisateurId, Double montantTotal, String modePaiement, String statut) {
        return paiementRepository.findPaiementByUtilisateurAndMontantAndStatut(utilisateurId, montantTotal, modePaiement, statut);
    }

    public Paiement save(Paiement paiement) {
        if (paiement.getMontant() == null || paiement.getMontant() <= 0) {
            throw new IllegalArgumentException("Le montant ne peut pas être nul ou négatif.");
        }

        if ("unique".equalsIgnoreCase(paiement.getModePaiement())) {
            paiement.setEcheances(null);
            paiement.setEcheancesRestantes(0);
        }

        Optional<Paiement> duplicatePaiement = findPaiementByUtilisateurAndMontantAndStatut(
    paiement.getUtilisateur().getId(),
    paiement.getMontantTotal(),
    paiement.getModePaiement(), // Ajout du mode de paiement ici
    "en attente"
);;

        if (duplicatePaiement.isPresent()) {
            throw new RuntimeException("Un paiement similaire existe déjà.");
        }

        if (paiement.getType() == null || paiement.getType().isEmpty()) {
            paiement.setType("standard");
        }

        return paiementRepository.save(paiement);
    }

    public void delete(Long id) {
        paiementRepository.deleteById(id);
    }

    public List<Paiement> filterPaiements(String statut, String modePaiement) {
        List<Paiement> paiements = paiementRepository.findAll();
        if (statut != null) {
            paiements = paiements.stream()
                    .filter(p -> p.getStatut().equalsIgnoreCase(statut))
                    .toList();
        }
        if (modePaiement != null) {
            paiements = paiements.stream()
                    .filter(p -> p.getModePaiement().equalsIgnoreCase(modePaiement))
                    .toList();
        }
        return paiements;
    }

    public void calculerPaiementDetails(Paiement paiement) {
        if ("echeances".equalsIgnoreCase(paiement.getModePaiement())) {
            paiement.setEcheancesRestantes(
                paiement.getEcheancesRestantes() == null || paiement.getEcheancesRestantes() <= 0 ? 1 : paiement.getEcheancesRestantes()
            );
            paiement.setMontantRestant(
                paiement.getMontantRestant() == null ? paiement.getMontantTotal() : paiement.getMontantRestant()
            );
        } else {
            paiement.setMontantRestant(0.0);
            paiement.setEcheancesRestantes(0);
        }
    }

    public DashboardStatsDTO buildDashboardStats() {
        LocalDate today = LocalDate.now();
        LocalDate firstDayMonth = today.withDayOfMonth(1);
        LocalDate minus30 = today.minusDays(30);

        try {
            List<Paiement> paiements = paiementRepository.findAll();

            double totalPayes = paiements.stream()
                    .filter(p -> "payé".equalsIgnoreCase(p.getStatut()))
                    .mapToDouble(p -> safeMontant(p.getMontantTotal()) - safeMontant(p.getMontantRestant()))
                    .sum();

            double totalAttente = paiements.stream()
                    .filter(p -> "en attente".equalsIgnoreCase(p.getStatut()))
                    .mapToDouble(p -> safeMontant(p.getMontantTotal()) - safeMontant(p.getMontantRestant()))
                    .sum();

            double totalAnnules = paiements.stream()
                    .filter(p -> "annulé".equalsIgnoreCase(p.getStatut()))
                    .mapToDouble(p -> safeMontant(p.getMontantTotal()) - safeMontant(p.getMontantRestant()))
                    .sum();

            double montantTotalMois = paiementRepository.sumByDatePaiementBetween(firstDayMonth, today);
            double montantPayesMois = paiementRepository.sumByStatutAndDatePaiementBetween("payé", firstDayMonth, today);
            double pctMois = montantTotalMois == 0 ? 0 : (montantPayesMois / montantTotalMois) * 100;

            var courbe = paiementRepository.sumByDay(minus30);
            var topRows = paiementRepository.topRetards(PageRequest.of(0, 5));
            var top = topRows.stream()
                    .map(o -> new MembreRetardDTO((String) o[0], (Double) o[1]))
                    .toList();

            return new DashboardStatsDTO(
                    totalPayes, totalAttente, totalAnnules,
                    pctMois, courbe, top);
        } catch (Exception e) {
            System.err.println("Erreur lors de la génération des statistiques : " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la génération des statistiques", e);
        }
    }

    private double safeMontant(Double montant) {
        return montant != null ? montant : 0.0;
    }

    private PaiementDTO toPaiementDto(Paiement paiement) {
        PaiementDTO paiementDTO = new PaiementDTO();
        paiementDTO.setId(paiement.getId());
        paiementDTO.setType(paiement.getType());
        paiementDTO.setMontant(paiement.getMontant());
        paiementDTO.setDatePaiement(paiement.getDatePaiement());
        paiementDTO.setStatut(paiement.getStatut());
        paiementDTO.setModePaiement(paiement.getModePaiement());
        paiementDTO.setUtilisateurId(paiement.getUtilisateur().getId());

        List<EcheanceDTO> listEcheanceDTO = new ArrayList<>();
        if (paiement.getEcheances() != null) {
            for (Echeance echeance : paiement.getEcheances()) {
                EcheanceDTO echeanceDTO = new EcheanceDTO();
                echeanceDTO.setId(echeance.getId());
                echeanceDTO.setDateEcheance(echeance.getDateEcheance());
                echeanceDTO.setMontant(echeance.getMontant());
                echeanceDTO.setStatut(echeance.getStatut());
                listEcheanceDTO.add(echeanceDTO);
            }
        }
        paiementDTO.setEcheances(listEcheanceDTO);
        return paiementDTO;
    }
}