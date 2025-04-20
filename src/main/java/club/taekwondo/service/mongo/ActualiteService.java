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

    public Optional<Actualite> getById(String id) {
        return actualiteRepository.findById(id);
    }

    public Actualite create(Actualite actualite) {
        return actualiteRepository.save(actualite);
    }

    public Actualite update(String id, Actualite actualite) {
        return actualiteRepository.findById(id)
                .map(existing -> {
                    existing.setTitre(actualite.getTitre());
                    existing.setContenu(actualite.getContenu());
                    existing.setDatePublication(actualite.getDatePublication());
                    existing.setTypeActu(actualite.getTypeActu());
                    return actualiteRepository.save(existing);
                })
                .orElse(null);
    }

    public void delete(String id) {
        actualiteRepository.deleteById(id);
    }
}
