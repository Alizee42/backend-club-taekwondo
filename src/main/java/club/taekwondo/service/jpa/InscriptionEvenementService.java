package club.taekwondo.service.jpa;

import club.taekwondo.entity.jpa.InscriptionEvenement;
import club.taekwondo.repository.jpa.InscriptionEvenementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class InscriptionEvenementService {

    @Autowired
    private InscriptionEvenementRepository inscriptionEvenementRepository;

    // Méthode pour récupérer toutes les inscriptions aux événements
    public List<InscriptionEvenement> getAllInscriptions() {
        return inscriptionEvenementRepository.findAll();
    }

    // Méthode pour récupérer une inscription par son ID
    public Optional<InscriptionEvenement> getInscriptionById(Long id) {
        return inscriptionEvenementRepository.findById(id);
    }

    // Méthode pour inscrire un membre à un événement
    public InscriptionEvenement inscrireMembre(InscriptionEvenement inscriptionEvenement) {
        return inscriptionEvenementRepository.save(inscriptionEvenement);
    }

    // Méthode pour mettre à jour une inscription
    public InscriptionEvenement updateInscription(Long id, InscriptionEvenement inscriptionEvenement) {
        if (inscriptionEvenementRepository.existsById(id)) {
            inscriptionEvenement.setId(id);
            return inscriptionEvenementRepository.save(inscriptionEvenement);
        }
        return null; // Ou gérer l'exception selon le cas
    }

    // Méthode pour annuler une inscription
    public void annulerInscription(Long id) {
        inscriptionEvenementRepository.deleteById(id);
    }
}
