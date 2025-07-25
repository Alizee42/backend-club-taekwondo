package club.taekwondo.controller.jpa;

import club.taekwondo.dto.LoginDTO;
import club.taekwondo.dto.UtilisateurDTO;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.service.jpa.UtilisateurService;
import club.taekwondo.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/utilisateurs")
@CrossOrigin(origins = "*")
public class UtilisateurController {

    private final JwtUtil jwtUtil;
    private final UtilisateurService utilisateurService;

    @Autowired
    public UtilisateurController(JwtUtil jwtUtil, UtilisateurService utilisateurService) {
        this.jwtUtil = jwtUtil;
        this.utilisateurService = utilisateurService;
    }

    // ✅ NOUVEAU ENDPOINT : /api/utilisateurs
    @GetMapping("")
    public ResponseEntity<List<UtilisateurDTO>> getAllUtilisateurs() {
        List<UtilisateurDTO> utilisateurs = utilisateurService.getAllUtilisateurs();
        return ResponseEntity.ok(utilisateurs);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UtilisateurDTO utilisateurDTO) {
        try {
            if (utilisateurService.getUtilisateurByEmail(utilisateurDTO.getEmail()).isPresent()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Cet email est déjà utilisé."));
            }

            if (utilisateurDTO.getRole() == null || utilisateurDTO.getRole().isBlank()) {
                utilisateurDTO.setRole("MEMBRE");
            }

            Utilisateur nouvelUtilisateur = utilisateurService.createUtilisateur(utilisateurDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "Utilisateur créé avec succès.",
                    "id", nouvelUtilisateur.getId(),
                    "email", nouvelUtilisateur.getEmail(),
                    "role", nouvelUtilisateur.getRole()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de l'inscription."));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginDTO loginDTO) {
        try {
            Optional<UtilisateurDTO> utilisateurOpt = utilisateurService.login(loginDTO.getEmail(), loginDTO.getPassword());
            if (utilisateurOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Email ou mot de passe incorrect."));
            }

            UtilisateurDTO utilisateurDTO = utilisateurOpt.get();
            String token = jwtUtil.generateToken(utilisateurDTO.getEmail(), utilisateurDTO.getRole());

            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "role", utilisateurDTO.getRole(),
                    "email", utilisateurDTO.getEmail(),
                    "utilisateur", utilisateurDTO
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de l'authentification."));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Token manquant ou invalide."));
            }

            String token = authHeader.substring(7);
            String email = jwtUtil.extractEmail(token);

            if (email == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Token invalide."));
            }

            Optional<Utilisateur> utilisateurOpt = utilisateurService.getUtilisateurEntityByEmail(email);

            if (utilisateurOpt.isPresent()) {
                UtilisateurDTO dto = utilisateurService.convertToDTO(utilisateurOpt.get());
                return ResponseEntity.ok(dto);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Utilisateur non trouvé."));
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de la récupération de l'utilisateur connecté."));
        }
    }
}



