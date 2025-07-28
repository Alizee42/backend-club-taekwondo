package club.taekwondo.controller.jpa;

import club.taekwondo.dto.MembreDTO;
import club.taekwondo.dto.UtilisateurDTO;
import club.taekwondo.security.JwtUtil;
import club.taekwondo.service.jpa.MembreService;
import club.taekwondo.service.jpa.UtilisateurService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/membres")
@CrossOrigin(origins = "*")
public class MembreController {

    @Autowired
    private MembreService membreService;

    @Autowired
    private UtilisateurService utilisateurService;

    @Autowired
    private JwtUtil jwtUtil;

    // --- Récupérer tous les membres ---
    @GetMapping
    public ResponseEntity<List<MembreDTO>> getAllMembres() {
        return ResponseEntity.ok(membreService.getAllMembres());
    }

    // --- Récupérer un membre par ID ---
    @GetMapping("/{id}")
    public ResponseEntity<?> getMembreById(@PathVariable Long id) {
        return membreService.getMembreById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Membre non trouvé avec l'ID : " + id)));
    }

    // --- Créer un nouveau membre ---
    @PostMapping
    public ResponseEntity<?> createMembre(@RequestBody MembreDTO membreDTO) {
        try {
            MembreDTO nouveauMembre = membreService.createMembre(membreDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(nouveauMembre);
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Le numéro de licence est déjà utilisé."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de la création du membre."));
        }
    }

    // --- Mettre à jour un membre ---
    @PutMapping("/{id}")
    public ResponseEntity<?> updateMembre(@PathVariable Long id, @RequestBody MembreDTO membreDTO) {
        if (membreService.getMembreById(id).isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Membre non trouvé avec l'ID : " + id));
        }

        try {
            MembreDTO membreMisAJour = membreService.updateMembre(id, membreDTO);
            return ResponseEntity.ok(membreMisAJour);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de la mise à jour du membre."));
        }
    }

    // --- Supprimer un membre ---
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

    // --- Récupérer le membre ou utilisateur connecté ---
    @GetMapping("/me")
    public ResponseEntity<?> getMembreConnecte(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "En-tête Authorization manquant ou invalide."));
        }

        try {
            String jwt = token.replace("Bearer ", "");
            String email = jwtUtil.extractEmail(jwt);

            Optional<MembreDTO> membreOpt = membreService.getMembreByEmail(email);
            if (membreOpt.isPresent()) return ResponseEntity.ok(membreOpt.get());

            Optional<UtilisateurDTO> utilisateurOpt = utilisateurService.getUtilisateurByEmail(email);
            return utilisateurOpt.<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("message", "Aucun membre ou utilisateur trouvé avec l'email : " + email)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de la récupération des informations utilisateur."));
        }
    }
 // --- Récupérer les membres rattachés au parent connecté ---
    @GetMapping("/mes-enfants")
    public ResponseEntity<?> getMembresDuParentConnecte(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "En-tête Authorization manquant ou invalide."));
        }

        try {
            String jwt = token.replace("Bearer ", "");
            String email = jwtUtil.extractEmail(jwt);

            Optional<UtilisateurDTO> utilisateurOpt = utilisateurService.getUtilisateurByEmail(email);
            if (utilisateurOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Aucun utilisateur trouvé avec l'email : " + email));
            }

            Long parentId = utilisateurOpt.get().getId();
            List<MembreDTO> enfants = membreService.getMembresByUtilisateurId(parentId);
            return ResponseEntity.ok(enfants);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de la récupération des enfants du parent."));
        }
    }

}
