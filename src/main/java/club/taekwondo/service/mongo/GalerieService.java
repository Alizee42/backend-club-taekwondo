

    package club.taekwondo.service.mongo;

    import club.taekwondo.dto.GalerieDTO;
    import club.taekwondo.entity.mongo.Galerie;
    import club.taekwondo.repository.mongo.GalerieRepository;
import club.taekwondo.service.common.FileUploadService;

import org.springframework.stereotype.Service;
    import org.springframework.web.multipart.MultipartFile;
    import org.springframework.beans.factory.annotation.Autowired;
    import java.time.LocalDateTime;
    import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

    @Service
    public class GalerieService {
        private final GalerieRepository galerieRepository;
        private final FileUploadService fileUploadService;

        @Autowired
        public GalerieService(GalerieRepository galerieRepository, FileUploadService fileUploadService) {
            this.galerieRepository = galerieRepository;
            this.fileUploadService = fileUploadService;
        }

        // 🔒 Récupérer toutes les galeries d'un club
        public List<GalerieDTO> getByClubId(Long clubId) {
            return galerieRepository.findByClubId(clubId)
                    .stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        }

        /**
         * Crée une galerie avec upload d'image via FileUploadService
         */
        public GalerieDTO createMultipart(String titre, String description, Long clubId, MultipartFile image, String userRole, Long userClubId) {
            if ("ADMIN".equalsIgnoreCase(userRole) && !userClubId.equals(clubId)) {
                throw new SecurityException("Un admin ne peut publier que sur son propre club.");
            }
            String imageUrl = null;
            try {
                if (image != null && !image.isEmpty()) {
                    // Utilise FileUploadService pour uploader dans le dossier "galerie"
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

        // ...existing code...

    public List<GalerieDTO> getAll() {
        return galerieRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public Optional<GalerieDTO> getById(String id) {
        return galerieRepository.findById(id).map(this::convertToDTO);
    }

    public GalerieDTO update(String id, GalerieDTO galerieDTO, String userRole, Long userClubId) {
        return galerieRepository.findById(id).map(existing -> {
            if ("ADMIN".equalsIgnoreCase(userRole) && !userClubId.equals(existing.getClubId())) {
                throw new SecurityException("Un admin ne peut modifier que les galeries de son propre club.");
            }
            existing.setTitre(galerieDTO.getTitre());
            existing.setImageUrl(galerieDTO.getImageUrl());
            existing.setDescription(galerieDTO.getDescription());
            existing.setDatePublication(galerieDTO.getDatePublication());
            // On ne change pas le clubId ici pour éviter la fraude
            return convertToDTO(galerieRepository.save(existing));
        }).orElse(null);
    }

    public void delete(String id) {
        galerieRepository.deleteById(id);
    }

    /**
     * Convertit une entité Galerie en DTO
     */
    private GalerieDTO convertToDTO(Galerie galerie) {
        GalerieDTO dto = new GalerieDTO();
        dto.setId(galerie.getId());
        dto.setTitre(galerie.getTitre());
        dto.setImageUrl(galerie.getImageUrl());
        dto.setDescription(galerie.getDescription());
        dto.setDatePublication(galerie.getDatePublication());
        dto.setClubId(galerie.getClubId());
        return dto;
    }

    }