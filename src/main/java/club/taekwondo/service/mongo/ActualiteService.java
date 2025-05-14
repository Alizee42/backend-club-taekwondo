package club.taekwondo.service.mongo;

import club.taekwondo.entity.mongo.Actualite;
import club.taekwondo.repository.mongo.ActualiteRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ActualiteService {

    private final ActualiteRepository actualiteRepository;

    public ActualiteService(ActualiteRepository actualiteRepository) {
        this.actualiteRepository = actualiteRepository;
    }

    public List<Actualite> getAll() {
        return actualiteRepository.findAll();
    }

    public List<Actualite> getFeatured() {
        return actualiteRepository.findByIsFeaturedTrue();
    }

    public Optional<Actualite> getById(String id) {
        return actualiteRepository.findById(id);
    }

    public Actualite create(Actualite actualite) {
        System.out.println("Enregistrement de l'actualité : " + actualite.isFeatured()); // Vérifiez la valeur ici
        return actualiteRepository.save(actualite);
    }

    public Actualite update(String id, Actualite updatedActualite) {
        return actualiteRepository.findById(id).map(actualite -> {
            actualite.setTitre(updatedActualite.getTitre());
            actualite.setContenu(updatedActualite.getContenu());
            actualite.setDatePublication(updatedActualite.getDatePublication());
            actualite.setTypeActu(updatedActualite.getTypeActu());
            actualite.setFeatured(updatedActualite.isFeatured());
            actualite.setImageUrl(updatedActualite.getImageUrl());
            return actualiteRepository.save(actualite);
        }).orElse(null);
    }

    public void delete(String id) {
        actualiteRepository.deleteById(id);
    }
}
