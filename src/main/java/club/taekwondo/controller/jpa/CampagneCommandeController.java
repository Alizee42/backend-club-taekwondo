package club.taekwondo.controller.jpa;

import club.taekwondo.dto.CampagneCommandeDTO;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.service.jpa.CampagneCommandeService;
import club.taekwondo.service.jpa.UtilisateurService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/campagnes-commande")
public class CampagneCommandeController {

    private final CampagneCommandeService campagneService;
    private final UtilisateurService utilisateurService;

    public CampagneCommandeController(CampagneCommandeService campagneService,
                                      UtilisateurService utilisateurService) {
        this.campagneService = campagneService;
        this.utilisateurService = utilisateurService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<CampagneCommandeDTO>> getAll(Authentication authentication) {
        Utilisateur caller = requireCaller(authentication);
        Long clubId = getClubId(caller);
        if (clubId == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(campagneService.getParClub(clubId));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('MEMBRE','PARENT','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<CampagneCommandeDTO> getActive(Authentication authentication) {
        Utilisateur caller = requireCaller(authentication);
        Long clubId = getClubId(caller);
        if (clubId == null) return ResponseEntity.noContent().build();
        return campagneService.getActive(clubId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<CampagneCommandeDTO> creer(@RequestBody CampagneCommandeDTO dto,
                                                     Authentication authentication) {
        Utilisateur caller = requireCaller(authentication);
        Long clubId = getClubId(caller);
        if (clubId == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        if (dto.getClubId() == null) dto.setClubId(clubId);
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(campagneService.creer(dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<CampagneCommandeDTO> modifier(@PathVariable Long id,
                                                        @RequestBody CampagneCommandeDTO dto,
                                                        Authentication authentication) {
        try {
            return ResponseEntity.ok(campagneService.modifier(id, dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        campagneService.supprimer(id);
        return ResponseEntity.noContent().build();
    }

    private Utilisateur requireCaller(Authentication authentication) {
        return utilisateurService.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Utilisateur authentifie introuvable"));
    }

    private Long getClubId(Utilisateur utilisateur) {
        return utilisateur != null && utilisateur.getClub() != null ? utilisateur.getClub().getId() : null;
    }
}
