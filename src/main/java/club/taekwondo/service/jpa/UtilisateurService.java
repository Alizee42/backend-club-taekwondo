package club.taekwondo.service.jpa;

import club.taekwondo.dto.UtilisateurDTO;
import club.taekwondo.dto.UtilisateurPaiementDTO;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.enums.Role;
import club.taekwondo.repository.jpa.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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
        UtilisateurDTO dto = new UtilisateurDTO();
        dto.setId(utilisateur.getId());
        dto.setNom(utilisateur.getNom());
        dto.setPrenom(utilisateur.getPrenom());
        dto.setDateNaissance(utilisateur.getDateNaissance());
        dto.setAdresse(utilisateur.getAdresse());
        dto.setEmail(utilisateur.getEmail());
        dto.setTelephone(utilisateur.getTelephone());
        dto.setRole(utilisateur.getRole() != null ? utilisateur.getRole().name() : null); // String coté DTO
        return dto;
    }

    private Utilisateur toUtilisateurEntity(UtilisateurDTO dto) {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setNom(dto.getNom());
        utilisateur.setPrenom(dto.getPrenom());
        utilisateur.setEmail(lowerOrNull(dto.getEmail())); // peut être null
        utilisateur.setDateNaissance(dto.getDateNaissance());
        utilisateur.setAdresse(dto.getAdresse());
        utilisateur.setTelephone(dto.getTelephone());
        utilisateur.setRole(parseRoleOrDefault(dto.getRole())); // ✅ convertit String -> Role
        utilisateur.setPassword(dto.getPassword()); // déjà encodé si createUtilisateur()
        return utilisateur;
    }

    /* =======================
     *   Lecture
     * ======================= */

    public List<UtilisateurDTO> getAllUtilisateurs() {
        List<UtilisateurDTO> result = new ArrayList<>();
        for (Utilisateur u : utilisateurRepository.findAll()) {
            result.add(convertToDTO(u));
        }
        return result;
    }

    public Optional<UtilisateurDTO> getUtilisateurByEmail(String email) {
        String e = lowerOrNull(email);
        return e == null ? Optional.empty()
                : utilisateurRepository.findByEmailIgnoreCase(e).map(this::convertToDTO);
    }

    public Optional<Utilisateur> getUtilisateurEntityById(Long id) {
        return utilisateurRepository.findById(id);
    }

    public Optional<Utilisateur> getUtilisateurEntityByEmail(String email) {
        String e = lowerOrNull(email);
        return e == null ? Optional.empty() : utilisateurRepository.findByEmailIgnoreCase(e);
    }

    public Optional<Utilisateur> findByNomPrenom(String nom, String prenom) {
        if (nom == null || prenom == null) return Optional.empty();
        return utilisateurRepository.findByNomIgnoreCaseAndPrenomIgnoreCase(nom.trim(), prenom.trim());
    }

    /** Exposés pour le PaiementService (évite l’erreur “undefined method”) */
    public Optional<Utilisateur> findByEmailIgnoreCase(String email) {
        String e = lowerOrNull(email);
        return e == null ? Optional.empty() : utilisateurRepository.findByEmailIgnoreCase(e);
    }

    public boolean existsByEmailIgnoreCase(String email) {
        String e = lowerOrNull(email);
        return e != null && utilisateurRepository.existsByEmailIgnoreCase(e);
    }

    // Historique si du code appelle encore cette version
    public Optional<Utilisateur> findByEmail(String email) {
        String e = lowerOrNull(email);
        return e == null ? Optional.empty() : utilisateurRepository.findByEmailIgnoreCase(e);
    }

    /* =======================
     *   Authentification
     * ======================= */

    public Optional<UtilisateurDTO> login(String email, String password) {
        String e = lowerOrNull(email);
        return utilisateurRepository.findByEmailIgnoreCase(e)
                .filter(u -> passwordEncoder.matches(password, u.getPassword()))
                .map(this::convertToDTO);
    }

    /* =======================
     *   Création
     * ======================= */

    public Utilisateur createUtilisateur(UtilisateurDTO dto) {
        if (dto.getNom() == null || dto.getNom().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom est requis.");
        }
        if (dto.getEmail() == null || dto.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("L'email est requis.");
        }
        if (dto.getPassword() == null || dto.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("Le mot de passe est requis.");
        }

        dto.setEmail(lowerOrNull(dto.getEmail()));
        if (existsByEmailIgnoreCase(dto.getEmail())) {
            throw new IllegalArgumentException("Un utilisateur avec cet email existe déjà.");
        }

        // Encode le mot de passe et parse le rôle proprement
        dto.setPassword(passwordEncoder.encode(dto.getPassword()));
        if (dto.getRole() == null) {
            dto.setRole(Role.MEMBRE.name());
        }

        Utilisateur utilisateur = toUtilisateurEntity(dto);
        return utilisateurRepository.save(utilisateur);
    }

    /* =======================
     *   Mise à jour
     * ======================= */

    public void updateUtilisateurFromDTO(Long id, UtilisateurDTO dto) {
        utilisateurRepository.findById(id).ifPresent(user -> {
            user.setNom(dto.getNom());
            user.setPrenom(dto.getPrenom());
            user.setEmail(lowerOrNull(dto.getEmail()));
            user.setTelephone(dto.getTelephone());
            user.setAdresse(dto.getAdresse());
            user.setDateNaissance(dto.getDateNaissance());
            user.setRole(parseRoleOrDefault(dto.getRole())); // ✅ String -> Role

            if (dto.getPassword() != null && !dto.getPassword().trim().isEmpty()) {
                user.setPassword(passwordEncoder.encode(dto.getPassword()));
            }

            utilisateurRepository.save(user);
        });
    }

    /* =======================
     *   Suppression
     * ======================= */

    public void deleteUtilisateur(Long id) {
        utilisateurRepository.deleteById(id);
    }

    /* =======================
     *   Paiements (DTO léger)
     * ======================= */

    public List<UtilisateurPaiementDTO> getAllWithPaiements() {
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
        return result;
    }

    /* =======================
     *   Sauvegarde bas niveau
     * ======================= */

    public Utilisateur save(Utilisateur utilisateur) {
        utilisateur.setEmail(lowerOrNull(utilisateur.getEmail()));
        return utilisateurRepository.save(utilisateur);
    }
}