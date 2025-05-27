package club.taekwondo.controller.jpa;

import club.taekwondo.entity.jpa.Membre;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.service.jpa.MembreService;
import club.taekwondo.service.jpa.UtilisateurService;
import club.taekwondo.security.JwtUtil;
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

    // Récupérer tous les membres
    @GetMapping
    public ResponseEntity<List<Membre>> getAllMembres() {
        try {
            List<Membre> membres = membreService.getAllMembres();
            return ResponseEntity.ok(membres);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // Récupérer un membre par ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getMembreById(@PathVariable Long id) {
        try {
            Optional<Membre> membre = membreService.getMembreById(id);
            if (membre.isPresent()) {
                return ResponseEntity.ok(membre.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Membre non trouvé avec l'ID : " + id);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur lors de la récupération du membre.");
        }
    }

    // Créer un nouveau membre
    @PostMapping
    public ResponseEntity<?> createMembre(@RequestBody Membre membre) {
        try {
            if (membre.getEmail() == null || membre.getPassword() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("L'email et le mot de passe sont obligatoires.");
            }
            Membre nouveauMembre = membreService.createMembre(membre);
            return ResponseEntity.status(HttpStatus.CREATED).body(nouveauMembre);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur lors de la création du membre.");
        }
    }

    // Mettre à jour un membre
    @PutMapping("/{id}")
    public ResponseEntity<?> updateMembre(@PathVariable Long id, @RequestBody Membre membre) {
        try {
            Optional<Membre> membreExistant = membreService.getMembreById(id);
            if (membreExistant.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Membre non trouvé avec l'ID : " + id);
            }
            Membre membreMisAJour = membreService.updateMembre(id, membre);
            return ResponseEntity.ok(membreMisAJour);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur lors de la mise à jour du membre.");
        }
    }

    // Supprimer un membre
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMembre(@PathVariable Long id) {
        try {
            Optional<Membre> membreExistant = membreService.getMembreById(id);
            if (membreExistant.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Membre non trouvé avec l'ID : " + id);
            }
            membreService.deleteMembre(id);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur lors de la suppression du membre.");
        }
    }

    // Récupérer le membre connecté
    @GetMapping("/me")
    public ResponseEntity<?> getMembreConnecte(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("En-tête Authorization manquant ou invalide.");
        }

        try {
            String jwt = token.replace("Bearer ", "");
            String email = jwtUtil.extractEmail(jwt);

            // Vérifiez si l'utilisateur est un membre
            Optional<Membre> membre = membreService.getMembreByEmail(email);
            if (membre.isPresent()) {
                return ResponseEntity.ok(membre.get());
            }

            // Si ce n'est pas un membre, retournez l'utilisateur générique
            Optional<Utilisateur> utilisateur = utilisateurService.getUtilisateurByEmail(email);
            if (utilisateur.isPresent()) {
                return ResponseEntity.ok(utilisateur.get());
            }

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Utilisateur non trouvé avec l'email : " + email);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token invalide ou expiré.");
        }
    }
}