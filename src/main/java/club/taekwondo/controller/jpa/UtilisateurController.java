package club.taekwondo.controller.jpa;

import club.taekwondo.dto.LoginDTO;
import club.taekwondo.dto.UtilisateurDTO;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.entity.jpa.Membre;
import club.taekwondo.enums.Role;
import club.taekwondo.repository.jpa.MembreRepository;
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
    private final MembreRepository membreRepository;

    @Autowired
    public UtilisateurController(JwtUtil jwtUtil,
                                 UtilisateurService utilisateurService,
                                 MembreRepository membreRepository) {
        this.jwtUtil = jwtUtil;
        this.utilisateurService = utilisateurService;
        this.membreRepository = membreRepository;
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

        List<UtilisateurDTO> all = Optional.ofNullable(utilisateurService.getAllUtilisateurs())
                .orElse(Collections.emptyList());

        if (role != null && !role.isBlank()) {
            String wanted = role.trim().toUpperCase(Locale.ROOT);
            all = all.stream()
                    .filter(u -> u != null && u.getRole() != null
                            && String.valueOf(u.getRole()).equalsIgnoreCase(wanted))
                    .toList();
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
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Email et mot de passe sont obligatoires."));
        }

        Optional<UtilisateurDTO> utilisateurOpt = utilisateurService.login(loginDTO.getEmail(), loginDTO.getPassword());
        if (utilisateurOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Email ou mot de passe incorrect."));
        }

        UtilisateurDTO utilisateurDTO = utilisateurOpt.get();
        String roleStr = utilisateurDTO.getRole() != null ? String.valueOf(utilisateurDTO.getRole()) : "MEMBRE";

        // 🔥 Chercher le membreId associé à cet utilisateur
        Long membreId = membreRepository.findByCompteUtilisateur_Id(utilisateurDTO.getId())
                .map(Membre::getId)
                .orElse(null);

        // Génération du token avec utilisateurId + membreId
        String token = jwtUtil.generateToken(
                utilisateurDTO.getEmail(),
                roleStr,
                utilisateurDTO.getId(),
                membreId
        );

        // ✅ Utilisation de HashMap au lieu de Map.of pour éviter les NPE
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("token", token);
        response.put("role", roleStr);
        response.put("email", utilisateurDTO.getEmail());
        response.put("utilisateurId", utilisateurDTO.getId());
        response.put("membreId", membreId); // peut rester null sans problème
        response.put("utilisateur", utilisateurDTO);

        return ResponseEntity.ok(response);

    } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Erreur lors de l'authentification."));
    }
}


    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader(value = "Authorization", required = false) String authHeader) {
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
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de la récupération de l'utilisateur connecté."));
        }
    }
}
