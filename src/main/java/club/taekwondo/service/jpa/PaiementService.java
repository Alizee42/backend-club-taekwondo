package club.taekwondo.service.jpa;

import club.taekwondo.dto.*;
import club.taekwondo.entity.jpa.Echeance;
import club.taekwondo.entity.jpa.Paiement;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.repository.jpa.PaiementRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class PaiementService {

    private final PaiementRepository paiementRepository;
    private final EcheanceService echeanceService;
    private final UtilisateurService utilisateurService;

    public PaiementService(
        PaiementRepository paiementRepository,
        EcheanceService echeanceService,
        UtilisateurService utilisateurService
    ) {
        this.paiementRepository = paiementRepository;
        this.echeanceService = echeanceService;
        this.utilisateurService = utilisateurService;
    }

    public List<PaiementDTO> getAllWithEcheances() {
        List<Paiement> paiements = paiementRepository.findAllWithEcheances();
        List<PaiementDTO> dtos = new ArrayList<>();
        for (Paiement paiement : paiements) {
            dtos.add(toPaiementDTO(paiement));
        }
        return dtos;
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
        if (paiement.getMontantTotal() == null || paiement.getMontantTotal() <= 0) {
            throw new IllegalArgumentException("Le montant total ne peut pas être nul ou négatif.");
        }

        if ("unique".equalsIgnoreCase(paiement.getModePaiement())) {
            paiement.setEcheances(null);
            paiement.setEcheancesRestantes(0);
        }

        Optional<Paiement> duplicate = findPaiementByUtilisateurAndMontantAndStatut(
            paiement.getUtilisateur().getId(),
            paiement.getMontantTotal(),
            paiement.getModePaiement(),
            "en attente"
        );
        if (duplicate.isPresent()) {
            throw new RuntimeException("Un paiement similaire existe déjà.");
        }

        if (paiement.getType() == null || paiement.getType().isEmpty()) {
            paiement.setType("standard");
        }

        return paiementRepository.save(paiement);
    }

    public void delete(Long id) {
        Optional<Paiement> paiementOpt = paiementRepository.findById(id);
        if (paiementOpt.isEmpty()) {
            throw new RuntimeException("Paiement introuvable avec l'ID : " + id);
        }

        Paiement paiement = paiementOpt.get();

        // Supprimer les échéances associées
        if (paiement.getEcheances() != null && !paiement.getEcheances().isEmpty()) {
            for (Echeance echeance : paiement.getEcheances()) {
                echeanceService.delete(echeance.getId()); // Appelle le service pour supprimer chaque échéance
            }
        }

        // Supprimer le paiement
        paiementRepository.deleteById(id);
        System.out.println("Paiement avec ID " + id + " supprimé avec succès.");
    }

    public List<Paiement> filterPaiements(String statut, String modePaiement) {
        List<Paiement> paiements = paiementRepository.findAll();
        if (statut != null) {
            paiements = paiements.stream().filter(p -> p.getStatut().equalsIgnoreCase(statut)).toList();
        }
        if (modePaiement != null) {
            paiements = paiements.stream().filter(p -> p.getModePaiement().equalsIgnoreCase(modePaiement)).toList();
        }
        return paiements;
    }

    public void calculerPaiementDetails(Paiement paiement) {
        if ("echeances".equalsIgnoreCase(paiement.getModePaiement())) {
            paiement.setEcheancesRestantes(
                paiement.getEcheancesRestantes() == null || paiement.getEcheancesRestantes() <= 0
                    ? 1
                    : paiement.getEcheancesRestantes()
            );
            paiement.setMontantRestant(
                paiement.getMontantRestant() == null
                    ? paiement.getMontantTotal()
                    : paiement.getMontantRestant()
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
                .filter(p -> !"annulé".equalsIgnoreCase(p.getStatut()))
                .mapToDouble(p -> safeMontant(p.getMontantTotal()) - safeMontant(p.getMontantRestant()))
                .sum();

            double totalAttente = paiements.stream()
                .filter(p -> !"annulé".equalsIgnoreCase(p.getStatut()))
                .mapToDouble(p -> safeMontant(p.getMontantRestant()))
                .sum();

            double totalAnnules = paiements.stream()
                .filter(p -> "annulé".equalsIgnoreCase(p.getStatut()))
                .mapToDouble(p -> safeMontant(p.getMontantTotal()))
                .sum();

            Double montantTotalMois = paiementRepository.sumByDatePaiementBetween(firstDayMonth, today);
            Double montantPayesMois = paiementRepository.sumByStatutAndDatePaiementBetween("payé", firstDayMonth, today);
            montantTotalMois = montantTotalMois != null ? montantTotalMois : 0.0;
            montantPayesMois = montantPayesMois != null ? montantPayesMois : 0.0;

            double pctMois = montantTotalMois == 0 ? 0 : (montantPayesMois / montantTotalMois) * 100;

            List<DaySumDTO> courbe = paiementRepository.sumByDay(minus30);
            List<MembreRetardDTO> top = echeanceService.getMembresEnRetard();

            return new DashboardStatsDTO(totalPayes, totalAttente, totalAnnules, pctMois, courbe, top);

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la génération des statistiques : " + e.getMessage());
            throw new RuntimeException("Erreur lors de la génération des statistiques", e);
        }
    }

    private double safeMontant(Double montant) {
        return montant != null ? montant : 0.0;
    }

    public Paiement ajouterPaiementManuel(PaiementDTO dto) {
        try {
            // Création du paiement
            Paiement paiement = new Paiement();
            paiement.setType(dto.getType());
            paiement.setModePaiement(dto.getModePaiement());
            paiement.setMontantTotal(dto.getMontantTotal());
            paiement.setMontantRestant(dto.getMontantTotal()); // Montant restant initialisé au montant total
            paiement.setStatut("en attente"); // Statut par défaut
            paiement.setDatePaiement(dto.getDatePaiement());

            // Recherche ou création de l'utilisateur
            Optional<Utilisateur> utilisateurOpt = Optional.empty();

            if (dto.getUtilisateurId() != null) {
                utilisateurOpt = utilisateurService.getUtilisateurEntityById(dto.getUtilisateurId());
            } else if (dto.getUtilisateurNom() != null && dto.getUtilisateurPrenom() != null) {
                utilisateurOpt = utilisateurService.findByNomPrenom(dto.getUtilisateurNom(), dto.getUtilisateurPrenom());

                if (utilisateurOpt.isEmpty()) {
                    Utilisateur nouveau = new Utilisateur();
                    nouveau.setNom(dto.getUtilisateurNom());
                    nouveau.setPrenom(dto.getUtilisateurPrenom());
                    nouveau.setEmail(dto.getUtilisateurEmail() != null ? dto.getUtilisateurEmail() : "noemail@carelink.local");
                    nouveau.setRole("membre");
                    nouveau.setPassword("defaultPassword"); // Ajout d'une valeur par défaut
                    utilisateurOpt = Optional.of(utilisateurService.save(nouveau));
                }
            }

            if (utilisateurOpt.isEmpty()) {
                throw new RuntimeException("Utilisateur non trouvé ou informations insuffisantes.");
            }

            paiement.setUtilisateur(utilisateurOpt.get());

            // Gestion des échéances
            if (dto.getEcheances() != null && !dto.getEcheances().isEmpty()) {
                List<Echeance> echeances = new ArrayList<>();
                for (EcheanceDTO edto : dto.getEcheances()) {
                	if (edto.getMontant() == null || edto.getMontant() <= 0) {
                	    throw new RuntimeException("Échéance invalide : date ou montant manquant.");
                	}

                    Echeance echeance = new Echeance();
                    echeance.setDateEcheance(edto.getDateEcheance());
                    echeance.setMontant(edto.getMontant());
                    echeance.setStatut("en attente"); // Statut par défaut pour les échéances
                    echeance.setPaiement(paiement); // Association de l'échéance au paiement
                    echeances.add(echeance);
                }
                paiement.setEcheances(echeances);
            }

            // Sauvegarde du paiement
            Paiement savedPaiement = paiementRepository.save(paiement);
            System.out.println("Paiement manuel ajouté avec succès : " + savedPaiement);
            return savedPaiement;

        } catch (Exception e) {
            System.err.println("Erreur lors de l'ajout manuel du paiement : " + e.getMessage());
            throw e;
        }
    }


    public PaiementDTO toPaiementDTO(Paiement paiement) {
        PaiementDTO dto = new PaiementDTO();
        dto.setId(paiement.getId());
        dto.setType(paiement.getType());
        dto.setDatePaiement(paiement.getDatePaiement());
        dto.setStatut(paiement.getStatut());
        dto.setModePaiement(paiement.getModePaiement());
        dto.setUtilisateurId(paiement.getUtilisateur().getId());
        dto.setMontantTotal(paiement.getMontantTotal());
        dto.setMontantRestant(paiement.getMontantRestant());
        dto.setUtilisateurNom(paiement.getUtilisateur().getNom());
        dto.setUtilisateurPrenom(paiement.getUtilisateur().getPrenom());
        dto.setUtilisateurEmail(paiement.getUtilisateur().getEmail());

        if (paiement.getEcheances() != null) {
            List<EcheanceDTO> liste = new ArrayList<>();
            for (Echeance e : paiement.getEcheances()) {
                EcheanceDTO edto = new EcheanceDTO();
                edto.setId(e.getId());
                edto.setDateEcheance(e.getDateEcheance());
                edto.setMontant(e.getMontant());
                edto.setStatut(e.getStatut());
                edto.setNumero(e.getNumero());
                liste.add(edto);
            }
            dto.setEcheances(liste);
        }

        return dto;
    }
}
