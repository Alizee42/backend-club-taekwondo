package club.taekwondo.service.jpa;

import club.taekwondo.dto.GalerieDTO;
import club.taekwondo.entity.jpa.Galerie;
import club.taekwondo.repository.jpa.GalerieRepository;
import club.taekwondo.service.common.FileUploadService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class GalerieService {
    private final GalerieRepository galerieRepository;
    private final FileUploadService fileUploadService;

    public GalerieService(GalerieRepository galerieRepository, FileUploadService fileUploadService) {
        this.galerieRepository = galerieRepository;
        this.fileUploadService = fileUploadService;
    }

    public List<GalerieDTO> getByClubId(Long clubId) {
        return galerieRepository.findByClubIdOrderByDatePublicationDesc(clubId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public GalerieDTO createMultipart(String titre, String description, Long clubId, MultipartFile image, String userRole, Long userClubId) {
        if ("ADMIN".equalsIgnoreCase(userRole) && !clubIdsMatch(userClubId, clubId)) {
            throw new SecurityException("Un admin ne peut publier que sur son propre club.");
        }

        String imageUrl = null;
        try {
            if (image != null && !image.isEmpty()) {
                imageUrl = fileUploadService.uploadFile(image, "galerie");
            }
        } catch (Exception e) {
            throw new RuntimeException("Erreur upload image", e);
        }

        Galerie galerie = new Galerie();
        galerie.setTitre(titre);
        galerie.setDescription(description);
        galerie.setClubId(clubId);
        galerie.setImageUrl(imageUrl);
        galerie.setDatePublication(LocalDateTime.now());
        return convertToDTO(galerieRepository.save(galerie));
    }

    public List<GalerieDTO> getAll() {
        return galerieRepository.findAllByOrderByDatePublicationDesc()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public Optional<GalerieDTO> getById(String id) {
        return galerieRepository.findById(parseId(id)).map(this::convertToDTO);
    }

    public GalerieDTO update(String id, GalerieDTO galerieDTO, String userRole, Long userClubId) {
        return galerieRepository.findById(parseId(id))
                .map(existing -> {
                    if ("ADMIN".equalsIgnoreCase(userRole) && !clubIdsMatch(userClubId, existing.getClubId())) {
                        throw new SecurityException("Un admin ne peut modifier que les galeries de son propre club.");
                    }
                    existing.setTitre(galerieDTO.getTitre());
                    existing.setImageUrl(galerieDTO.getImageUrl());
                    existing.setDescription(galerieDTO.getDescription());
                    if (galerieDTO.getDatePublication() != null) {
                        existing.setDatePublication(galerieDTO.getDatePublication());
                    }
                    return convertToDTO(galerieRepository.save(existing));
                })
                .orElse(null);
    }

    public void delete(String id) {
        galerieRepository.deleteById(parseId(id));
    }

    private GalerieDTO convertToDTO(Galerie galerie) {
        GalerieDTO dto = new GalerieDTO();
        dto.setId(String.valueOf(galerie.getId()));
        dto.setTitre(galerie.getTitre());
        dto.setImageUrl(galerie.getImageUrl());
        dto.setDescription(galerie.getDescription());
        dto.setDatePublication(galerie.getDatePublication());
        dto.setClubId(galerie.getClubId());
        return dto;
    }

    private boolean clubIdsMatch(Long firstClubId, Long secondClubId) {
        return firstClubId != null && firstClubId.equals(secondClubId);
    }

    private Long parseId(String id) {
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Identifiant de galerie invalide: " + id, ex);
        }
    }
}
