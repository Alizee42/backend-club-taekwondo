package club.taekwondo.controller.jpa;

import club.taekwondo.dto.LigneCommandeDTO;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.service.jpa.LigneCommandeService;
import club.taekwondo.service.jpa.UtilisateurService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/lignes-commande")
public class LigneCommandeController {

    private final LigneCommandeService ligneCommandeService;
    private final UtilisateurService utilisateurService;

    public LigneCommandeController(LigneCommandeService ligneCommandeService,
                                   UtilisateurService utilisateurService) {
        this.ligneCommandeService = ligneCommandeService;
        this.utilisateurService = utilisateurService;
    }

    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @GetMapping
    public ResponseEntity<List<LigneCommandeDTO>> getAllLignesCommande() {
        return ResponseEntity.ok(ligneCommandeService.getAllLignesCommande());
    }

    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @GetMapping("/club/{clubId}")
    public ResponseEntity<List<LigneCommandeDTO>> getLignesCommandeByClub(
            @PathVariable Long clubId, Authentication authentication) {
        // ADMIN limité à son propre club
        if (!hasRole(authentication, "SUPER_ADMIN")) {
            Utilisateur admin = utilisateurService.findByEmail(authentication.getName())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
            if (admin.getClub() == null || !admin.getClub().getId().equals(clubId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }
        return ResponseEntity.ok(ligneCommandeService.getLignesCommandeByClubId(clubId));
    }

    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<LigneCommandeDTO> getLigneCommandeById(@PathVariable Long id) {
        Optional<LigneCommandeDTO> ligne = ligneCommandeService.getLigneCommandeById(id);
        return ligne.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @PostMapping
    public ResponseEntity<LigneCommandeDTO> createLigneCommande(@RequestBody LigneCommandeDTO dto) {
        LigneCommandeDTO created = ligneCommandeService.createLigneCommande(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<LigneCommandeDTO> updateLigneCommande(
            @PathVariable Long id, @RequestBody LigneCommandeDTO dto) {
        try {
            return ResponseEntity.ok(ligneCommandeService.updateLigneCommande(id, dto));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLigneCommande(@PathVariable Long id) {
        ligneCommandeService.deleteLigneCommande(id);
        return ResponseEntity.noContent().build();
    }

    private boolean hasRole(Authentication auth, String role) {
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> ("ROLE_" + role).equals(a.getAuthority()));
    }
}
