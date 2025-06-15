package club.taekwondo.controller.jpa;

import club.taekwondo.dto.MembreDTO;
import club.taekwondo.dto.UtilisateurDTO;
import club.taekwondo.security.JwtUtil;
import club.taekwondo.service.jpa.MembreService;
import club.taekwondo.service.jpa.UtilisateurService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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

    // 🔹 Récupérer tous les membres
    @GetMapping
    public ResponseEntity<List<MembreDTO>> getAllMembres() {
        try {
            List<MembreDTO> membres = membreService.getAllMembres();
            return ResponseEntity.ok(membres);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // 🔹 Récupérer un membre par ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getMembreById(@PathVariable Long id) {
        try {
            Optional<MembreDTO> membre = membreService.getMembreById(id);
            return membre.<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("Membre non trouvé avec l'ID : " + id));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur lors de la récupération du membre.");
        }
    }

    // 🔹 Créer un nouveau membre
    @PostMapping
    public ResponseEntity<?> createMembre(@RequestBody MembreDTO membreDTO) {
        try {
            if (membreDTO.getEmail() == null) {
                return ResponseEntity.badRequest().body("L'email est requis.");
            }
            MembreDTO nouveauMembre = membreService.createMembre(membreDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(nouveauMembre);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur lors de la création du membre.");
        }
    }

    // 🔹 Mettre à jour un membre
    @PutMapping("/{id}")
    public ResponseEntity<?> updateMembre(@PathVariable Long id, @RequestBody MembreDTO membreDTO) {
        try {
            Optional<MembreDTO> membreExistant = membreService.getMembreById(id);
            if (membreExistant.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Membre non trouvé avec l'ID : " + id);
            }
            MembreDTO membreMisAJour = membreService.updateMembre(id, membreDTO);
            return ResponseEntity.ok(membreMisAJour);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur lors de la mise à jour du membre.");
        }
    }

    // 🔹 Supprimer un membre
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMembre(@PathVariable Long id) {
        try {
            Optional<MembreDTO> membreExistant = membreService.getMembreById(id);
            if (membreExistant.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Membre non trouvé avec l'ID : " + id);
            }
            membreService.deleteMembre(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur lors de la suppression du membre.");
        }
    }

    // 🔹 Récupérer le membre connecté
    @GetMapping("/me")
    public ResponseEntity<?> getMembreConnecte(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("En-tête Authorization manquant ou invalide.");
        }

        try {
            String jwt = token.replace("Bearer ", "");
            String email = jwtUtil.extractEmail(jwt);

            Optional<MembreDTO> membreDTO = membreService.getMembreByEmail(email);
            if (membreDTO.isPresent()) {
                return ResponseEntity.ok(membreDTO.get());
            }

            Optional<UtilisateurDTO> utilisateurDTO = utilisateurService.getUtilisateurByEmail(email);
            return utilisateurDTO.<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Utilisateur non trouvé avec l'email : " + email));


        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token invalide ou expiré.");
        }
    }
}
