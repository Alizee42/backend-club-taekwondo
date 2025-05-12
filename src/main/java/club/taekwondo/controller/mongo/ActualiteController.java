package club.taekwondo.controller.mongo;

import club.taekwondo.entity.mongo.Actualite;
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
    public List<Actualite> getAll() {
        return actualiteService.getAll();
    }

    @GetMapping("/featured")
    public List<Actualite> getFeatured() {
        return actualiteService.getFeatured();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Actualite> getById(@PathVariable String id) {
        return actualiteService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Actualite create(@RequestBody Actualite actualite) {
        return actualiteService.create(actualite);
    }

    @PostMapping("/with-image")
    public ResponseEntity<Actualite> createWithImage(
            @RequestParam("titre") String titre,
            @RequestParam("contenu") String contenu,
            @RequestParam("typeActu") String typeActu,
            @RequestParam("isFeatured") boolean isFeatured,
            @RequestParam(value = "image", required = false) MultipartFile imageFile
    ) {
        try {
            String imageUrl = null;

            // Upload de l'image si fournie
            if (imageFile != null && !imageFile.isEmpty()) {
                imageUrl = fileUploadService.uploadFile(imageFile);
            }

            // Création de l’actualité
            Actualite actualite = new Actualite();
            actualite.setTitre(titre);
            actualite.setContenu(contenu);
            actualite.setTypeActu(typeActu);
            actualite.setFeatured(isFeatured);
            actualite.setDatePublication(LocalDateTime.now());
            actualite.setImageUrl(imageUrl);

            return ResponseEntity.ok(actualiteService.create(actualite));

        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Actualite> update(@PathVariable String id, @RequestBody Actualite actualite) {
        Actualite updated = actualiteService.update(id, actualite);
        if (updated != null) {
            return ResponseEntity.ok(updated);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        actualiteService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
