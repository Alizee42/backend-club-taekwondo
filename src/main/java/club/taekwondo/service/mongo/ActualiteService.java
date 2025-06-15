package club.taekwondo.service.mongo;

import club.taekwondo.dto.ActualiteDTO;
import club.taekwondo.entity.mongo.Actualite;
import club.taekwondo.repository.mongo.ActualiteRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ActualiteService {

    private final ActualiteRepository actualiteRepository;

    public ActualiteService(ActualiteRepository actualiteRepository) {
        this.actualiteRepository = actualiteRepository;
    }

    public List<ActualiteDTO> getAll() {
        return actualiteRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<ActualiteDTO> getFeatured() {
        return actualiteRepository.findByIsFeaturedTrue().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public Optional<ActualiteDTO> getById(String id) {
        return actualiteRepository.findById(id).map(this::toDTO);
    }

    public ActualiteDTO create(ActualiteDTO actualiteDTO) {
        Actualite entity = toEntity(actualiteDTO);
        if (actualiteDTO.isFeatured()) {
            actualiteRepository.findByIsFeaturedTrue().forEach(a -> {
                a.setFeatured(false);
                actualiteRepository.save(a);
            });
        }
        return toDTO(actualiteRepository.save(entity));
    }

    public ActualiteDTO update(String id, ActualiteDTO actualiteDTO) {
        return actualiteRepository.findById(id).map(existing -> {
            if (actualiteDTO.isFeatured()) {
                actualiteRepository.findByIsFeaturedTrue().forEach(a -> {
                    if (!a.getId().equals(id)) {
                        a.setFeatured(false);
                        actualiteRepository.save(a);
                    }
                });
            }

            existing.setTitre(actualiteDTO.getTitre());
            existing.setContenu(actualiteDTO.getContenu());
            existing.setDatePublication(actualiteDTO.getDatePublication());
            existing.setTypeActu(actualiteDTO.getTypeActu());
            existing.setFeatured(actualiteDTO.isFeatured());
            existing.setImageUrl(actualiteDTO.getImageUrl());

            return toDTO(actualiteRepository.save(existing));
        }).orElse(null);
    }

    public void delete(String id) {
        actualiteRepository.deleteById(id);
    }

    // 🔁 Conversion: Entity ➜ DTO
    private ActualiteDTO toDTO(Actualite actualite) {
        ActualiteDTO actualiteDTO = new ActualiteDTO();
        actualiteDTO.setId(actualite.getId());
        actualiteDTO.setTitre(actualite.getTitre());
        actualiteDTO.setContenu(actualite.getContenu());
        actualiteDTO.setDatePublication(actualite.getDatePublication());
        actualiteDTO.setTypeActu(actualite.getTypeActu());
        actualiteDTO.setFeatured(actualite.isFeatured());
        actualiteDTO.setImageUrl(actualite.getImageUrl());
        return actualiteDTO;
    }

    // 🔁 Conversion: DTO ➜ Entity
    private Actualite toEntity(ActualiteDTO actualiteDTO) {
        Actualite actualite = new Actualite();
        actualite.setId(actualiteDTO.getId());
        actualite.setTitre(actualiteDTO.getTitre());
        actualite.setContenu(actualiteDTO.getContenu());
        actualite.setDatePublication(actualiteDTO.getDatePublication());
        actualite.setTypeActu(actualiteDTO.getTypeActu());
        actualite.setFeatured(actualiteDTO.isFeatured());
        actualite.setImageUrl(actualiteDTO.getImageUrl());
        return actualite;
    }
}

