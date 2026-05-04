package club.taekwondo.controller.jpa;

import club.taekwondo.dto.HeroConfigDto;
import club.taekwondo.service.jpa.HeroConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/hero-config")
public class HeroConfigController {

    private static final Logger log = LoggerFactory.getLogger(HeroConfigController.class);

    @Autowired
    private HeroConfigService heroConfigService;

    @GetMapping
    public ResponseEntity<HeroConfigDto> get() {
        return ResponseEntity.ok(heroConfigService.get());
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<HeroConfigDto> update(@RequestBody HeroConfigDto dto) {
        log.info("[HeroConfig] PUT /api/hero-config");
        return ResponseEntity.ok(heroConfigService.update(dto));
    }

    @PostMapping(value = "/video", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> uploadVideo(@RequestParam("video") MultipartFile file) {
        log.info("[HeroConfig] POST /api/hero-config/video - {}", file.getOriginalFilename());
        try {
            return ResponseEntity.ok(heroConfigService.uploadVideo(file));
        } catch (Exception e) {
            log.error("[HeroConfig] Erreur upload vidéo", e);
            return ResponseEntity.internalServerError().body("Erreur lors de l'upload de la vidéo");
        }
    }
}
