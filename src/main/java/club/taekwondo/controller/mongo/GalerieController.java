package club.taekwondo.controller.mongo;

import club.taekwondo.dto.GalerieDTO;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.service.jpa.UtilisateurService;
import club.taekwondo.service.mongo.GalerieService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/galeries")
public class GalerieController {

    private final GalerieService galerieService;
    private final UtilisateurService utilisateurService;

    public GalerieController(GalerieService galerieService, UtilisateurService utilisateurService) {
        this.galerieService = galerieService;
        this.utilisateurService = utilisateurService;
    }

    @GetMapping("/club/{clubId}")
    public List<GalerieDTO> getByClubId(@PathVariable Long clubId) {
        return galerieService.getByClubId(clubId);
    }

    @GetMapping
    public List<GalerieDTO> getAll() {
        return galerieService.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<GalerieDTO> getById(@PathVariable String id) {
        return galerieService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public GalerieDTO createMultipart(
            @RequestParam("titre") String titre,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("clubId") Long clubId,
            @RequestParam("image") MultipartFile image,
            Authentication authentication) {
        Utilisateur user = resolveUser(authentication);
        return galerieService.createMultipart(titre, description, clubId, image,
                user.getRole().name(), user.getClub() != null ? user.getClub().getId() : null);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<GalerieDTO> update(
            @PathVariable String id,
            @RequestBody GalerieDTO galerieDTO,
            Authentication authentication) {
        Utilisateur user = resolveUser(authentication);
        GalerieDTO updated = galerieService.update(id, galerieDTO,
                user.getRole().name(), user.getClub() != null ? user.getClub().getId() : null);
        return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        galerieService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private Utilisateur resolveUser(Authentication authentication) {
        return utilisateurService.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Utilisateur introuvable : " + authentication.getName()));
    }
}
