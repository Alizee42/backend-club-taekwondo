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

    // ========== CONVERSION ==========
    public UtilisateurDTO convertToDTO(Utilisateur utilisateur) {
        UtilisateurDTO dto = new UtilisateurDTO();
        dto.setId(utilisateur.getId());
        dto.setNom(utilisateur.getNom());
        dto.setPrenom(utilisateur.getPrenom());
        dto.setDateNaissance(utilisateur.getDateNaissance());
        dto.setAdresse(utilisateur.getAdresse());
        dto.setEmail(utilisateur.getEmail());
        dto.setTelephone(utilisateur.getTelephone());
        dto.setRole(utilisateur.getRole() != null ? utilisateur.getRole().name() : null); // ✅
        return dto;
    }

    private Utilisateur toUtilisateurEntity(UtilisateurDTO dto) {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setNom(dto.getNom());
        utilisateur.setPrenom(dto.getPrenom());
        utilisateur.setEmail(dto.getEmail().toLowerCase());
        utilisateur.setDateNaissance(dto.getDateNaissance());
        utilisateur.setAdresse(dto.getAdresse());
        utilisateur.setTelephone(dto.getTelephone());
        utilisateur.setRole(dto.getRole());
        utilisateur.setPassword(dto.getPassword());
        return utilisateur;
    }

    // ========== LECTURE ==========
    public List<UtilisateurDTO> getAllUtilisateurs() {
        List<UtilisateurDTO> result = new ArrayList<>();
        for (Utilisateur u : utilisateurRepository.findAll()) {
            result.add(convertToDTO(u));
        }
        return result;
    }

    public Optional<UtilisateurDTO> getUtilisateurByEmail(String email) {
        return utilisateurRepository.findByEmail(email.toLowerCase()).map(this::convertToDTO);
    }

    public Optional<Utilisateur> getUtilisateurEntityById(Long id) {
        return utilisateurRepository.findById(id);
    }

    public Optional<Utilisateur> getUtilisateurEntityByEmail(String email) {
        return utilisateurRepository.findByEmail(email.toLowerCase());
    }

    public Optional<Utilisateur> findByNomPrenom(String nom, String prenom) {
        return utilisateurRepository.findByNomIgnoreCaseAndPrenomIgnoreCase(nom, prenom);
    }
    public Optional<Utilisateur> findByEmail(String email) {
        return utilisateurRepository.findByEmail(email);
    }

    // ========== AUTHENTIFICATION ==========
    public Optional<UtilisateurDTO> login(String email, String password) {
        if (email != null) {
            email = email.toLowerCase();
        }
        return utilisateurRepository.findByEmail(email)
                .filter(u -> passwordEncoder.matches(password, u.getPassword()))
                .map(this::convertToDTO);
    }

    // ========== CRÉATION ==========
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

        dto.setEmail(dto.getEmail().toLowerCase());

        if (utilisateurRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Un utilisateur avec cet email existe déjà.");
        }

        if (dto.getRole() == null) {
        	dto.setRole(Role.MEMBRE.name()); // ✅ Corrigé : convertit enum → String
        }

        dto.setPassword(passwordEncoder.encode(dto.getPassword()));
        Utilisateur utilisateur = toUtilisateurEntity(dto);
        return utilisateurRepository.save(utilisateur);
    }

    // ========== MISE À JOUR ==========
    public void updateUtilisateurFromDTO(Long id, UtilisateurDTO dto) {
        utilisateurRepository.findById(id).ifPresent(user -> {
            user.setNom(dto.getNom());
            user.setPrenom(dto.getPrenom());
            user.setEmail(dto.getEmail().toLowerCase());
            user.setTelephone(dto.getTelephone());
            user.setAdresse(dto.getAdresse());
            user.setDateNaissance(dto.getDateNaissance());
            user.setRole(dto.getRole());

            if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
                user.setPassword(passwordEncoder.encode(dto.getPassword()));
            }

            utilisateurRepository.save(user);
        });
    }

    // ========== SUPPRESSION ==========
    public void deleteUtilisateur(Long id) {
        utilisateurRepository.deleteById(id);
    }

    // ========== PAIEMENTS ==========
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

    // ========== SAUVEGARDE ==========
    public Utilisateur save(Utilisateur utilisateur) {
        utilisateur.setEmail(utilisateur.getEmail().toLowerCase());
        return utilisateurRepository.save(utilisateur);
    }
}

