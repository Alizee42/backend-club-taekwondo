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
        ActualiteDTO created = actualiteService.create(actualiteDTO);
        return ResponseEntity.status(201).body(created);
    }

    @PostMapping("/with-image")
    public ResponseEntity<ActualiteDTO> createWithImage(
            @RequestParam("titre") String titre,
            @RequestParam("contenu") String contenu,
            @RequestParam("typeActu") String typeActu,
            @RequestParam("isFeatured") boolean isFeatured,
            @RequestParam(value = "image", required = false) MultipartFile imageFile
    ) {
        try {
            String imageUrl = null;
            if (imageFile != null && !imageFile.isEmpty()) {
                imageUrl = fileUploadService.uploadFile(imageFile);
            }

            ActualiteDTO actualiteDTO = new ActualiteDTO();
            actualiteDTO.setTitre(titre);
            actualiteDTO.setContenu(contenu);
            actualiteDTO.setTypeActu(typeActu);
            actualiteDTO.setFeatured(isFeatured);
            actualiteDTO.setImageUrl(imageUrl);
            actualiteDTO.setDatePublication(LocalDateTime.now());

            ActualiteDTO created = actualiteService.create(actualiteDTO);
            return ResponseEntity.status(201).body(created);

        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ActualiteDTO> update(@PathVariable String id, @RequestBody ActualiteDTO actualiteDTO) {
        ActualiteDTO updated = actualiteService.update(id, actualiteDTO);
        return updated != null
                ? ResponseEntity.ok(updated)
                : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        actualiteService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
