package club.taekwondo.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/uploads")
public class UploadsController {

    // réutilise ta propriété existante: upload.dir=uploads
    @Value("${upload.dir:uploads}")
    private String uploadDir;

    // /api/uploads/documents/<NOM DE FICHIER AVEC ESPACES/APOSTROPHES/ACCENTS>
    @GetMapping(value = "/documents/{filename:.+}", produces = MediaType.ALL_VALUE)
    public ResponseEntity<Resource> getDocument(@PathVariable String filename) {
        try {
            // base = {upload.dir}/documents
            Path base = Paths.get(uploadDir).toAbsolutePath().normalize().resolve("documents").normalize();
            Path file = base.resolve(filename).normalize();

            // anti-traversal + existence
            if (!file.startsWith(base) || !Files.exists(file)) {
                return ResponseEntity.notFound().build();
            }

            String mime = Files.probeContentType(file);
            if (mime == null) mime = MediaType.APPLICATION_OCTET_STREAM_VALUE;

            UrlResource resource = new UrlResource(file.toUri());

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(mime))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + file.getFileName().toString().replace("\"", "") + "\"")
                    .cacheControl(CacheControl.noCache())
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
