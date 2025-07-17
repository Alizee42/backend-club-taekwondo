package club.taekwondo.service.jpa;

import club.taekwondo.dto.PaiementDTO;
import club.taekwondo.dto.UtilisateurDTO;
import club.taekwondo.dto.UtilisateurPaiementDTO;
import club.taekwondo.entity.jpa.Paiement;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.repository.jpa.UtilisateurRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;

    public UtilisateurService(UtilisateurRepository utilisateurRepository, PasswordEncoder passwordEncoder) {
        this.utilisateurRepository = utilisateurRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // 🔹 Récupérer tous les utilisateurs
    public List<UtilisateurDTO> getAllUtilisateurs() {
        List<UtilisateurDTO> result = new ArrayList<>();
        for (Utilisateur u : utilisateurRepository.findAll()) {
            result.add(toUtilisateurDTO(u));
        }
        return result;
    }

    // 🔹 Récupérer un utilisateur par email
    public Optional<UtilisateurDTO> getUtilisateurByEmail(String email) {
        return utilisateurRepository.findByEmail(email).map(this::toUtilisateurDTO);
    }

    // 🔹 Login par email + mot de passe
    public Optional<UtilisateurDTO> login(String email, String password) {
        return utilisateurRepository.findByEmail(email)
                .filter(u -> passwordEncoder.matches(password, u.getPassword()))
                .map(this::toUtilisateurDTO);
    }

    // 🔹 Récupération DTO et entité par ID ou email
    public Optional<UtilisateurDTO> getUtilisateurById(Long id) {
        return utilisateurRepository.findById(id).map(this::toUtilisateurDTO);
    }

    public Optional<Utilisateur> getUtilisateurEntityById(Long id) {
        return utilisateurRepository.findById(id);
    }

    public Optional<Utilisateur> getUtilisateurEntityByEmail(String email) {
        return utilisateurRepository.findByEmail(email);
    }

    public Optional<Utilisateur> getByEmail(String email) {
        return utilisateurRepository.findByEmail(email);
    }

    // 🔹 Trouver par nom + prénom
    public Optional<Utilisateur> findByNomPrenom(String nom, String prenom) {
        return utilisateurRepository.findByNomIgnoreCaseAndPrenomIgnoreCase(nom, prenom);
    }

    // 🔹 Sauvegarde directe
    public Utilisateur save(Utilisateur utilisateur) {
        return utilisateurRepository.save(utilisateur);
    }

    // 🔹 Création à partir d'un DTO
    public Utilisateur createUtilisateur(UtilisateurDTO dto) {
        if (dto.getPassword() == null || dto.getPassword().isEmpty()) {
            throw new IllegalArgumentException("Le mot de passe est requis.");
        }

        if (utilisateurRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Un utilisateur avec cet email existe déjà.");
        }

        dto.setPassword(passwordEncoder.encode(dto.getPassword()));
        return utilisateurRepository.save(toUtilisateurEntity(dto));
    }

    // 🔹 Mise à jour des infos d’un utilisateur depuis un DTO
    public void updateUtilisateurFromDTO(Long id, UtilisateurDTO dto) {
        utilisateurRepository.findById(id).ifPresent(user -> {
            user.setNom(dto.getNom());
            user.setPrenom(dto.getPrenom());
            user.setEmail(dto.getEmail());
            user.setTelephone(dto.getTelephone());
            user.setRole(dto.getRole());
            user.setAdresse(dto.getAdresse());
            user.setDateNaissance(dto.getDateNaissance());

            if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
                user.setPassword(passwordEncoder.encode(dto.getPassword()));
            }

            utilisateurRepository.save(user);
        });
    }

    // 🔹 Suppression
    public void deleteUtilisateur(Long id) {
        utilisateurRepository.deleteById(id);
    }

    // 🔹 Liste des utilisateurs avec leurs paiements (utilisé côté admin)
    public List<UtilisateurPaiementDTO> getAllWithPaiements() {
        List<UtilisateurPaiementDTO> result = new ArrayList<>();
        for (Utilisateur u : utilisateurRepository.findAll()) {
            UtilisateurPaiementDTO dto = new UtilisateurPaiementDTO();
            dto.setId(u.getId());
            dto.setNom(u.getNom());
            dto.setPrenom(u.getPrenom());
            dto.setEmail(u.getEmail());
            dto.setPaiements(toPaiementDTOList(u.getPaiements()));
            result.add(dto);
        }
        return result;
    }

    // ==== MAPPERS ====

    private UtilisateurDTO toUtilisateurDTO(Utilisateur utilisateur) {
        UtilisateurDTO dto = new UtilisateurDTO();
        dto.setId(utilisateur.getId());
        dto.setNom(utilisateur.getNom());
        dto.setPrenom(utilisateur.getPrenom());
        dto.setDateNaissance(utilisateur.getDateNaissance());
        dto.setAdresse(utilisateur.getAdresse());
        dto.setEmail(utilisateur.getEmail());
        dto.setTelephone(utilisateur.getTelephone());
        dto.setRole(utilisateur.getRole());
        return dto;
    }

    private Utilisateur toUtilisateurEntity(UtilisateurDTO dto) {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setNom(dto.getNom());
        utilisateur.setPrenom(dto.getPrenom());
        utilisateur.setEmail(dto.getEmail());
        utilisateur.setDateNaissance(dto.getDateNaissance());
        utilisateur.setAdresse(dto.getAdresse());
        utilisateur.setTelephone(dto.getTelephone());
        utilisateur.setRole(dto.getRole());
        utilisateur.setPassword(dto.getPassword()); // déjà encodé
        return utilisateur;
    }

    private PaiementDTO toPaiementDTO(Paiement paiement) {
        PaiementDTO dto = new PaiementDTO();
        dto.setId(paiement.getId());
        dto.setDatePaiement(paiement.getDatePaiement());
        return dto;
    }

    private List<PaiementDTO> toPaiementDTOList(List<Paiement> paiements) {
        List<PaiementDTO> result = new ArrayList<>();
        for (Paiement p : paiements) {
            result.add(toPaiementDTO(p));
        }
        return result;
    }
}

