package club.taekwondo.controller.jpa;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.service.jpa.UtilisateurService;
import club.taekwondo.dto.MembreDTO;
import club.taekwondo.service.jpa.MembreService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/membres")
public class MembreController {

    private final MembreService membreService;
    private final UtilisateurService utilisateurService;

    public MembreController(MembreService membreService, UtilisateurService utilisateurService) {
        this.membreService = membreService;
        this.utilisateurService = utilisateurService;
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
                @RequestParam(value = "clubId", required = false) Long clubId,
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


            if (clubId != null) {
                // Pour tous les rôles : si clubId fourni, on filtre par club
                // (sauf pour les admins d'autres clubs, on garde la vérification d'accès)
                boolean isSuperAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN") || a.getAuthority().equals("SUPER_ADMIN"));
                if (!isSuperAdmin) {
                    Utilisateur utilisateur = utilisateurService.findByEmail(authentication.getName()).orElse(null);
                    boolean isAdmin = authentication.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ADMIN"));
                    if (!isAdmin && (utilisateur == null || utilisateur.getClub() == null || !utilisateur.getClub().getId().equals(clubId))) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("message", "Accès refusé à ce club."));
                    }
                }
                List<MembreDTO> membres = membreService.getMembresByClubId(clubId);
                return membres.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(membres);
            }

            // MEMBRE : accès refusé à la liste globale
            boolean isMembre = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_MEMBRE") || a.getAuthority().equals("MEMBRE"));
            if (isMembre) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Accès refusé : un membre ne peut pas lister tous les membres."));
            }

            List<MembreDTO> all = membreService.getAllMembres();
            return all.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(all);
        }

    @GetMapping("/{id}")
    public ResponseEntity<?> getMembreById(@PathVariable Long id, Authentication authentication) {
        boolean isAdminOrSuper = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ADMIN")
                        || a.getAuthority().equals("ROLE_SUPER_ADMIN") || a.getAuthority().equals("SUPER_ADMIN"));
        if (!isAdminOrSuper) {
            String email = authentication.getName();
            boolean isParent = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_PARENT") || a.getAuthority().equals("PARENT"));
            if (isParent) {
                boolean ownsChild = membreService.getMembresByParentEmail(email).stream()
                        .anyMatch(m -> id.equals(m.getId()));
                if (!ownsChild) return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Accès refusé : ce membre ne fait pas partie de votre famille."));
            } else {
                // MEMBRE : seulement sa propre fiche
                return membreService.getMembreByUtilisateurEmail(email)
                        .filter(m -> id.equals(m.getId()))
                        .<ResponseEntity<?>>map(ResponseEntity::ok)
                        .orElseGet(() -> ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(Map.of("message", "Accès refusé.")));
            }
        }
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

    /** Alias pour compatibilité frontend : membre rattaché à un utilisateur adulte */
    @GetMapping("/by-utilisateur/{utilisateurId}")
    public ResponseEntity<?> getMembreByUtilisateurIdAlias(@PathVariable Long utilisateurId) {
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

