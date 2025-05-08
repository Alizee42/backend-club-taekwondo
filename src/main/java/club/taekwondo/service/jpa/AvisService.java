package club.taekwondo.service.jpa;

import club.taekwondo.entity.jpa.Avis;
import club.taekwondo.repository.jpa.AvisRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AvisService {

    @Autowired
    private AvisRepository avisRepository;

    // Récupérer tous les avis
    public List<Avis> getAllAvis() {
        return avisRepository.findAll();
    }

    // Récupérer un avis par son ID
    public Optional<Avis> getAvisById(Integer id) {
        return avisRepository.findById(id);
    }

    // Ajouter un nouvel avis
    public Avis createAvis(Avis avis) {
        return avisRepository.save(avis);
    }

    // Mettre à jour un avis existant
    public Avis updateAvis(Integer id, Avis avisDetails) {
        return avisRepository.findById(id).map(avis -> {
            if (avisDetails.getContenu() != null) {
                avis.setContenu(avisDetails.getContenu());
            }
            if (avisDetails.getPseudoVisiteur() != null) {
                avis.setPseudoVisiteur(avisDetails.getPseudoVisiteur());
            }
            if (avisDetails.getNote() != null) {
                avis.setNote(avisDetails.getNote());
            }
            if (avisDetails.getTypeAvis() != null) {
                avis.setTypeAvis(avisDetails.getTypeAvis());
            }
            return avisRepository.save(avis);
        }).orElseThrow(() -> new RuntimeException("Avis non trouvé avec l'ID : " + id));
    }

    // Approuver un avis
    public Avis approuverAvis(Integer id) {
        return avisRepository.findById(id).map(avis -> {
            avis.setApprouve(true); // Mettre à jour le statut d'approbation
            return avisRepository.save(avis); // Sauvegarder les modifications
        }).orElseThrow(() -> new RuntimeException("Avis non trouvé avec l'ID : " + id));
    }

    // Supprimer un avis
    public void deleteAvis(Integer id) {
        avisRepository.deleteById(id);
    }
}