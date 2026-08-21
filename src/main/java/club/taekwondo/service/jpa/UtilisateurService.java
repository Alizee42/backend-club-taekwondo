package club.taekwondo.service.jpa;

import club.taekwondo.dto.UtilisateurDTO;
import club.taekwondo.dto.UtilisateurPaiementDTO;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.entity.jpa.Membre;
import club.taekwondo.enums.Role;
import club.taekwondo.repository.jpa.UtilisateurRepository;
import club.taekwondo.repository.jpa.MembreRepository;
import club.taekwondo.entity.jpa.Club;
import club.taekwondo.repository.jpa.ClubRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import club.taekwondo.enums.Genre;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class UtilisateurService {

    private static final Logger log = LoggerFactory.getLogger(UtilisateurService.class);

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final MembreRepository membreRepository;
    private final ClubRepository clubRepository;
    private final NotificationService notificationService;

    @Autowired
    public UtilisateurService(UtilisateurRepository utilisateurRepository, 
                              PasswordEncoder passwordEncoder,
                              MembreRepository membreRepository,
                              ClubRepository clubRepository,
                              @Lazy NotificationService notificationService) {
        this.utilisateurRepository = utilisateurRepository;
        this.passwordEncoder = passwordEncoder;
        this.membreRepository = membreRepository;
        this.clubRepository = clubRepository;
        this.notificationService = notificationService;
    }

    /**
     * Pour compatibilité avec les anciens contrôleurs :
     * Retourne tous les utilisateurs (tous clubs confondus).
     * À utiliser uniquement pour l’admin global !
     */
    public List<UtilisateurDTO> getAllUtilisateurs() {
        List<UtilisateurDTO> result = new ArrayList<>();
        for (Utilisateur u : utilisateurRepository.findAll()) {
            result.add(convertToDTO(u));
        }
        return result;
    }

    // ===== Helpers debug =====
    private String now() { return OffsetDateTime.now().toString(); }
    private String maskPwd(String pwd) { return (pwd == null) ? "null" : "***(" + pwd.length() + " chars)"; }
    private String safeRole(Role r) { return r != null ? r.name() : "null"; }
    private String safeDto(UtilisateurDTO d) {
        if (d == null) return "null";
        return String.format("DTO[id=%s, nom=%s, prenom=%s, email=%s, tel=%s, adr='%s', dateN=%s, role=%s, pwd=%s]",
                d.getId(), d.getNom(), d.getPrenom(), d.getEmail(), d.getTelephone(),
                d.getAdresse(), d.getDateNaissance(), String.valueOf(d.getRole()), maskPwd(d.getPassword()));
    }
    private String safeEntity(Utilisateur u) {
        if (u == null) return "null";
        return String.format("Entity[id=%s, nom=%s, prenom=%s, email=%s, tel=%s, adr='%s', dateN=%s, role=%s]",
                u.getId(), u.getNom(), u.getPrenom(), u.getEmail(), u.getTelephone(),
                u.getAdresse(), u.getDateNaissance(), safeRole(u.getRole()));
    }

    private String lowerOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t.toLowerCase(Locale.ROOT);
    }

    /** Convertit une chaîne (éventuellement null/vide) en Role, avec fallback MEMBRE. */
    private Role parseRoleOrDefault(String roleStr) {
        if (roleStr == null || roleStr.trim().isEmpty()) {
            return Role.MEMBRE;
        }
        try {
            return Role.valueOf(roleStr.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return Role.MEMBRE;
        }
    }

    /** Surcharge pour accepter un objet Role directement (délègue à la version String pour lever l’avertissement IDE) */
    private Role parseRoleOrDefault(Role role) {
        return parseRoleOrDefault(role != null ? role.name() : null);
    }

    /* =======================
     *   Conversions
     * ======================= */

    public UtilisateurDTO convertToDTO(Utilisateur utilisateur) {
        log.debug("[USR-SVC][convertToDTO] in: {}", safeEntity(utilisateur));
        if (utilisateur == null) return null;
        
        UtilisateurDTO dto = new UtilisateurDTO();
        dto.setId(utilisateur.getId());
        dto.setDateNaissance(utilisateur.getDateNaissance());
        dto.setAdresse(utilisateur.getAdresse());
        dto.setEmail(utilisateur.getEmail());
        dto.setTelephone(utilisateur.getTelephone());
        dto.setRole(utilisateur.getRole() != null ? utilisateur.getRole().name() : null);
    dto.setPasswordTemporaire(utilisateur.isPasswordTemporaire());
    dto.setPasswordUpdatedAt(utilisateur.getPasswordUpdatedAt());
        if (utilisateur.getClub() != null) {
            dto.setClubId(utilisateur.getClub().getId());
        }
        if (utilisateur.getGenre() != null) {
            dto.setGenre(utilisateur.getGenre().name());
        }
        
        // 🔧 Pour un parent, récupérer les vraies informations depuis le premier enfant
        if (utilisateur.getRole() == Role.PARENT) {
            // Récupérer le premier membre enfant de ce parent pour avoir le vrai nom
            List<Membre> enfants = membreRepository.findByParentId(utilisateur.getId());
            
            if (!enfants.isEmpty()) {
                Membre premierEnfant = enfants.get(0);
                // Les vraies informations du parent sont stockées dans l'enfant
                dto.setNomParent(premierEnfant.getNom());
                dto.setPrenomParent(premierEnfant.getPrenom());
                
                // Garder les valeurs par défaut pour compatibilité
                dto.setNom(utilisateur.getNom());
                dto.setPrenom(utilisateur.getPrenom());
                
                log.debug("[USR-SVC] Parent enrichi: {} {}", premierEnfant.getPrenom(), premierEnfant.getNom());
            } else {
                // Pas d'enfant trouvé, utiliser les données de base
                dto.setNom(utilisateur.getNom());
                dto.setPrenom(utilisateur.getPrenom());
                dto.setNomParent(utilisateur.getNom());
                dto.setPrenomParent(utilisateur.getPrenom());
            }
        } else {
            // Pour les autres rôles, utiliser les données normales
            dto.setNom(utilisateur.getNom());
            dto.setPrenom(utilisateur.getPrenom());
        }
        
        log.debug("[USR-SVC][convertToDTO] out: {}", safeDto(dto));
        return dto;
    }

    private Utilisateur toUtilisateurEntity(UtilisateurDTO dto) {
        log.debug("[USR-SVC][toEntity] in: {}", safeDto(dto));
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setNom(dto.getNom());
        utilisateur.setPrenom(dto.getPrenom());
        utilisateur.setEmail(lowerOrNull(dto.getEmail())); // peut être null
        utilisateur.setDateNaissance(dto.getDateNaissance());
        utilisateur.setAdresse(dto.getAdresse());
        utilisateur.setTelephone(dto.getTelephone());
    Role parsed = parseRoleOrDefault(dto.getRole()); // ✅ convertit String -> Role
    utilisateur.setRole(parsed);
    utilisateur.setPassword(dto.getPassword()); // déjà encodé si createUtilisateur()
    utilisateur.setPasswordTemporaire(dto.isPasswordTemporaire());        if (dto.getGenre() != null && !dto.getGenre().isBlank()) {
            try { utilisateur.setGenre(Genre.valueOf(dto.getGenre())); } catch (IllegalArgumentException ignored) {}
        }        if (dto.getClubId() != null) {
            Club club = clubRepository.findById(dto.getClubId()).orElse(null);
            utilisateur.setClub(club);
        }
        log.debug("[USR-SVC][toEntity] out: {}", safeEntity(utilisateur));
        return utilisateur;
    }

    /* =======================
     *   Lecture
     * ======================= */

    public List<UtilisateurDTO> getAllUtilisateursByClubId(Long clubId) {
        log.debug("[USR-SVC][getAllUtilisateursByClubId] clubId={}", clubId);
        List<UtilisateurDTO> result = new ArrayList<>();
        for (Utilisateur u : utilisateurRepository.findByClub_Id(clubId)) {
            result.add(convertToDTO(u));
        }
        log.debug("[USR-SVC][getAllUtilisateursByClubId] size={}", result.size());
        return result;
    }

    public Optional<UtilisateurDTO> getUtilisateurByEmail(String email) {
        String e = lowerOrNull(email);
        log.debug("[USR-SVC][getUtilisateurByEmail] in email={} -> normalized={}", email, e);
        Optional<UtilisateurDTO> out = e == null ? Optional.empty()
                : utilisateurRepository.findByEmailIgnoreCase(e).map(this::convertToDTO);
        log.debug("[USR-SVC][getUtilisateurByEmail] present? {}", out.isPresent());
        return out;
    }

    public Optional<Utilisateur> getUtilisateurEntityById(Long id) {
        log.debug("[USR-SVC][getUtilisateurEntityById] id={}", id);
        Optional<Utilisateur> out = utilisateurRepository.findById(id);
        log.debug("[USR-SVC][getUtilisateurEntityById] present? {}", out.isPresent());
        return out;
    }

    public Optional<Utilisateur> getUtilisateurEntityByEmail(String email) {
        String e = lowerOrNull(email);
        log.debug("[USR-SVC][getUtilisateurEntityByEmail] in email={} -> normalized={}", email, e);
        Optional<Utilisateur> out = e == null ? Optional.empty() : utilisateurRepository.findByEmailIgnoreCase(e);
        log.debug("[USR-SVC][getUtilisateurEntityByEmail] present? {}", out.isPresent());
        return out;
    }

    public Optional<Utilisateur> findByNomPrenom(String nom, String prenom) {
        log.debug("[USR-SVC][findByNomPrenom] nom={}, prenom={}", nom, prenom);
        if (nom == null || prenom == null) {
            return Optional.empty();
        }
        Optional<Utilisateur> out = utilisateurRepository.findByNomIgnoreCaseAndPrenomIgnoreCase(nom.trim(), prenom.trim());
        log.debug("[USR-SVC][findByNomPrenom] present? {}", out.isPresent());
        return out;
    }

    /** Exposés pour le PaiementService (évite l’erreur “undefined method”) */
    public Optional<Utilisateur> findByEmailIgnoreCase(String email) {
        String e = lowerOrNull(email);
        log.debug("[USR-SVC][findByEmailIgnoreCase] in email={} -> normalized={}", email, e);
        Optional<Utilisateur> out = e == null ? Optional.empty() : utilisateurRepository.findByEmailIgnoreCase(e);
        log.debug("[USR-SVC][findByEmailIgnoreCase] present? {}", out.isPresent());
        return out;
    }

    public boolean existsByEmailIgnoreCase(String email) {
        String e = lowerOrNull(email);
        boolean exists = e != null && utilisateurRepository.existsByEmailIgnoreCase(e);
        log.debug("[USR-SVC][existsByEmailIgnoreCase] email={} -> normalized={}, exists={}", email, e, exists);
        return exists;
    }

    // Historique si du code appelle encore cette version
    public Optional<Utilisateur> findByEmail(String email) {
        String e = lowerOrNull(email);
        log.debug("[USR-SVC][findByEmail] in email={} -> normalized={}", email, e);
        Optional<Utilisateur> out = e == null ? Optional.empty() : utilisateurRepository.findByEmailIgnoreCase(e);
        log.debug("[USR-SVC][findByEmail] present? {}", out.isPresent());
        return out;
    }

    /* =======================
     *   Authentification
     * ======================= */

    public Optional<UtilisateurDTO> login(String email, String password) {
        // Convention : email|clubId transmis pour login multi-club. Le split doit avoir
        // lieu AVANT la recherche en base, sinon findByEmailIgnoreCase reçoit l'email
        // composé (jamais présent en base) et ne trouve jamais l'utilisateur.
        Long clubIdDemande = null;
        if (email != null && email.contains("|")) {
            String[] parts = email.split("\\|");
            if (parts.length == 2) {
                try {
                    clubIdDemande = Long.parseLong(parts[1]);
                    email = parts[0];
                } catch (NumberFormatException ignore) {}
            }
        }

        String e = lowerOrNull(email);
        log.debug("[USR-SVC][login] email={} -> normalized={}", email, e);
        Optional<Utilisateur> userOpt = utilisateurRepository.findByEmailIgnoreCase(e);
        Optional<UtilisateurDTO> out = userOpt
                .filter(u -> {
                    boolean match = passwordEncoder.matches(password, u.getPassword());
                    log.debug("[USR-SVC][login] password matches? {} for userId={}", match, u.getId());
                    return match;
                })
                .map(this::convertToDTO);
        // Refuser la connexion si l'utilisateur est ADMIN et n'a pas de club
        // Contrôle clubId pour tous les utilisateurs
        if (out.isPresent()) {
            if (clubIdDemande != null && !clubIdDemande.equals(out.get().getClubId())) {
                log.warn("[USR-SVC][login] clubId mismatch, connexion refusée");
                return Optional.empty();
            }
            if (out.get().getClubId() == null) {
                // Autoriser les SUPER_ADMIN à se connecter même s'ils n'ont pas de club
                Role dtoRole = out.get().getRole();
                if (dtoRole == Role.SUPER_ADMIN) {
                    log.debug("[USR-SVC][login] super-admin sans club, connexion autorisée");
                } else {
                    log.warn("[USR-SVC][login] utilisateur sans club, connexion refusée");
                    return Optional.empty();
                }
            }
        }
        log.debug("[USR-SVC][login] success? {}", out.isPresent());
        return out;
    }

    public Utilisateur createUtilisateur(UtilisateurDTO dto) {
        // Par défaut désormais: auto-inscription (password définitif)
        return createUtilisateur(dto, false);
    }

    /**
     * Crée un utilisateur en contrôlant l'origine de la création.
     * @param dto Données utilisateur
     * @param adminCreation true si créé par un admin (mot de passe temporaire), false si auto-inscription (mot de passe définitif)
     */
    public Utilisateur createUtilisateur(UtilisateurDTO dto, boolean adminCreation) {
        log.debug("[USR-SVC][createUtilisateur] in DTO: {}, adminCreation={}", safeDto(dto), adminCreation);

        if (dto.getNom() == null || dto.getNom().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom est requis.");
        }
        if (dto.getEmail() == null || dto.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("L'email est requis.");
        }
        // Gestion différenciée du mot de passe selon l'origine
    if (adminCreation) {
            // Si l'admin ne fournit pas de mot de passe, on en génère un temporaire
            if (dto.getPassword() == null || dto.getPassword().trim().isEmpty()) {
                String generated = java.util.UUID.randomUUID().toString().replace("-", "");
                // raccourcir à 12 caractères pour lisibilité si jamais exposé (il reste encodé avant save)
                generated = generated.substring(0, 12);
                dto.setPassword(generated);
                log.debug("[USR-SVC][createUtilisateur] mot de passe temporaire généré (admin)");
            }
        } else {
            // Auto-inscription: le mot de passe est obligatoire et définitif
            if (dto.getPassword() == null || dto.getPassword().trim().isEmpty()) {
                throw new IllegalArgumentException("Le mot de passe est requis.");
            }
        }

        dto.setEmail(lowerOrNull(dto.getEmail()));
        log.debug("[USR-SVC][createUtilisateur] email normalisé={}", dto.getEmail());

        if (existsByEmailIgnoreCase(dto.getEmail())) {
            throw new IllegalArgumentException("Un utilisateur avec cet email existe déjà.");
        }

        log.debug("[USR-SVC][createUtilisateur] encodage mot de passe...");
        dto.setPassword(passwordEncoder.encode(dto.getPassword()));

        if (dto.getRole() == null) {
            dto.setRole(Role.MEMBRE.name());
            log.debug("[USR-SVC][createUtilisateur] role null -> set MEMBRE");
        } else {
            log.debug("[USR-SVC][createUtilisateur] role reçu='{}'", dto.getRole());
        }

    // Propager l'état temporaire dans le DTO pour toEntity(), puis s'assurer sur l'entité
    dto.setPasswordTemporaire(adminCreation);
        Utilisateur utilisateur = toUtilisateurEntity(dto);
        utilisateur.setPasswordTemporaire(adminCreation);

        // Initialiser la date de mise à jour du mot de passe si absente
        if (utilisateur.getPasswordUpdatedAt() == null) {
            utilisateur.setPasswordUpdatedAt(java.time.OffsetDateTime.now());
        }
        Utilisateur saved = utilisateurRepository.save(utilisateur);
        log.info("[USR-SVC][createUtilisateur] saved: {}", safeEntity(saved));

        // 🔔 Notifications
        try {
            Role savedRole = saved.getRole();
            if (savedRole == Role.ADMIN) {
                // Nouvel admin créé → notifier les super-admins
                notificationService.envoyerNotificationAuxSuperAdmins(
                        "Nouvel administrateur",
                        "Un nouvel administrateur a été créé : " + saved.getPrenom() + " " + saved.getNom() + " (" + saved.getEmail() + ").",
                        "utilisateur",
                        "/super-admin/utilisateurs"
                );
            } else if (!adminCreation && savedRole != null && savedRole != Role.SUPER_ADMIN) {
                // Auto-inscription d'un membre/parent → notifier les admins du club
                if (saved.getClub() != null) {
                    notificationService.envoyerNotificationAuxAdminsClub(
                            saved.getClub().getId(),
                            "Nouvelle inscription",
                            saved.getPrenom() + " " + saved.getNom() + " vient de s'inscrire sur la plateforme.",
                            "utilisateur",
                            "/admin/membres"
                    );
                }
            }
        } catch (Exception ex) {
            System.out.println("[" + now() + "][USR-SVC][createUtilisateur] ⚠ notification échouée: " + ex.getMessage());
        }

        return saved;
    }


    public void updateUtilisateurFromDTO(Long id, UtilisateurDTO dto) {
        System.out.println("[" + now() + "][USR-SVC][updateUtilisateurFromDTO] id=" + id + ", DTO: " + safeDto(dto));
        utilisateurRepository.findById(id).ifPresent(user -> {
            user.setNom(dto.getNom());
            user.setPrenom(dto.getPrenom());
            user.setEmail(lowerOrNull(dto.getEmail()));
            user.setTelephone(dto.getTelephone());
            user.setAdresse(dto.getAdresse());
            user.setDateNaissance(dto.getDateNaissance());
            user.setRole(parseRoleOrDefault(dto.getRole()));

            if (dto.getPassword() != null && !dto.getPassword().trim().isEmpty()) {
                System.out.println("[" + now() + "][USR-SVC][updateUtilisateurFromDTO] encodage nouveau mot de passe...");
                user.setPassword(passwordEncoder.encode(dto.getPassword()));
                user.setPasswordTemporaire(false); // Si on change le mot de passe, il n'est plus temporaire
                user.setPasswordUpdatedAt(java.time.OffsetDateTime.now());
            }

            if (dto.getClubId() != null) {
                Club club = clubRepository.findById(dto.getClubId()).orElse(null);
                user.setClub(club);
            }

            utilisateurRepository.save(user);
            System.out.println("[" + now() + "][USR-SVC][updateUtilisateurFromDTO] ✅ updated userId=" + user.getId());
        });
    }

    /**
     * Mise à jour "self-service" du profil par l'utilisateur connecté lui-même.
     * Volontairement restreinte aux champs personnels : ne touche jamais au rôle,
     * au club ni au mot de passe (contrairement à updateUtilisateurFromDTO, réservée aux admins).
     */
    public void updateProfilPersonnel(Long id, UtilisateurDTO dto) {
        utilisateurRepository.findById(id).ifPresent(user -> {
            user.setNom(dto.getNom());
            user.setPrenom(dto.getPrenom());
            user.setEmail(lowerOrNull(dto.getEmail()));
            user.setTelephone(dto.getTelephone());
            user.setAdresse(dto.getAdresse());
            user.setDateNaissance(dto.getDateNaissance());
            utilisateurRepository.save(user);
        });
    }

    /**
     * Changement de mot de passe "self-service" : exige le mot de passe actuel
     * pour éviter qu'un token volé suffise à prendre le contrôle du compte.
     * @return true si le changement a eu lieu, false si le mot de passe actuel est incorrect.
     */
    public boolean changerMotDePassePersonnel(Long id, String currentPassword, String newPassword) {
        Optional<Utilisateur> userOpt = utilisateurRepository.findById(id);
        if (userOpt.isEmpty()) return false;

        Utilisateur user = userOpt.get();
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return false;
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordTemporaire(false);
        user.setPasswordUpdatedAt(java.time.OffsetDateTime.now());
        utilisateurRepository.save(user);
        return true;
    }

    /* =======================
     *   Suppression
     * ======================= */

    public void deleteUtilisateur(Long id) {
        System.out.println("[" + now() + "][USR-SVC][deleteUtilisateur] id=" + id);
        utilisateurRepository.deleteById(id);
        System.out.println("[" + now() + "][USR-SVC][deleteUtilisateur] ✅ deleted id=" + id);
    }

    /* =======================
     *   Paiements (DTO léger)
     * ======================= */

    public List<UtilisateurPaiementDTO> getAllWithPaiements() {
        System.out.println("[" + now() + "][USR-SVC][getAllWithPaiements] start");
        List<UtilisateurPaiementDTO> result = new ArrayList<>();
        for (Utilisateur u : utilisateurRepository.findAll()) {
            UtilisateurPaiementDTO dto = new UtilisateurPaiementDTO();
            dto.setId(u.getId());
            dto.setNom(u.getNom());
            dto.setPrenom(u.getPrenom());
            dto.setEmail(u.getEmail());
            dto.setRole(u.getRole() != null ? u.getRole().name() : "MEMBRE");
            result.add(dto);
        }
        System.out.println("[" + now() + "][USR-SVC][getAllWithPaiements] size=" + result.size());
        return result;
    }

    /* =======================
     *   Sauvegarde bas niveau
     * ======================= */

    public Utilisateur save(Utilisateur utilisateur) {
        System.out.println("[" + now() + "][USR-SVC][save] in: " + safeEntity(utilisateur));
        utilisateur.setEmail(lowerOrNull(utilisateur.getEmail()));
        Utilisateur saved = utilisateurRepository.save(utilisateur);
        System.out.println("[" + now() + "][USR-SVC][save] ✅ out: " + safeEntity(saved));
        return saved;
    }
}
