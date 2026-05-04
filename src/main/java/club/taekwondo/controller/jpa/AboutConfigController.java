package club.taekwondo.controller.jpa;

import club.taekwondo.dto.AboutConfigDto;
import club.taekwondo.service.jpa.AboutConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/about-config")
public class AboutConfigController {

    private static final Logger log = LoggerFactory.getLogger(AboutConfigController.class);

    @Autowired
    private AboutConfigService aboutConfigService;

    @GetMapping
    public ResponseEntity<AboutConfigDto> get() {
        return ResponseEntity.ok(aboutConfigService.get());
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<AboutConfigDto> update(@RequestBody AboutConfigDto dto) {
        log.info("[AboutConfig] PUT /api/about-config");
        return ResponseEntity.ok(aboutConfigService.update(dto));
    }

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> uploadImage(@RequestParam("image") MultipartFile file) {
        log.info("[AboutConfig] POST /api/about-config/image - {}", file.getOriginalFilename());
        try {
            return ResponseEntity.ok(aboutConfigService.uploadImage(file));
        } catch (Exception e) {
            log.error("[AboutConfig] Erreur upload image", e);
            return ResponseEntity.internalServerError().body("Erreur lors de l'upload de l'image");
        }
    }
}
