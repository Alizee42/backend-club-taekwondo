package club.taekwondo.controller.jpa;

import club.taekwondo.security.JwtUtil;
import club.taekwondo.dto.LoginDTO;
import club.taekwondo.dto.UtilisateurDTO;
import club.taekwondo.dto.UtilisateurPaiementDTO;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.enums.Role;
import club.taekwondo.service.jpa.UtilisateurService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/utilisateurs")
@CrossOrigin(origins = "*")
public class UtilisateurController {

    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    private UtilisateurService utilisateurService;

    public UtilisateurController(JwtUtil jwtUtil, PasswordEncoder passwordEncoder) {
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public ResponseEntity<List<UtilisateurDTO>> getAllUtilisateurs() {
        return ResponseEntity.ok(utilisateurService.getAllUtilisateurs());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUtilisateurById(@PathVariable Long id) {
        return utilisateurService.getUtilisateurById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Utilisateur non trouvé avec l'ID : " + id)));
    }

    @PostMapping
    public ResponseEntity<?> createUtilisateur(@RequestBody UtilisateurDTO utilisateurDTO) {
        try {
            Utilisateur nouvelUtilisateur = utilisateurService.createUtilisateur(utilisateurDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "Utilisateur créé avec succès.",
                    "id", nouvelUtilisateur.getId(),
                    "email", nouvelUtilisateur.getEmail(),
                    "roles", nouvelUtilisateur.getRoles()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Erreur lors de la création de l'utilisateur."));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUtilisateur(@PathVariable Long id, @RequestBody UtilisateurDTO utilisateurDTO) {
        if (utilisateurService.getUtilisateurById(id).isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Utilisateur non trouvé."));
        }

        utilisateurService.updateUtilisateurFromDTO(id, utilisateurDTO);
        return ResponseEntity.ok(Map.of("message", "Utilisateur mis à jour avec succès."));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUtilisateur(@PathVariable Long id) {
        utilisateurService.deleteUtilisateur(id);
        return ResponseEntity.ok(Map.of("message", "Utilisateur supprimé avec succès."));
    }

    @GetMapping("/paiements")
    public ResponseEntity<List<UtilisateurPaiementDTO>> getUtilisateursAvecPaiements() {
        return ResponseEntity.ok(utilisateurService.getAllWithPaiements());
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginDTO loginDTO) {
        Optional<UtilisateurDTO> utilisateurOpt = utilisateurService.login(loginDTO.getEmail(), loginDTO.getPassword());
        if (utilisateurOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Email ou mot de passe incorrect."));
        }

        UtilisateurDTO utilisateurDTO = utilisateurOpt.get();
        // Attention : adapter la génération du token pour plusieurs rôles si besoin
        String token = jwtUtil.generateToken(utilisateurDTO.getEmail(), utilisateurDTO.getRoles().toString());

        return ResponseEntity.ok(Map.of(
                "token", token,
                "roles", utilisateurDTO.getRoles(),
                "email", utilisateurDTO.getEmail(),
                "utilisateur", utilisateurDTO
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String token) {
        try {
            token = token.replace("Bearer ", "");
            String email = jwtUtil.extractEmail(token);

            UtilisateurDTO utilisateur = utilisateurService.getUtilisateurByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            return ResponseEntity.ok(utilisateur);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Token invalide ou utilisateur non trouvé."));
        }
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateCurrentUser(@RequestHeader("Authorization") String token, @RequestBody UtilisateurDTO updatedDTO) {
        try {
            token = token.replace("Bearer ", "");
            String email = jwtUtil.extractEmail(token);

            Utilisateur utilisateur = utilisateurService.getUtilisateurEntityByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            updatedDTO.setId(utilisateur.getId());
            utilisateurService.updateUtilisateurFromDTO(utilisateur.getId(), updatedDTO);

            return ResponseEntity.ok(Map.of("message", "Profil mis à jour avec succès."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Erreur lors de la mise à jour du profil."));
        }
    }

    @PutMapping("/me/password")
    public ResponseEntity<?> updatePassword(@RequestHeader("Authorization") String token, @RequestBody Map<String, String> passwordData) {
        try {
            token = token.replace("Bearer ", "");
            String email = jwtUtil.extractEmail(token);

            Utilisateur utilisateur = utilisateurService.getUtilisateurEntityByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            String newPassword = passwordData.get("password");
            if (newPassword == null || newPassword.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Le mot de passe est requis."));
            }

            UtilisateurDTO dto = utilisateurService.getUtilisateurById(utilisateur.getId()).get();
            dto.setPassword(passwordEncoder.encode(newPassword));

            utilisateurService.updateUtilisateurFromDTO(utilisateur.getId(), dto);

            return ResponseEntity.ok(Map.of("message", "Mot de passe mis à jour avec succès."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Erreur lors de la mise à jour du mot de passe."));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UtilisateurDTO utilisateurDTO) {
        try {
            if (utilisateurService.getUtilisateurByEmail(utilisateurDTO.getEmail()).isPresent()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Cet email est déjà utilisé."));
            }

            // Si aucun rôle n'est fourni, on met MEMBRE par défaut
            if (utilisateurDTO.getRoles() == null || utilisateurDTO.getRoles().isEmpty()) {
                utilisateurDTO.setRoles(Set.of(Role.MEMBRE));
            }

            Utilisateur nouvelUtilisateur = utilisateurService.createUtilisateur(utilisateurDTO);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "Utilisateur créé avec succès.",
                    "id", nouvelUtilisateur.getId(),
                    "email", nouvelUtilisateur.getEmail(),
                    "roles", nouvelUtilisateur.getRoles()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Erreur lors de l'inscription."));
        }
    }
}