package club.taekwondo.service.jpa;

import club.taekwondo.entity.jpa.Evenement;
import club.taekwondo.repository.jpa.EvenementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EvenementService {

    @Autowired
    private EvenementRepository evenementRepository;

    // Méthode pour récupérer tous les événements
    public List<Evenement> getAllEvenements() {
        return evenementRepository.findAll();
    }

    // Méthode pour récupérer un événement par son ID
    public Optional<Evenement> getEvenementById(Long id) {
        return evenementRepository.findById(id);
    }

    // Méthode pour créer un événement
    public Evenement createEvenement(Evenement evenement) {
        return evenementRepository.save(evenement);
    }

    // Méthode pour mettre à jour un événement existant
    public Evenement updateEvenement(Long id, Evenement evenement) {
        if (evenementRepository.existsById(id)) {
            evenement.setId(id);
            return evenementRepository.save(evenement);
        }
        return null; // Ou gérer l'exception selon le cas
    }

    // Méthode pour supprimer un événement
    public void deleteEvenement(Long id) {
        evenementRepository.deleteById(id);
    }
}
