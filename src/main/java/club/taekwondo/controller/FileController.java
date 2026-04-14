package club.taekwondo.controller;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/uploads")
public class FileController {

    private final Path uploadPath = Paths.get("uploads").toAbsolutePath().normalize();

    // Accepte les sous-dossiers (ex: uploads/avis/image.jpg)
    @GetMapping("/{folder}/{filename:.+}")
    public ResponseEntity<Resource> getFile(
            @PathVariable String folder,
            @PathVariable String filename) {
        try {
            Path base = uploadPath.resolve(folder).normalize();
            Path file = base.resolve(filename).normalize();

            if (!base.startsWith(uploadPath) || !file.startsWith(base)) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new UrlResource(file.toUri());

            if (!Files.exists(file) || !Files.isRegularFile(file) || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            // Détecter le type MIME (image/jpeg, application/pdf, etc.)
            String contentType = java.nio.file.Files.probeContentType(file);

            return ResponseEntity.ok()
                    .header("Content-Type", contentType != null ? contentType : "application/octet-stream")
                    .body(resource);

        } catch (Exception e) {
            System.err.println("Erreur accès fichier: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

}
