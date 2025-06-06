package club.taekwondo.service.jpa;

import club.taekwondo.entity.jpa.Paiement;
import club.taekwondo.repository.jpa.PaiementRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PaiementService {

    private final PaiementRepository paiementRepository;

    public PaiementService(PaiementRepository paiementRepository) {
        this.paiementRepository = paiementRepository;
    }

    public List<Paiement> getAll() {
        List<Paiement> paiements = paiementRepository.findAll();
        paiements.forEach(this::calculerPaiementDetails); // Calculer les détails pour chaque paiement
        return paiements;
    }

    public Optional<Paiement> getById(Long id) {
        return paiementRepository.findById(id);
    }

    public List<Paiement> getByMembreId(Long membreId) {
        return paiementRepository.findByUtilisateurId(membreId);
    }

    public Paiement save(Paiement paiement) {
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
    
    private void calculerPaiementDetails(Paiement paiement) {
        if (paiement.getModePaiement().equalsIgnoreCase("echeances")) {
            Double montantTotal = paiement.getMontant();
            Integer echeancesRestantes = paiement.getEcheancesRestantes();

            if (echeancesRestantes == null || echeancesRestantes <= 0) {
                echeancesRestantes = 1; // Par défaut, une échéance restante
            }

            Double montantRestant = paiement.getMontantRestant();
            if (montantRestant == null) {
                montantRestant = montantTotal; // Par défaut, tout le montant est restant
            }

            paiement.setMontantTotal(montantTotal);
            paiement.setMontantRestant(montantRestant);
            paiement.setEcheancesRestantes(echeancesRestantes);
        } else {
            paiement.setMontantTotal(paiement.getMontant());
            paiement.setMontantRestant(0.0); // Aucun montant restant pour un paiement unique
            paiement.setEcheancesRestantes(0); // Pas d'échéances restantes
        }
    }
}
