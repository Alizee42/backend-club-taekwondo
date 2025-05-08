package club.taekwondo.service.mongo;

import club.taekwondo.entity.mongo.Galerie;
import club.taekwondo.repository.mongo.GalerieRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class GalerieService {

    private final GalerieRepository galerieRepository;

    public GalerieService(GalerieRepository galerieRepository) {
        this.galerieRepository = galerieRepository;
    }

    public List<Galerie> getAll() {
        return galerieRepository.findAll();
    }

    public Optional<Galerie> getById(String id) {
        return galerieRepository.findById(id);
    }

    public Galerie create(Galerie galerie) {
        return galerieRepository.save(galerie);
    }

    public Galerie update(String id, Galerie galerie) {
        return galerieRepository.findById(id)
                .map(existing -> {
                    existing.setTitre(galerie.getTitre());
                    existing.setImageUrl(galerie.getImageUrl());
                    existing.setDescription(galerie.getDescription());
                    existing.setDatePublication(galerie.getDatePublication());
                    return galerieRepository.save(existing);
                })
                .orElse(null);
    }

    public void delete(String id) {
        galerieRepository.deleteById(id);
    }
}