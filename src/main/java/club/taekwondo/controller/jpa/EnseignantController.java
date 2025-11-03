package club.taekwondo.controller.jpa;

import club.taekwondo.dto.EnseignantDTO;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.service.jpa.EnseignantService;
import club.taekwondo.service.common.FileUploadService;
import club.taekwondo.service.jpa.UtilisateurService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/enseignants")
public class EnseignantController {

    private static final Logger log = LoggerFactory.getLogger(EnseignantController.class);

    private final EnseignantService enseignantService;
    private final UtilisateurService utilisateurService;
    private final FileUploadService fileUploadService;

    public EnseignantController(EnseignantService enseignantService, UtilisateurService utilisateurService, FileUploadService fileUploadService) {
        this.enseignantService = enseignantService;
        this.utilisateurService = utilisateurService;
        this.fileUploadService = fileUploadService;
    }

    // Public: liste des enseignants par club
    @GetMapping("/club/{clubId}")
    public ResponseEntity<List<EnseignantDTO>> getByClub(@PathVariable Long clubId) {
        List<EnseignantDTO> list = enseignantService.getByClub(clubId);
        return ResponseEntity.ok(list);
    }

    // Création: restreinte ADMIN (club) & SUPER_ADMIN
    @PostMapping
    public ResponseEntity<EnseignantDTO> create(@RequestBody EnseignantDTO dto, Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        String userRole = null;
        Long userClubId = null;
        if (email != null) {
            Optional<Utilisateur> opt = utilisateurService.getUtilisateurEntityByEmail(email);
            if (opt.isPresent()) {
                Utilisateur u = opt.get();
                userRole = u.getRole() != null ? u.getRole().name() : null;
                userClubId = u.getClub() != null ? u.getClub().getId() : null;
            }
        }
        EnseignantDTO created = enseignantService.create(dto, userRole, userClubId);
        return ResponseEntity.created(URI.create("/api/enseignants/" + created.getId())).body(created);
    }

    // Mise à jour
    @PutMapping("/{id}")
    public ResponseEntity<EnseignantDTO> update(@PathVariable Long id, @RequestBody EnseignantDTO dto, Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        String userRole = null;
        Long userClubId = null;
        if (email != null) {
            Optional<Utilisateur> opt = utilisateurService.getUtilisateurEntityByEmail(email);
            if (opt.isPresent()) {
                Utilisateur u = opt.get();
                userRole = u.getRole() != null ? u.getRole().name() : null;
                userClubId = u.getClub() != null ? u.getClub().getId() : null;
            }
        }
        Optional<EnseignantDTO> updated = enseignantService.update(id, dto, userRole, userClubId);
        return updated.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Suppression
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        String userRole = null;
        Long userClubId = null;
        if (email != null) {
            Optional<Utilisateur> opt = utilisateurService.getUtilisateurEntityByEmail(email);
            if (opt.isPresent()) {
                Utilisateur u = opt.get();
                userRole = u.getRole() != null ? u.getRole().name() : null;
                userClubId = u.getClub() != null ? u.getClub().getId() : null;
            }
        }
        boolean deleted = enseignantService.delete(id, userRole, userClubId);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    // Upload de photo d'enseignant (ADMIN/SUPER_ADMIN)
    @PostMapping("/upload-photo")
    public ResponseEntity<Map<String, String>> uploadPhoto(@RequestParam("photo") MultipartFile photo) {
        try {
            String path = fileUploadService.uploadFile(photo, "enseignants");
            Map<String, String> body = new HashMap<>();
            body.put("path", path); // ex: enseignants/1699999999_photo.jpg
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            log.warn("[Enseignants] Upload photo échoué: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}
