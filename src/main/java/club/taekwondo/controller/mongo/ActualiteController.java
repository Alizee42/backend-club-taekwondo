package club.taekwondo.controller.mongo;

import club.taekwondo.dto.ActualiteDTO;
import club.taekwondo.service.mongo.ActualiteService;
import club.taekwondo.service.common.FileUploadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/actualites")
public class ActualiteController {

    private final ActualiteService actualiteService;
    private final FileUploadService fileUploadService;

    public ActualiteController(ActualiteService actualiteService, FileUploadService fileUploadService) {
        this.actualiteService = actualiteService;
        this.fileUploadService = fileUploadService;
    }

    @GetMapping
    public List<ActualiteDTO> getAll() {
        return actualiteService.getAll();
    }

    @GetMapping("/featured")
    public List<ActualiteDTO> getFeatured() {
        return actualiteService.getFeatured();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ActualiteDTO> getById(@PathVariable String id) {
        return actualiteService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ActualiteDTO> create(@RequestBody ActualiteDTO actualiteDTO) {
        actualiteDTO.setDatePublication(LocalDateTime.now());
        return ResponseEntity.status(201).body(actualiteService.create(actualiteDTO));
    }

    @PostMapping("/with-image")
    public ResponseEntity<ActualiteDTO> createWithImage(
            @RequestParam String titre,
            @RequestParam String contenu,
            @RequestParam String typeActu,
            @RequestParam boolean isFeatured,
            @RequestParam(value = "image", required = false) MultipartFile imageFile
    ) {
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

            return ResponseEntity.status(201).body(actualiteService.create(actualiteDTO));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ActualiteDTO> update(@PathVariable String id, @RequestBody ActualiteDTO actualiteDTO) {
        ActualiteDTO updated = actualiteService.update(id, actualiteDTO);
        return (updated != null) ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}/featured")
    public ResponseEntity<Void> setFeatured(@PathVariable String id) {
        try {
            actualiteService.setFeatured(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        actualiteService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

