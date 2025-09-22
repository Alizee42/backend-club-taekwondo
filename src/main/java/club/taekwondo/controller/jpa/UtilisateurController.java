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

import java.time.OffsetDateTime;
import java.util.Collections;
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

    // -------- Helpers debug --------
    private String now() {
        return OffsetDateTime.now().toString();
    }
    private String maskPwd(String pwd) {
        return (pwd == null) ? "null" : "***(" + pwd.length() + " chars)";
    }
    private String safeUser(UtilisateurDTO u) {
        if (u == null) return "null";
        return String.format(
            "nom=%s, prenom=%s, email=%s, tel=%s, adresse='%s', dateNaissance=%s, role=%s, password=%s",
            u.getNom(), u.getPrenom(), u.getEmail(), u.getTelephone(),
            u.getAdresse(), u.getDateNaissance(), String.valueOf(u.getRole()),
            maskPwd(u.getPassword())
        );
    }

    @GetMapping("")
    public ResponseEntity<List<UtilisateurDTO>> listUtilisateurs(
            @RequestParam(value = "role", required = false) String role,
            @RequestParam(value = "q", required = false) String q) {

        System.out.println("[" + now() + "][USR][LIST] params role=" + role + ", q=" + q);

        // ⚙️ Sécurise contre un retour null du service
        List<UtilisateurDTO> all = Optional.ofNullable(utilisateurService.getAllUtilisateurs())
                .orElse(Collections.emptyList());

        System.out.println("[" + now() + "][USR][LIST] total avant filtres=" + all.size());

        if (role != null && !role.isBlank()) {
            String wanted = role.trim().toUpperCase(Locale.ROOT);
            all = all.stream()
                    .filter(u -> {
                        Object r = (u != null) ? u.getRole() : null;
                        if (r == null) return false;
                        String val = String.valueOf(r);
                        return val.equalsIgnoreCase(wanted);
                    })
                    .toList();
            System.out.println("[" + now() + "][USR][LIST] après filtre role=" + wanted + " => " + all.size());
        }

        if (q != null && !q.isBlank()) {
            String s = q.trim().toLowerCase(Locale.ROOT);
            all = all.stream()
                    .filter(u -> u != null && (
                            (u.getNom() != null && u.getNom().toLowerCase(Locale.ROOT).contains(s)) ||
                            (u.getPrenom() != null && u.getPrenom().toLowerCase(Locale.ROOT).contains(s)) ||
                            (u.getEmail() != null && u.getEmail().toLowerCase(Locale.ROOT).contains(s))
                    ))
                    .toList();
            System.out.println("[" + now() + "][USR][LIST] après filtre q='" + s + "' => " + all.size());
        }

        return ResponseEntity.ok(all);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody(required = false) UtilisateurDTO utilisateurDTO) {
        System.out.println("[" + now() + "][USR][REGISTER] ⬅ payload reçu: " + safeUser(utilisateurDTO));
        try {
            if (utilisateurDTO == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Requête invalide : données manquantes."));
            }

            String email = utilisateurDTO.getEmail();
            if (email == null || email.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Email obligatoire."));
            }

            Optional<Utilisateur> exist = utilisateurService.getUtilisateurEntityByEmail(email);
            if (exist.isPresent()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Cet email est déjà utilisé."));
            }

            if (utilisateurDTO.getRole() == null || String.valueOf(utilisateurDTO.getRole()).isBlank()) {
                utilisateurDTO.setRole(Role.MEMBRE.name());
            }

            Utilisateur nouvelUtilisateur = utilisateurService.createUtilisateur(utilisateurDTO);

            Map<String, Object> response = new java.util.HashMap<>();
            response.put("message", "Utilisateur créé avec succès.");
            response.put("id", nouvelUtilisateur.getId());
            response.put("email", nouvelUtilisateur.getEmail());
            response.put("role", nouvelUtilisateur.getRole() != null ? nouvelUtilisateur.getRole().name() : null);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de l'inscription."));
        }
    }


    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody(required = false) LoginDTO loginDTO) {
        System.out.println("[" + now() + "][USR][LOGIN] ⬅ payload reçu");
        try {
            if (loginDTO == null || loginDTO.getEmail() == null || loginDTO.getEmail().isBlank()
                    || loginDTO.getPassword() == null || loginDTO.getPassword().isBlank()) {
                System.out.println("[" + now() + "][USR][LOGIN] ❌ email ou mot de passe manquant");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Email et mot de passe sont obligatoires."));
            }

            System.out.println("[" + now() + "][USR][LOGIN] ⬅ email=" + loginDTO.getEmail()
                    + ", password=" + maskPwd(loginDTO.getPassword()));

            Optional<UtilisateurDTO> utilisateurOpt = utilisateurService.login(loginDTO.getEmail(), loginDTO.getPassword());
            if (utilisateurOpt.isEmpty()) {
                System.out.println("[" + now() + "][USR][LOGIN] ❌ échec authentification -> 401");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Email ou mot de passe incorrect."));
            }

            UtilisateurDTO utilisateurDTO = utilisateurOpt.get();
            String roleStr = String.valueOf(utilisateurDTO.getRole());
            System.out.println("[" + now() + "][USR][LOGIN] ✅ OK email=" + utilisateurDTO.getEmail()
                    + ", role=" + roleStr);

            String token = jwtUtil.generateToken(utilisateurDTO.getEmail(), roleStr);

            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "role", roleStr,
                    "email", utilisateurDTO.getEmail(),
                    "utilisateur", utilisateurDTO
            ));
        } catch (Exception e) {
            System.out.println("[" + now() + "][USR][LOGIN] ❌ Exception: " + e.getClass().getSimpleName()
                    + " -> " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de l'authentification."));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        System.out.println("[" + now() + "][USR][ME] ⬅ Authorization header présent? " + (authHeader != null));
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                System.out.println("[" + now() + "][USR][ME] ❌ token manquant ou invalide");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Token manquant ou invalide."));
            }

            String token = authHeader.substring(7);
            String email = jwtUtil.extractEmail(token);
            System.out.println("[" + now() + "][USR][ME] email extrait du token = " + email);

            if (email == null) {
                System.out.println("[" + now() + "][USR][ME] ❌ email null -> token invalide");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Token invalide."));
            }

            Optional<Utilisateur> utilisateurOpt = utilisateurService.getUtilisateurEntityByEmail(email);
            if (utilisateurOpt.isPresent()) {
                UtilisateurDTO dto = utilisateurService.convertToDTO(utilisateurOpt.get());
                System.out.println("[" + now() + "][USR][ME] ✅ utilisateur trouvé id=" + utilisateurOpt.get().getId());
                return ResponseEntity.ok(dto);
            } else {
                System.out.println("[" + now() + "][USR][ME] ❌ utilisateur non trouvé pour email=" + email);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Utilisateur non trouvé."));
            }

        } catch (Exception e) {
            System.out.println("[" + now() + "][USR][ME] ❌ Exception: " + e.getClass().getSimpleName()
                    + " -> " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de la récupération de l'utilisateur connecté."));
        }
    }
}

