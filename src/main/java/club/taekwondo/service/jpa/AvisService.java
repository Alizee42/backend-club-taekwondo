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

    // Méthode pour obtenir tous les avis
    public List<Avis> getAllAvis() {
        return avisRepository.findAll();
    }

    // Méthode pour obtenir un avis par son ID
    public Optional<Avis> getAvisById(Long id) {
        return avisRepository.findById(id);
    }

    // Méthode pour créer un nouvel avis
    public Avis createAvis(Avis avis) {
        return avisRepository.save(avis);
    }

    // Méthode pour mettre à jour un avis existant
    public Avis updateAvis(Long id, Avis avis) {
        if (avisRepository.existsById(id)) {
            avis.setId(id);
            return avisRepository.save(avis);
        }
        return null; // ou gérer l'exception
    }

    // Méthode pour supprimer un avis
    public void deleteAvis(Long id) {
        avisRepository.deleteById(id);
    }
}
