package club.taekwondo.service.jpa;

import club.taekwondo.entity.jpa.Cours;
import club.taekwondo.repository.jpa.CoursRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CoursService {

    @Autowired
    private CoursRepository coursRepository;

    // Méthode pour obtenir tous les cours
    public List<Cours> getAllCours() {
        return coursRepository.findAll();
    }

    // Méthode pour obtenir un cours par son ID
    public Optional<Cours> getCoursById(Long id) {
        return coursRepository.findById(id);
    }

    // Méthode pour créer un nouveau cours
    public Cours createCours(Cours cours) {
        return coursRepository.save(cours);
    }

    // Méthode pour mettre à jour un cours existant
    public Cours updateCours(Long id, Cours cours) {
        if (coursRepository.existsById(id)) {
            cours.setId(id);
            return coursRepository.save(cours);
        }
        return null; // ou gérer l'exception
    }

    // Méthode pour supprimer un cours
    public void deleteCours(Long id) {
        coursRepository.deleteById(id);
    }
}
