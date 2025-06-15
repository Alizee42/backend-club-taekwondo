package club.taekwondo.controller.jpa;

import club.taekwondo.security.JwtUtil;
import club.taekwondo.dto.UtilisateurDTO;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.service.jpa.UtilisateurService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/utilisateurs")
@CrossOrigin(origins = "*") // À restreindre en production
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
    public List<UtilisateurDTO> getAllUtilisateurs() {
        return utilisateurService.getAllUtilisateurs();
    }

    @GetMapping("/{id}")
    public Optional<Utilisateur> getUtilisateurById(@PathVariable Long id) {
        return utilisateurService.getUtilisateurById(id);
    }

    @PostMapping
    public Utilisateur createUtilisateur(@RequestBody UtilisateurDTO utilisateurDTO) {
       
        return utilisateurService.createUtilisateur(utilisateurDTO);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUtilisateur(@PathVariable Long id, @RequestBody UtilisateurDTO utilisateurDTO) {
        Optional<Utilisateur> existingUserOptional = utilisateurService.getUtilisateurById(id);
        if (existingUserOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Utilisateur non trouvé avec l'ID : " + id);
        }

        utilisateurService.updateUtilisateurFromDTO(id, utilisateurDTO);
        return ResponseEntity.ok("Utilisateur mis à jour avec succès.");
    }


    @DeleteMapping("/{id}")
    public void deleteUtilisateur(@PathVariable Long id) {
        utilisateurService.deleteUtilisateur(id);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam("email") String email, @RequestParam("password") String password) {
        try {
          Optional<UtilisateurDTO> optionalUtilisateurDTO = utilisateurService.login(email, password);
          if (optionalUtilisateurDTO.isPresent()) {
        	  UtilisateurDTO utilisateurDTO = optionalUtilisateurDTO.get();
        	  String token = jwtUtil.generateToken(email, utilisateurDTO.getRole());

              return ResponseEntity.ok(Map.of(
                  "token", token,
                  "role", utilisateurDTO.getRole(),
                  "email", utilisateurDTO.getEmail()
              ));
          }
          else {
        	  return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Email ou mot de passe incorrect.");
          }
           
        } catch (RuntimeException e) {
            System.out.println("Erreur lors de la connexion : " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Erreur interne");
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String token) {
        try {
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            String email = jwtUtil.extractEmail(token);

            UtilisateurDTO utilisateur = utilisateurService.getUtilisateurByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            return ResponseEntity.ok(utilisateur);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token invalide ou utilisateur non trouvé.");
        }
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateCurrentUser(@RequestHeader("Authorization") String token, @RequestBody Utilisateur updatedUser) {
        try {
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            String email = jwtUtil.extractEmail(token);
            UtilisateurDTO utilisateur = utilisateurService.getUtilisateurByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            utilisateur.setNom(updatedUser.getNom());
            utilisateur.setPrenom(updatedUser.getPrenom());
            utilisateur.setTelephone(updatedUser.getTelephone());
            utilisateur.setEmail(updatedUser.getEmail());

            utilisateurService.createUtilisateur(utilisateur);

            return ResponseEntity.ok(Map.of("message", "Profil mis à jour avec succès."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Token invalide ou utilisateur non trouvé."));
        }
    }

    @PutMapping("/me/password")
    public ResponseEntity<?> updatePassword(@RequestHeader("Authorization") String token, @RequestBody Map<String, String> passwordData) {
        try {
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            String email = jwtUtil.extractEmail(token);
            UtilisateurDTO utilisateurDTO = utilisateurService.getUtilisateurByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            String newPassword = passwordData.get("password");
            if (newPassword == null || newPassword.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Le mot de passe ne peut pas être vide."));
            }

            // Hachage du mot de passe
            utilisateurDTO.setPassword(passwordEncoder.encode(newPassword));
            utilisateurService.createUtilisateur(utilisateurDTO);

            return ResponseEntity.ok(Map.of("message", "Mot de passe mis à jour avec succès."));
        } catch (Exception e) {
            System.out.println("Erreur lors de la mise à jour du mot de passe : " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Une erreur est survenue lors de la mise à jour du mot de passe."));
        }
    }
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UtilisateurDTO utilisateurDTO) {
        try {
            // Vérifiez si l'email est déjà utilisé
            if (utilisateurService.getUtilisateurByEmail(utilisateurDTO.getEmail()).isPresent()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Cet email est déjà utilisé."));
            }

            // Attribuez un rôle par défaut si aucun rôle n'est fourni
            if (utilisateurDTO.getRole() == null || utilisateurDTO.getRole().isEmpty()) {
            	utilisateurDTO.setRole("membre"); // Rôle par défaut
            }

            // Créez l'utilisateur (le mot de passe sera haché dans le service)
            Utilisateur nouvelUtilisateur = utilisateurService.createUtilisateur(utilisateurDTO);

            // Retournez une réponse de succès
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Utilisateur créé avec succès.",
                "id", nouvelUtilisateur.getId(),
                "email", nouvelUtilisateur.getEmail(),
                "role", nouvelUtilisateur.getRole()
            ));
        } catch (Exception e) {
            // Gestion des erreurs
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Une erreur est survenue lors de l'inscription."));
        }
    }
    
}

