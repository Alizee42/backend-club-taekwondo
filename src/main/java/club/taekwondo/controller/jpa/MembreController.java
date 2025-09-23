package club.taekwondo.controller.jpa;

import club.taekwondo.dto.MembreDTO;
import club.taekwondo.service.jpa.MembreService;
import club.taekwondo.service.jpa.UtilisateurService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/membres")
@CrossOrigin(origins = "*")
public class MembreController {

    private final MembreService membreService;

    public MembreController(MembreService membreService, UtilisateurService utilisateurService) {
        this.membreService = membreService;
    }

    // ------------------ READ ------------------

    /** Polyvalent :
     * - Si l'utilisateur connecté est PARENT => retourne uniquement SES enfants (ignore les query params).
     * - Sinon, si ?parentId=... est fourni => enfants de ce parent (usage admin).
     * - Sinon => tous les membres (ex: admin).
     */
    @GetMapping
    public ResponseEntity<?> getMembres(
            @RequestParam(value = "parentId", required = false) Long parentId,
            Authentication authentication
    ) {
        boolean isParent = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_PARENT") || a.getAuthority().equals("PARENT"));

        if (isParent) {
            String email = authentication.getName();
            List<MembreDTO> mine = membreService.getMembresByParentEmail(email);
            return mine.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(mine);
        }

        if (parentId != null) {
            List<MembreDTO> children = membreService.getMembresByUtilisateurId(parentId);
            return children.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(children);
        }

        List<MembreDTO> all = membreService.getAllMembres();
        return all.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(all);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getMembreById(@PathVariable Long id) {
        return membreService.getMembreById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Membre non trouvé avec l'ID : " + id)));
    }

    /** Parent : récupère uniquement SES enfants (via le JWT) */
    @GetMapping("/mes-enfants")
    public ResponseEntity<?> getMembresDuParentConnecte(Authentication authentication) {
        String email = authentication.getName();
        List<MembreDTO> enfants = membreService.getMembresByParentEmail(email);
        return enfants.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(enfants);
    }

    /** 1-1 éventuel : membre rattaché à un utilisateur adulte, renvoie DTO */
    @GetMapping("/by-user/{utilisateurId}")
    public ResponseEntity<?> getMembreByUtilisateurId(@PathVariable Long utilisateurId) {
        return membreService.getMembreEntityByIdUtilisateur(utilisateurId)
                .<ResponseEntity<?>>map(m -> ResponseEntity.ok(membreService.toMembreDTO(m)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Aucun membre trouvé pour cet utilisateur.")));
    }

    /** Admin: utile pour sélecteur enfants par parent en BO */
    @GetMapping("/by-parent/{parentId}")
    public ResponseEntity<List<Map<String, Object>>> getByParent(@PathVariable Long parentId) {
        List<MembreDTO> enfants = membreService.getMembresByUtilisateurId(parentId);
        List<Map<String, Object>> out = new ArrayList<>();
        for (MembreDTO m : enfants) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", m.getId());
            item.put("prenom", m.getPrenom());
            item.put("nom", m.getNom());
            out.add(item);
        }
        return out.isEmpty() ? ResponseEntity.status(HttpStatus.NO_CONTENT).build() : ResponseEntity.ok(out);
    }

    // ------------------ CREATE / UPDATE / DELETE ------------------

    @PostMapping
    public ResponseEntity<?> createMembre(@RequestBody MembreDTO membreDTO) {
        try {
            MembreDTO nouveauMembre = membreService.createMembre(membreDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(nouveauMembre);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Le numéro de licence est déjà utilisé."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de la création du membre."));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateMembre(@PathVariable Long id, @RequestBody MembreDTO membreDTO) {
        if (membreService.getMembreById(id).isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Membre non trouvé avec l'ID : " + id));
        }
        try {
            MembreDTO membreMisAJour = membreService.updateMembre(id, membreDTO);
            return ResponseEntity.ok(membreMisAJour);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de la mise à jour du membre."));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMembre(@PathVariable Long id) {
        if (membreService.getMembreById(id).isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Membre non trouvé avec l'ID : " + id));
        }
        try {
            membreService.deleteMembre(id);
            return ResponseEntity.ok(Map.of("message", "Membre supprimé avec succès."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de la suppression du membre."));
        }
    }
    @GetMapping("/me")
    public ResponseEntity<?> getMembreConnecte(Authentication authentication) {
        String email = authentication.getName();

        // 🔹 On récupère le rôle via les authorities
        boolean isParent = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_PARENT") || a.getAuthority().equals("PARENT"));
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ADMIN"));

        // 🚫 Cas ADMIN : pas de membre associé → on renvoie 204 No Content
        if (isAdmin) {
            return ResponseEntity.noContent().build();
        }

        // 🔹 Cas ADULTE : membre lié à l’utilisateur
        Optional<MembreDTO> membre = membreService.getMembreByUtilisateurEmail(email);
        if (membre.isPresent()) {
            return ResponseEntity.ok(membre.get());
        }

        // 🔹 Cas PARENT : retourne null (car parent n’est pas un membre pratiquant)
        if (isParent) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Aucun membre trouvé pour l’utilisateur connecté."));
    }

}

