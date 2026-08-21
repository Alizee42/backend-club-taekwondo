package club.taekwondo.controller.jpa;

import club.taekwondo.dto.LoginDTO;
import club.taekwondo.dto.UtilisateurDTO;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.entity.jpa.Membre;
import club.taekwondo.enums.Role;
import club.taekwondo.repository.jpa.MembreRepository;
import club.taekwondo.security.JwtRevocationService;
import club.taekwondo.service.jpa.EmailService;
import club.taekwondo.service.jpa.ReinitialisationMotDePasseService;
import club.taekwondo.security.JwtUtil;
import club.taekwondo.service.jpa.UtilisateurService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/utilisateurs")
public class UtilisateurController {
    @PostMapping("")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> createUtilisateur(@RequestBody UtilisateurDTO utilisateurDTO) {
        try {
            // Création par ADMIN / SUPER-ADMIN -> mot de passe TEMPORAIRE + envoi email de réinitialisation
            Utilisateur nouvelUtilisateur = utilisateurService.createUtilisateur(utilisateurDTO, true);

            // Générer un token de réinitialisation et envoyer l'email (best-effort)
            try {
                var demande = reinitService.creerDemande(nouvelUtilisateur.getId());
                if (nouvelUtilisateur.getEmail() != null && !nouvelUtilisateur.getEmail().isBlank()) {
                    emailService.envoyerEmailReinitialisationMotDePasse(nouvelUtilisateur.getEmail(), demande.getToken());
                }
            } catch (Exception ignore) {}

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                            "message", "Utilisateur créé. Un email de définition de mot de passe a été envoyé.",
                            "id", nouvelUtilisateur.getId(),
                            "email", nouvelUtilisateur.getEmail(),
                            "role", nouvelUtilisateur.getRole() != null ? nouvelUtilisateur.getRole().name() : null,
                            "passwordTemporaire", true
                    ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Erreur lors de la création."));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> updateUtilisateur(@PathVariable Long id, @RequestBody UtilisateurDTO utilisateurDTO) {
        try {
            utilisateurDTO.setId(id);
            utilisateurService.updateUtilisateurFromDTO(id, utilisateurDTO);
            // Récupérer l'utilisateur mis à jour pour le retourner
            Optional<Utilisateur> utilisateurOpt = utilisateurService.getUtilisateurEntityById(id);
            if (utilisateurOpt.isPresent()) {
                return ResponseEntity.ok(utilisateurOpt.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Utilisateur non trouvé après modification."));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Erreur lors de la modification."));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> deleteUtilisateur(@PathVariable Long id) {
        try {
            utilisateurService.deleteUtilisateur(id);
            return ResponseEntity.ok(Map.of("message", "Utilisateur supprimé."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Erreur lors de la suppression."));
        }
    }

    private final JwtUtil jwtUtil;
    private final UtilisateurService utilisateurService;
    private final MembreRepository membreRepository;
    private final EmailService emailService;
    private final ReinitialisationMotDePasseService reinitService;
    private final JwtRevocationService jwtRevocationService;

    @Autowired
    public UtilisateurController(JwtUtil jwtUtil,
                                 UtilisateurService utilisateurService,
                                 MembreRepository membreRepository,
                                 EmailService emailService,
                                 ReinitialisationMotDePasseService reinitService,
                                 JwtRevocationService jwtRevocationService) {
        this.jwtUtil = jwtUtil;
        this.utilisateurService = utilisateurService;
        this.membreRepository = membreRepository;
        this.emailService = emailService;
        this.reinitService = reinitService;
        this.jwtRevocationService = jwtRevocationService;
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
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<UtilisateurDTO>> listUtilisateurs(
            @RequestParam(value = "role", required = false) String role,
            @RequestParam(value = "clubId", required = false) Long clubId,
            @RequestParam(value = "q", required = false) String q) {

        System.out.println("[" + now() + "][USR][LIST] params role=" + role + ", q=" + q);

    org.springframework.security.core.Authentication authentication =
        org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

    // ADMIN : restreindre au périmètre de son propre club
    boolean isSuperAdmin = authentication.getAuthorities().stream()
        .anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a.getAuthority()));
    if (!isSuperAdmin) {
        Optional<club.taekwondo.entity.jpa.Utilisateur> adminEntity =
            utilisateurService.getUtilisateurEntityByEmail(authentication.getName());
        if (adminEntity.isPresent() && adminEntity.get().getClub() != null) {
            Long adminClubId = adminEntity.get().getClub().getId();
            if (clubId != null && !clubId.equals(adminClubId)) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).build();
            }
            clubId = adminClubId; // force le scope club de l'admin
        }
    }

    List<UtilisateurDTO> all = Optional.ofNullable(utilisateurService.getAllUtilisateurs())
        .orElse(Collections.emptyList());

    if (role != null && !role.isBlank()) {
        String wanted = role.trim().toUpperCase(Locale.ROOT);
        all = all.stream()
            .filter(u -> u != null && u.getRole() != null
                && String.valueOf(u.getRole()).equalsIgnoreCase(wanted))
            .toList();
    }

    if (clubId != null) {
        final Long finalClubId = clubId;
        all = all.stream()
            .filter(u -> u != null && u.getClubId() != null && u.getClubId().equals(finalClubId))
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

            // Auto-inscription: mot de passe défini par l'utilisateur (pas temporaire)
            Utilisateur nouvelUtilisateur = utilisateurService.createUtilisateur(utilisateurDTO, false);

            // tenter d'envoyer un email de confirmation (ne doit pas empêcher la création)
            boolean emailSent = false;
            try {
                if (nouvelUtilisateur.getEmail() != null && !nouvelUtilisateur.getEmail().isBlank()) {
                    String nom = nouvelUtilisateur.getPrenom() != null ? nouvelUtilisateur.getPrenom() : nouvelUtilisateur.getNom();
                    emailService.envoyerEmailConfirmationInscription(nouvelUtilisateur.getEmail(), nom != null ? nom : "");
                    emailSent = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
                emailSent = false;
            }

            // notifier le club de la nouvelle inscription (ne doit pas empêcher la création)
            try {
                if (nouvelUtilisateur.getClub() != null) {
                    String nomComplet = ((nouvelUtilisateur.getPrenom() != null ? nouvelUtilisateur.getPrenom() : "") + " "
                            + (nouvelUtilisateur.getNom() != null ? nouvelUtilisateur.getNom() : "")).trim();
                    String role = nouvelUtilisateur.getRole() != null ? nouvelUtilisateur.getRole().name() : "";
                    emailService.envoyerNotificationInscriptionClub(
                            nouvelUtilisateur.getClub().getEmail(),
                            nouvelUtilisateur.getClub().getName(),
                            nomComplet,
                            nouvelUtilisateur.getEmail(),
                            role);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            Map<String, Object> response = new java.util.HashMap<>();
            response.put("message", "Utilisateur créé avec succès.");
            response.put("id", nouvelUtilisateur.getId());
            response.put("email", nouvelUtilisateur.getEmail());
            response.put("role", nouvelUtilisateur.getRole() != null ? nouvelUtilisateur.getRole().name() : null);
            response.put("emailSent", emailSent);

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

    // Vérifier si le mot de passe est temporaire
    boolean passwordTemporaire = false;
    Optional<Utilisateur> userEntityOpt = utilisateurService.getUtilisateurEntityById(utilisateurDTO.getId());
    if (userEntityOpt.isPresent()) {
        passwordTemporaire = userEntityOpt.get().isPasswordTemporaire();
    }

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
    response.put("passwordTemporaire", passwordTemporaire);
    // Le resetToken n'est plus renvoyé dans la réponse de login pour éviter l'exposition
    // dans l'historique navigateur, les logs et les referers.
    // Le frontend doit rediriger vers /mot-de-passe-oublie lorsque passwordTemporaire est true.

	    return ResponseEntity.ok(response);

	    } catch (Exception e) {
	        e.printStackTrace();
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                .body(Map.of("message", "Erreur lors de l'authentification."));
	    }
	}

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Token manquant ou invalide."));
        }

        String token = authHeader.substring(7).trim();
        if (token.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Token manquant ou invalide."));
        }

        try {
            jwtRevocationService.revokeToken(token);
            return ResponseEntity.ok(Map.of("message", "Deconnexion prise en compte."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Token manquant ou invalide."));
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

    @PutMapping("/me")
    public ResponseEntity<?> updateCurrentUser(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                                @RequestBody UtilisateurDTO utilisateurDTO) {
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
            if (utilisateurOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Utilisateur non trouvé."));
            }

            Long id = utilisateurOpt.get().getId();
            utilisateurService.updateProfilPersonnel(id, utilisateurDTO);

            Optional<Utilisateur> updated = utilisateurService.getUtilisateurEntityById(id);
            return ResponseEntity.ok(utilisateurService.convertToDTO(updated.get()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Erreur lors de la modification du profil."));
        }
    }

    @PutMapping("/me/mot-de-passe")
    public ResponseEntity<?> updateCurrentUserPassword(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                                         @RequestBody Map<String, String> body) {
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
            if (utilisateurOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Utilisateur non trouvé."));
            }

            String currentPassword = body.get("currentPassword");
            String newPassword = body.get("newPassword");
            if (currentPassword == null || newPassword == null || newPassword.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Mot de passe actuel et nouveau mot de passe requis."));
            }

            boolean ok = utilisateurService.changerMotDePassePersonnel(utilisateurOpt.get().getId(), currentPassword, newPassword);
            if (!ok) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Mot de passe actuel incorrect."));
            }

            return ResponseEntity.ok(Map.of("message", "Mot de passe mis à jour."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Erreur lors de la mise à jour du mot de passe."));
        }
    }
}
