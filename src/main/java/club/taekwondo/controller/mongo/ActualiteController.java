package club.taekwondo.controller.mongo;

import club.taekwondo.dto.ActualiteDTO;
import club.taekwondo.service.jpa.ActualiteService;
import club.taekwondo.service.common.FileUploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping(value = "/api/actualites", produces = MediaType.APPLICATION_JSON_VALUE) // ✅ garantit JSON

public class ActualiteController {
    /** Actualités filtrées par club */
    @GetMapping("/club/{clubId}")
    public ResponseEntity<List<ActualiteDTO>> getByClub(@PathVariable String clubId) {
    log.debug("[CTRL] GET /api/actualites/club/{}", clubId);
    List<ActualiteDTO> actualites = actualiteService.getByClubId(clubId);
    if (actualites == null) actualites = List.of();
    return ResponseEntity.ok(actualites);
    }

    private static final Logger log = LoggerFactory.getLogger(ActualiteController.class);

    private final ActualiteService actualiteService;
    private final FileUploadService fileUploadService;

    public ActualiteController(ActualiteService actualiteService, FileUploadService fileUploadService) {
        this.actualiteService = actualiteService;
        this.fileUploadService = fileUploadService;
    }

    @GetMapping
    public ResponseEntity<List<ActualiteDTO>> getAll() {
        log.debug("[CTRL] GET /api/actualites");
        try {
            List<ActualiteDTO> actualites = actualiteService.getAll();
            log.info("[CTRL] Actualités récupérées: {}", actualites.size());
            return ResponseEntity.ok(actualites);
        } catch (Exception e) {
            log.error("[CTRL] Erreur lors de la récupération des actualités", e);
            return ResponseEntity.status(500).body(List.of());
        }
    }

    @GetMapping("/featured")
    public ResponseEntity<List<ActualiteDTO>> getFeatured() {
        log.debug("[CTRL] GET /api/actualites/featured");
        return ResponseEntity.ok(actualiteService.getFeatured());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ActualiteDTO> getById(@PathVariable String id) {
        log.debug("[CTRL] GET /api/actualites/{}", id);
        return actualiteService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ActualiteDTO> create(@RequestBody ActualiteDTO actualiteDTO) {
        log.info("[CTRL] POST /api/actualites (JSON) - Payload reçu : {}", actualiteDTO);
        actualiteDTO.setDatePublication(LocalDateTime.now());
        ActualiteDTO saved = actualiteService.create(actualiteDTO);
        log.info("[CTRL] POST /api/actualites (JSON) - Actualité sauvegardée : {}", saved);
        return ResponseEntity.status(201).body(saved);
    }

    @PostMapping(value = "/with-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> createWithImage(
        @RequestParam String titre,
        @RequestParam String contenu,
        @RequestParam String typeActu,
        @RequestParam boolean isFeatured,
        @RequestParam String clubId,
        @RequestParam(value = "complement", required = false) String complement,
        @RequestParam(value = "image", required = false) MultipartFile imageFile
    ) {
    log.info("[CTRL] POST /api/actualites/with-image - titre={}, clubId={}, typeActu={}, isFeatured={}, complement={}, imageFileName={}",
        titre, clubId, typeActu, isFeatured, complement, (imageFile != null ? imageFile.getOriginalFilename() : "AUCUNE"));
    try {
        String imageUrl = (imageFile != null && !imageFile.isEmpty())
            ? fileUploadService.uploadFile(imageFile, "actualites")
            : null;

    ActualiteDTO actualiteDTO = new ActualiteDTO();
    actualiteDTO.setTitre(titre);
    actualiteDTO.setContenu(contenu);
    actualiteDTO.setTypeActu(typeActu);
    actualiteDTO.setFeatured(isFeatured);
    actualiteDTO.setImageUrl(imageUrl);
    actualiteDTO.setDatePublication(LocalDateTime.now());
    actualiteDTO.setClubId(clubId);
    actualiteDTO.setComplement(complement);

        ActualiteDTO saved = actualiteService.create(actualiteDTO);
        log.info("[CTRL] POST /api/actualites/with-image - Actualité sauvegardée : {}", saved);
        return ResponseEntity.status(201).body(saved);
    } catch (IOException e) {
            log.error("[CTRL] Erreur upload image", e);
            // ✅ réponse JSON au lieu de string brut
            return ResponseEntity.internalServerError()
                    .body(new ErrorResponse("Erreur lors de l'upload de l'image"));
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ActualiteDTO> update(@PathVariable String id, @RequestBody ActualiteDTO actualiteDTO) {
        log.debug("[CTRL] PUT /api/actualites/{} payload={}", id, actualiteDTO);
        ActualiteDTO updated = actualiteService.update(id, actualiteDTO);
        return (updated != null) ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}/featured")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> setFeatured(@PathVariable String id) {
        log.debug("[CTRL] PUT /api/actualites/{}/featured", id);
        try {
            actualiteService.setFeatured(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            log.error("[CTRL] Impossible de mettre en avant l’actualité {}", id, e);
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        log.debug("[CTRL] DELETE /api/actualites/{}", id);
        actualiteService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ✅ classe utilitaire pour renvoyer des erreurs en JSON
    private record ErrorResponse(String message) {}
}
