package club.taekwondo.service.jpa;

import club.taekwondo.dto.UtilisateurDTO;
import club.taekwondo.dto.UtilisateurPaiementDTO;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.enums.Role;
import club.taekwondo.repository.jpa.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UtilisateurService(UtilisateurRepository utilisateurRepository, PasswordEncoder passwordEncoder) {
        this.utilisateurRepository = utilisateurRepository;
        this.passwordEncoder = passwordEncoder;
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

    /** Surcharge pour accepter un objet Role directement */
    private Role parseRoleOrDefault(Role role) {
        return role != null ? role : Role.MEMBRE;
    }

    /* =======================
     *   Conversions
     * ======================= */

    public UtilisateurDTO convertToDTO(Utilisateur utilisateur) {
        System.out.println("[" + now() + "][USR-SVC][convertToDTO] in: " + safeEntity(utilisateur));
        if (utilisateur == null) return null;
        UtilisateurDTO dto = new UtilisateurDTO();
        dto.setId(utilisateur.getId());
        dto.setNom(utilisateur.getNom());
        dto.setPrenom(utilisateur.getPrenom());
        dto.setDateNaissance(utilisateur.getDateNaissance());
        dto.setAdresse(utilisateur.getAdresse());
        dto.setEmail(utilisateur.getEmail());
        dto.setTelephone(utilisateur.getTelephone());
        dto.setRole(utilisateur.getRole() != null ? utilisateur.getRole().name() : null); // String coté DTO
        System.out.println("[" + now() + "][USR-SVC][convertToDTO] out: " + safeDto(dto));
        return dto;
    }

    private Utilisateur toUtilisateurEntity(UtilisateurDTO dto) {
        System.out.println("[" + now() + "][USR-SVC][toEntity] in: " + safeDto(dto));
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
        System.out.println("[" + now() + "][USR-SVC][toEntity] out: " + safeEntity(utilisateur));
        return utilisateur;
    }

    /* =======================
     *   Lecture
     * ======================= */

    public List<UtilisateurDTO> getAllUtilisateurs() {
        System.out.println("[" + now() + "][USR-SVC][getAllUtilisateurs] start");
        List<UtilisateurDTO> result = new ArrayList<>();
        for (Utilisateur u : utilisateurRepository.findAll()) {
            result.add(convertToDTO(u));
        }
        System.out.println("[" + now() + "][USR-SVC][getAllUtilisateurs] size=" + result.size());
        return result;
    }

    public Optional<UtilisateurDTO> getUtilisateurByEmail(String email) {
        String e = lowerOrNull(email);
        System.out.println("[" + now() + "][USR-SVC][getUtilisateurByEmail] in email=" + email + " -> normalized=" + e);
        Optional<UtilisateurDTO> out = e == null ? Optional.empty()
                : utilisateurRepository.findByEmailIgnoreCase(e).map(this::convertToDTO);
        System.out.println("[" + now() + "][USR-SVC][getUtilisateurByEmail] present? " + out.isPresent());
        return out;
    }

    public Optional<Utilisateur> getUtilisateurEntityById(Long id) {
        System.out.println("[" + now() + "][USR-SVC][getUtilisateurEntityById] id=" + id);
        Optional<Utilisateur> out = utilisateurRepository.findById(id);
        System.out.println("[" + now() + "][USR-SVC][getUtilisateurEntityById] present? " + out.isPresent());
        return out;
    }

    public Optional<Utilisateur> getUtilisateurEntityByEmail(String email) {
        String e = lowerOrNull(email);
        System.out.println("[" + now() + "][USR-SVC][getUtilisateurEntityByEmail] in email=" + email + " -> normalized=" + e);
        Optional<Utilisateur> out = e == null ? Optional.empty() : utilisateurRepository.findByEmailIgnoreCase(e);
        System.out.println("[" + now() + "][USR-SVC][getUtilisateurEntityByEmail] present? " + out.isPresent());
        return out;
    }

    public Optional<Utilisateur> findByNomPrenom(String nom, String prenom) {
        System.out.println("[" + now() + "][USR-SVC][findByNomPrenom] nom=" + nom + ", prenom=" + prenom);
        if (nom == null || prenom == null) {
            System.out.println("[" + now() + "][USR-SVC][findByNomPrenom] -> empty (null param)");
            return Optional.empty();
        }
        Optional<Utilisateur> out = utilisateurRepository.findByNomIgnoreCaseAndPrenomIgnoreCase(nom.trim(), prenom.trim());
        System.out.println("[" + now() + "][USR-SVC][findByNomPrenom] present? " + out.isPresent());
        return out;
    }

    /** Exposés pour le PaiementService (évite l’erreur “undefined method”) */
    public Optional<Utilisateur> findByEmailIgnoreCase(String email) {
        String e = lowerOrNull(email);
        System.out.println("[" + now() + "][USR-SVC][findByEmailIgnoreCase] in email=" + email + " -> normalized=" + e);
        Optional<Utilisateur> out = e == null ? Optional.empty() : utilisateurRepository.findByEmailIgnoreCase(e);
        System.out.println("[" + now() + "][USR-SVC][findByEmailIgnoreCase] present? " + out.isPresent());
        return out;
    }

    public boolean existsByEmailIgnoreCase(String email) {
        String e = lowerOrNull(email);
        boolean exists = e != null && utilisateurRepository.existsByEmailIgnoreCase(e);
        System.out.println("[" + now() + "][USR-SVC][existsByEmailIgnoreCase] email=" + email + " -> normalized=" + e + ", exists=" + exists);
        return exists;
    }

    // Historique si du code appelle encore cette version
    public Optional<Utilisateur> findByEmail(String email) {
        String e = lowerOrNull(email);
        System.out.println("[" + now() + "][USR-SVC][findByEmail] in email=" + email + " -> normalized=" + e);
        Optional<Utilisateur> out = e == null ? Optional.empty() : utilisateurRepository.findByEmailIgnoreCase(e);
        System.out.println("[" + now() + "][USR-SVC][findByEmail] present? " + out.isPresent());
        return out;
    }

    /* =======================
     *   Authentification
     * ======================= */

    public Optional<UtilisateurDTO> login(String email, String password) {
        String e = lowerOrNull(email);
        System.out.println("[" + now() + "][USR-SVC][login] email=" + email + " -> normalized=" + e + ", password=" + maskPwd(password));
        Optional<UtilisateurDTO> out = utilisateurRepository.findByEmailIgnoreCase(e)
                .filter(u -> {
                    boolean match = passwordEncoder.matches(password, u.getPassword());
                    System.out.println("[" + now() + "][USR-SVC][login] password matches? " + match + " for userId=" + u.getId());
                    return match;
                })
                .map(this::convertToDTO);
        System.out.println("[" + now() + "][USR-SVC][login] success? " + out.isPresent());
        return out;
    }

    /* =======================
     *   Création
     * ======================= */

    public Utilisateur createUtilisateur(UtilisateurDTO dto) {
        System.out.println("[" + now() + "][USR-SVC][createUtilisateur] in DTO: " + safeDto(dto));

        if (dto.getNom() == null || dto.getNom().trim().isEmpty()) {
            System.out.println("[" + now() + "][USR-SVC][createUtilisateur] ❌ nom manquant");
            throw new IllegalArgumentException("Le nom est requis.");
        }
        if (dto.getEmail() == null || dto.getEmail().trim().isEmpty()) {
            System.out.println("[" + now() + "][USR-SVC][createUtilisateur] ❌ email manquant");
            throw new IllegalArgumentException("L'email est requis.");
        }
        if (dto.getPassword() == null || dto.getPassword().trim().isEmpty()) {
            System.out.println("[" + now() + "][USR-SVC][createUtilisateur] ❌ mot de passe manquant");
            throw new IllegalArgumentException("Le mot de passe est requis.");
        }

        dto.setEmail(lowerOrNull(dto.getEmail()));
        System.out.println("[" + now() + "][USR-SVC][createUtilisateur] email normalisé=" + dto.getEmail());

        if (existsByEmailIgnoreCase(dto.getEmail())) {
            System.out.println("[" + now() + "][USR-SVC][createUtilisateur] ❌ email déjà existant");
            throw new IllegalArgumentException("Un utilisateur avec cet email existe déjà.");
        }

        // Encode le mot de passe et parse le rôle proprement
        System.out.println("[" + now() + "][USR-SVC][createUtilisateur] encodage mot de passe...");
        dto.setPassword(passwordEncoder.encode(dto.getPassword()));

        if (dto.getRole() == null) {
            dto.setRole(Role.MEMBRE.name());
            System.out.println("[" + now() + "][USR-SVC][createUtilisateur] role null -> set MEMBRE");
        } else {
            System.out.println("[" + now() + "][USR-SVC][createUtilisateur] role reçu='" + dto.getRole() + "'");
        }

        Utilisateur utilisateur = toUtilisateurEntity(dto);
        Utilisateur saved = utilisateurRepository.save(utilisateur);
        System.out.println("[" + now() + "][USR-SVC][createUtilisateur] ✅ saved: " + safeEntity(saved));
        return saved;
    }

    /* =======================
     *   Mise à jour
     * ======================= */

    public void updateUtilisateurFromDTO(Long id, UtilisateurDTO dto) {
        System.out.println("[" + now() + "][USR-SVC][updateUtilisateurFromDTO] id=" + id + ", DTO: " + safeDto(dto));
        utilisateurRepository.findById(id).ifPresent(user -> {
            user.setNom(dto.getNom());
            user.setPrenom(dto.getPrenom());
            user.setEmail(lowerOrNull(dto.getEmail()));
            user.setTelephone(dto.getTelephone());
            user.setAdresse(dto.getAdresse());
            user.setDateNaissance(dto.getDateNaissance());
            user.setRole(parseRoleOrDefault(dto.getRole())); // ✅ String -> Role

            if (dto.getPassword() != null && !dto.getPassword().trim().isEmpty()) {
                System.out.println("[" + now() + "][USR-SVC][updateUtilisateurFromDTO] encodage nouveau mot de passe...");
                user.setPassword(passwordEncoder.encode(dto.getPassword()));
            }

            utilisateurRepository.save(user);
            System.out.println("[" + now() + "][USR-SVC][updateUtilisateurFromDTO] ✅ updated userId=" + user.getId());
        });
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
