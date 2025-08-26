package club.taekwondo.controller.jpa;

import club.taekwondo.dto.LoginDTO;
import club.taekwondo.dto.UtilisateurDTO;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.enums.Role;
import club.taekwondo.security.JwtUtil;
import club.taekwondo.service.jpa.UtilisateurService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;
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

    @GetMapping("")
    public ResponseEntity<List<UtilisateurDTO>> listUtilisateurs(
            @RequestParam(value = "role", required = false) String role,
            @RequestParam(value = "q", required = false) String q) {

        List<UtilisateurDTO> all = utilisateurService.getAllUtilisateurs();

        if (role != null && !role.isBlank()) {
            String wanted = role.trim().toUpperCase(Locale.ROOT);
            all = all.stream()
                    .filter(u -> {
                        Object r = u.getRole();
                        if (r == null) return false;
                        String val = String.valueOf(r);
                        return val.equalsIgnoreCase(wanted);
                    })
                    .toList();
        }

        if (q != null && !q.isBlank()) {
            String s = q.trim().toLowerCase(Locale.ROOT);
            all = all.stream()
                    .filter(u ->
                            (u.getNom() != null && u.getNom().toLowerCase(Locale.ROOT).contains(s)) ||
                            (u.getPrenom() != null && u.getPrenom().toLowerCase(Locale.ROOT).contains(s)) ||
                            (u.getEmail() != null && u.getEmail().toLowerCase(Locale.ROOT).contains(s))
                    )
                    .toList();
        }

        return ResponseEntity.ok(all);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UtilisateurDTO utilisateurDTO) {
        try {
            if (utilisateurService.getUtilisateurByEmail(utilisateurDTO.getEmail()).isPresent()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Cet email est déjà utilisé."));
            }

            // ✅ ton DTO attend une String -> on passe Role.MEMBRE.name()
            if (utilisateurDTO.getRole() == null || String.valueOf(utilisateurDTO.getRole()).isBlank()) {
                utilisateurDTO.setRole(Role.MEMBRE.name());
            }

            Utilisateur nouvelUtilisateur = utilisateurService.createUtilisateur(utilisateurDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "Utilisateur créé avec succès.",
                    "id", nouvelUtilisateur.getId(),
                    "email", nouvelUtilisateur.getEmail(),
                    "role", nouvelUtilisateur.getRole() != null ? nouvelUtilisateur.getRole().name() : null
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
            String roleStr = String.valueOf(utilisateurDTO.getRole());

            String token = jwtUtil.generateToken(utilisateurDTO.getEmail(), roleStr);

            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "role", roleStr,
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
