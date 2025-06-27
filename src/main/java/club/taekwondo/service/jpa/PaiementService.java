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
            paiement.setMontantRestant(0.0);
            paiement.setMontantPaye(paiement.getMontantTotal());
            paiement.setStatut("payé");
        } else {
            paiement.setMontantRestant(paiement.getMontantTotal());
            paiement.setMontantPaye(0.0);
            paiement.setStatut("en attente");
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

        if (paiement.getEcheances() != null && !paiement.getEcheances().isEmpty()) {
            for (Echeance echeance : paiement.getEcheances()) {
                echeanceService.delete(echeance.getId());
            }
        }

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
        Paiement paiement = new Paiement();
        paiement.setType(dto.getType());
        paiement.setModePaiement(dto.getModePaiement());
        paiement.setDatePaiement(dto.getDatePaiement());

        // 🔹 Recherche ou création de l'utilisateur
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
                nouveau.setPassword("defaultPassword");
                utilisateurOpt = Optional.of(utilisateurService.save(nouveau));
            }
        }

        if (utilisateurOpt.isEmpty()) {
            throw new RuntimeException("Utilisateur non trouvé ou informations insuffisantes.");
        }

        paiement.setUtilisateur(utilisateurOpt.get());

        // 🔹 Gestion avec échéances
        if (dto.getEcheances() != null && !dto.getEcheances().isEmpty()) {
            List<Echeance> echeances = new ArrayList<>();
            double total = 0.0;
            int numero = 1;
            int restantes = 0;
            double montantPaye = 0.0;

            for (EcheanceDTO edto : dto.getEcheances()) {
                if (edto.getMontant() == null || edto.getMontant() <= 0) {
                    throw new RuntimeException("Échéance invalide.");
                }

                Echeance echeance = new Echeance();
                echeance.setDateEcheance(edto.getDateEcheance());
                echeance.setMontant(edto.getMontant());
                echeance.setStatut(edto.getStatut() != null ? edto.getStatut() : "en attente");
                echeance.setNumero(numero++);
                echeance.setPaiement(paiement);

                echeances.add(echeance);
                total += edto.getMontant();

                if (!"payé".equalsIgnoreCase(echeance.getStatut())) {
                    restantes++;
                } else {
                    montantPaye += echeance.getMontant();
                }
            }

            paiement.setEcheances(echeances);
            paiement.setMontantTotal(total);
            paiement.setMontantPaye(montantPaye);
            paiement.setMontantRestant(total - montantPaye);
            paiement.setEcheancesTotales(echeances.size());
            paiement.setEcheancesRestantes(restantes);
            paiement.setStatut(restantes == 0 ? "payé" : "en attente");

        } else {
            // 🔹 Paiement sans échéance
            double montant = dto.getMontantTotal() != null ? dto.getMontantTotal() : 0.0;
            paiement.setMontantTotal(montant);

            String mode = paiement.getModePaiement() != null ? paiement.getModePaiement().toLowerCase() : "";

            if (mode.equals("espèces") || mode.equals("virement") || mode.equals("chèque") || mode.equals("unique")) {
                paiement.setMontantPaye(montant);
                paiement.setMontantRestant(0.0);
                paiement.setStatut("payé");
            } else {
                paiement.setMontantPaye(0.0);
                paiement.setMontantRestant(montant);
                paiement.setStatut("en attente");
            }

            paiement.setEcheancesTotales(0);
            paiement.setEcheancesRestantes(0);
            paiement.setEcheances(null);
        }

        return paiementRepository.save(paiement);
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

        // ✅ calcul du montant payé
        double montantPaye = (paiement.getMontantTotal() != null ? paiement.getMontantTotal() : 0.0)
                           - (paiement.getMontantRestant() != null ? paiement.getMontantRestant() : 0.0);
        dto.setMontantPaye(montantPaye);

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


