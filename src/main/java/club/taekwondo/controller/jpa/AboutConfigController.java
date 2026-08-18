package club.taekwondo.controller.jpa;

import club.taekwondo.dto.AboutConfigDto;
import club.taekwondo.service.jpa.AboutConfigService;
import club.taekwondo.service.jpa.UtilisateurService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/about-config")
public class AboutConfigController {

    private static final Logger log = LoggerFactory.getLogger(AboutConfigController.class);

    @Autowired
    private AboutConfigService aboutConfigService;

    @Autowired
    private UtilisateurService utilisateurService;

    @GetMapping
    public ResponseEntity<AboutConfigDto> get(@RequestParam(required = false) Long clubId) {
        if (clubId == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(aboutConfigService.get(clubId));
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<AboutConfigDto> update(@RequestBody AboutConfigDto dto, Authentication authentication) {
        Long clubId = resolveClubId(dto.getClubId(), authentication);
        if (clubId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        log.info("[AboutConfig] PUT /api/about-config club={}", clubId);
        return ResponseEntity.ok(aboutConfigService.update(clubId, dto));
    }

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> uploadImage(@RequestParam("image") MultipartFile file,
                                          @RequestParam(required = false) Long clubId,
                                          Authentication authentication) {
        Long resolvedClubId = resolveClubId(clubId, authentication);
        if (resolvedClubId == null) {
            return ResponseEntity.badRequest().body("clubId manquant");
        }
        log.info("[AboutConfig] POST /api/about-config/image club={} - {}", resolvedClubId, file.getOriginalFilename());
        try {
            return ResponseEntity.ok(aboutConfigService.uploadImage(resolvedClubId, file));
        } catch (Exception e) {
            log.error("[AboutConfig] Erreur upload image", e);
            return ResponseEntity.internalServerError().body("Erreur lors de l'upload de l'image");
        }
    }

    // ADMIN : club toujours dérivé de son propre compte (ignore tout clubId reçu du client).
    // SUPER_ADMIN : n'a pas de club propre, doit fournir explicitement clubId.
    private Long resolveClubId(Long requestedClubId, Authentication authentication) {
        return utilisateurService.findByEmail(authentication.getName())
                .map(caller -> {
                    if (caller.getClub() != null) {
                        return caller.getClub().getId();
                    }
                    return requestedClubId;
                })
                .orElse(requestedClubId);
    }
}
