package club.taekwondo.service.mongo;

import club.taekwondo.dto.ActualiteDTO;
import club.taekwondo.entity.mongo.Actualite;
import club.taekwondo.repository.mongo.ActualiteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ActualiteService {
    /** Actualités par club */
    public List<ActualiteDTO> getByClubId(String clubId) {
        log.info("Récupération des actualités pour le club : {}", clubId);
        return actualiteRepository.findByClubId(clubId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private static final Logger log = LoggerFactory.getLogger(ActualiteService.class);
    private final ActualiteRepository actualiteRepository;

    public ActualiteService(ActualiteRepository actualiteRepository) {
        this.actualiteRepository = actualiteRepository;
    }

    public List<ActualiteDTO> getAll() {
        log.info("Récupération de toutes les actualités.");
        return actualiteRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<ActualiteDTO> getFeatured() {
        log.info("Récupération des actualités à la une.");
        return actualiteRepository.findByIsFeaturedTrue()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public Optional<ActualiteDTO> getById(String id) {
        log.info("Récupération de l'actualité avec ID : {}", id);
        return actualiteRepository.findById(id).map(this::toDTO);
    }

    public ActualiteDTO create(ActualiteDTO actualiteDTO) {
        log.info("Création d'une nouvelle actualité : {}", actualiteDTO.getTitre());

        if (actualiteDTO.isFeatured()) {
            log.info("Désactivation des autres actualités mises à la une.");
            unsetAllFeatured();
        }

        Actualite saved = actualiteRepository.save(toEntity(actualiteDTO));
        return toDTO(saved);
    }

    public ActualiteDTO update(String id, ActualiteDTO actualiteDTO) {
        log.info("Mise à jour de l'actualité avec ID : {}", id);

        Actualite existing = actualiteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Actualité introuvable avec l'ID : " + id));

        if (actualiteDTO.isFeatured()) {
            log.info("Désactivation des autres actualités mises à la une.");
            unsetAllFeaturedExcept(id);
        }

        existing.setTitre(actualiteDTO.getTitre());
        existing.setContenu(actualiteDTO.getContenu());
        existing.setDatePublication(actualiteDTO.getDatePublication());
        existing.setTypeActu(actualiteDTO.getTypeActu());
        existing.setFeatured(actualiteDTO.isFeatured());
        existing.setImageUrl(actualiteDTO.getImageUrl());

        return toDTO(actualiteRepository.save(existing));
    }

    public void delete(String id) {
        log.info("Suppression de l'actualité avec ID : {}", id);
        actualiteRepository.deleteById(id);
    }

    public void setFeatured(String id) {
        log.info("Mise à la une de l'actualité avec ID : {}", id);
        unsetAllFeatured();

        Actualite actualite = actualiteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Actualité introuvable avec l'ID : " + id));

        actualite.setFeatured(true);
        actualiteRepository.save(actualite);
    }

    /** Désactive toutes les actualités mises à la une */
    private void unsetAllFeatured() {
        actualiteRepository.findByIsFeaturedTrue().forEach(a -> {
            a.setFeatured(false);
            actualiteRepository.save(a);
        });
    }

    /** Compte le nombre d'actualités (pour debug) */
    public long countActualites() {
        return actualiteRepository.count();
    }

    /** Désactive toutes les actualités mises à la une sauf celle en cours */
    private void unsetAllFeaturedExcept(String excludeId) {
        actualiteRepository.findByIsFeaturedTrue().forEach(a -> {
            if (!a.getId().equals(excludeId)) {
                a.setFeatured(false);
                actualiteRepository.save(a);
            }
        });
    }

    private ActualiteDTO toDTO(Actualite actualite) {
    return new ActualiteDTO(
        actualite.getId(),
        actualite.getTitre(),
        actualite.getContenu(),
        actualite.getDatePublication(),
        actualite.getTypeActu(),
        actualite.getClubId(),
        actualite.isFeatured(),
        actualite.getImageUrl()
    );
    }

    private Actualite toEntity(ActualiteDTO actualiteDTO) {
        Actualite actualite = new Actualite();
    actualite.setId(actualiteDTO.getId());
    actualite.setTitre(actualiteDTO.getTitre());
    actualite.setContenu(actualiteDTO.getContenu());
    actualite.setDatePublication(actualiteDTO.getDatePublication());
    actualite.setTypeActu(actualiteDTO.getTypeActu());
    actualite.setClubId(actualiteDTO.getClubId());
    actualite.setFeatured(actualiteDTO.isFeatured());
    actualite.setImageUrl(actualiteDTO.getImageUrl());
    return actualite;
    }
}

