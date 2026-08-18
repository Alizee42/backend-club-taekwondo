package club.taekwondo.service.jpa;

import club.taekwondo.dto.ActualiteDTO;
import club.taekwondo.entity.jpa.Actualite;
import club.taekwondo.repository.jpa.ActualiteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ActualiteService {
    private static final Logger log = LoggerFactory.getLogger(ActualiteService.class);

    private final ActualiteRepository actualiteRepository;

    public ActualiteService(ActualiteRepository actualiteRepository) {
        this.actualiteRepository = actualiteRepository;
    }

    public List<ActualiteDTO> getByClubId(String clubId) {
        Long parsedClubId = parseClubId(clubId);
        log.info("Recuperation des actualites pour le club : {}", parsedClubId);
        return actualiteRepository.findByClubIdOrderByDatePublicationDesc(parsedClubId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<ActualiteDTO> getAll() {
        log.info("Recuperation de toutes les actualites.");
        return actualiteRepository.findAllByOrderByDatePublicationDesc()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<ActualiteDTO> getFeatured() {
        log.info("Recuperation des actualites a la une.");
        return actualiteRepository.findByFeaturedTrueOrderByDatePublicationDesc()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public Optional<ActualiteDTO> getById(String id) {
        return actualiteRepository.findById(parseId(id)).map(this::toDTO);
    }

    public ActualiteDTO create(ActualiteDTO actualiteDTO) {
        if (actualiteDTO.isFeatured()) {
            unsetAllFeatured(parseClubId(actualiteDTO.getClubId()));
        }

        Actualite entity = toEntity(actualiteDTO);
        Actualite saved = actualiteRepository.save(entity);
        return toDTO(saved);
    }

    public ActualiteDTO update(String id, ActualiteDTO actualiteDTO) {
        return actualiteRepository.findById(parseId(id))
                .map(existing -> {
                    Long clubId = actualiteDTO.getClubId() != null
                            ? parseClubId(actualiteDTO.getClubId())
                            : existing.getClubId();

                    if (actualiteDTO.isFeatured()) {
                        unsetAllFeaturedExcept(existing.getId(), clubId);
                    }

                    existing.setTitre(actualiteDTO.getTitre());
                    existing.setContenu(actualiteDTO.getContenu());
                    existing.setExtrait(actualiteDTO.getExtrait());
                    if (actualiteDTO.getDatePublication() != null) {
                        existing.setDatePublication(actualiteDTO.getDatePublication());
                    }
                    existing.setTypeActu(actualiteDTO.getTypeActu());
                    existing.setClubId(clubId);
                    existing.setFeatured(actualiteDTO.isFeatured());
                    existing.setImageUrl(actualiteDTO.getImageUrl());
                    existing.setComplement(actualiteDTO.getComplement());

                    return toDTO(actualiteRepository.save(existing));
                })
                .orElse(null);
    }

    public void delete(String id) {
        actualiteRepository.deleteById(parseId(id));
    }

    public void setFeatured(String id) {
        Actualite actualite = actualiteRepository.findById(parseId(id))
                .orElseThrow(() -> new RuntimeException("Actualite introuvable avec l'ID : " + id));
        unsetAllFeatured(actualite.getClubId());
        actualite.setFeatured(true);
        actualiteRepository.save(actualite);
    }

    public long countActualites() {
        return actualiteRepository.count();
    }

    private void unsetAllFeatured(Long clubId) {
        actualiteRepository.findByClubIdAndFeaturedTrue(clubId)
                .forEach(actualite -> {
                    actualite.setFeatured(false);
                    actualiteRepository.save(actualite);
                });
    }

    private void unsetAllFeaturedExcept(Long excludeId, Long clubId) {
        actualiteRepository.findByClubIdAndFeaturedTrue(clubId)
                .stream()
                .filter(actualite -> !actualite.getId().equals(excludeId))
                .forEach(actualite -> {
                    actualite.setFeatured(false);
                    actualiteRepository.save(actualite);
                });
    }

    private ActualiteDTO toDTO(Actualite actualite) {
        ActualiteDTO dto = new ActualiteDTO(
                String.valueOf(actualite.getId()),
                actualite.getTitre(),
                actualite.getContenu(),
                actualite.getDatePublication(),
                actualite.getTypeActu(),
                String.valueOf(actualite.getClubId()),
                actualite.isFeatured(),
                actualite.getImageUrl(),
                actualite.getComplement()
        );
        dto.setExtrait(actualite.getExtrait());
        return dto;
    }

    private Actualite toEntity(ActualiteDTO actualiteDTO) {
        Actualite actualite = new Actualite();
        if (actualiteDTO.getId() != null && !actualiteDTO.getId().isBlank()) {
            actualite.setId(parseId(actualiteDTO.getId()));
        }
        actualite.setTitre(actualiteDTO.getTitre());
        actualite.setContenu(actualiteDTO.getContenu());
        actualite.setExtrait(actualiteDTO.getExtrait());
        actualite.setDatePublication(actualiteDTO.getDatePublication());
        actualite.setTypeActu(actualiteDTO.getTypeActu());
        actualite.setClubId(parseClubId(actualiteDTO.getClubId()));
        actualite.setFeatured(actualiteDTO.isFeatured());
        actualite.setImageUrl(actualiteDTO.getImageUrl());
        actualite.setComplement(actualiteDTO.getComplement());
        return actualite;
    }

    private Long parseId(String id) {
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Identifiant d'actualite invalide: " + id, ex);
        }
    }

    private Long parseClubId(String clubId) {
        try {
            return Long.parseLong(clubId);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Identifiant de club invalide: " + clubId, ex);
        }
    }
}
