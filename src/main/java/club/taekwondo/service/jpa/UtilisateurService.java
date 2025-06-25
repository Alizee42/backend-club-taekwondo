package club.taekwondo.service.jpa;

import club.taekwondo.dto.PaiementDTO;
import club.taekwondo.dto.UtilisateurDTO;
import club.taekwondo.dto.UtilisateurPaiementDTO;
import club.taekwondo.entity.jpa.Paiement;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.repository.jpa.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UtilisateurService {

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<UtilisateurDTO> getAllUtilisateurs() {
        List<UtilisateurDTO> utilisateursDTO = new ArrayList<>();
        List<Utilisateur> utilisateurs = utilisateurRepository.findAll();
        for (Utilisateur utilisateur : utilisateurs) {
            utilisateursDTO.add(toUtilisateurDTO(utilisateur));
        }
        return utilisateursDTO;
    }

    public Optional<UtilisateurDTO> getUtilisateurByEmail(String email) {
        System.out.println("Recherche de l'utilisateur avec l'email : " + email);
        Optional<Utilisateur> utilisateurOptional = utilisateurRepository.findByEmail(email);
        return utilisateurOptional.map(this::toUtilisateurDTO);
    }

    public Optional<UtilisateurDTO> login(String email, String password) {
        System.out.println("Recherche de l'utilisateur avec l'email : " + email);
        Optional<Utilisateur> utilisateurOptional = utilisateurRepository.findByEmail(email);

        if (utilisateurOptional.isPresent()) {
            Utilisateur utilisateur = utilisateurOptional.get();
            boolean passwordMatches = passwordEncoder.matches(password, utilisateur.getPassword());
            System.out.println("Résultat de la comparaison des mots de passe : " + passwordMatches);

            if (!passwordMatches) {
                throw new RuntimeException("Email ou mot de passe incorrect");
            }

            return Optional.of(toUtilisateurDTO(utilisateur));
        }
        return Optional.empty();
    }

    public Optional<UtilisateurDTO> getUtilisateurById(Long id) {
        System.out.println("Recherche de l'utilisateur avec l'ID : " + id);
        Optional<Utilisateur> utilisateur = utilisateurRepository.findById(id);
        utilisateur.ifPresent(u -> System.out.println("Utilisateur trouvé : " + u.getNom()));
        return utilisateur.map(this::toUtilisateurDTO);
    }

    public Optional<Utilisateur> getUtilisateurEntityById(Long id) {
        return utilisateurRepository.findById(id);
    }

    public Utilisateur save(Utilisateur utilisateur) {
        return utilisateurRepository.save(utilisateur);
    }

    public Optional<Utilisateur> getByEmail(String email) {
        return utilisateurRepository.findByEmail(email);
    }

    public Utilisateur createUtilisateur(UtilisateurDTO utilisateur) {
        if (utilisateur.getPassword() != null && !utilisateur.getPassword().isEmpty()) {
            utilisateur.setPassword(passwordEncoder.encode(utilisateur.getPassword()));
        } else {
            throw new IllegalArgumentException("Le mot de passe est requis.");
        }
        return utilisateurRepository.save(toUtilisateurEntity(utilisateur));
    }

    public void updateUtilisateurFromDTO(Long id, UtilisateurDTO utilisateurDTO) {
        utilisateurRepository.findById(id).ifPresent(user -> {
            user.setNom(utilisateurDTO.getNom());
            user.setPrenom(utilisateurDTO.getPrenom());
            user.setEmail(utilisateurDTO.getEmail());
            user.setTelephone(utilisateurDTO.getTelephone());
            user.setRole(utilisateurDTO.getRole());

            if (utilisateurDTO.getAdresse() != null) {
                user.setAdresse(utilisateurDTO.getAdresse());
            }

            if (utilisateurDTO.getDateNaissance() != null) {
                user.setDateNaissance(utilisateurDTO.getDateNaissance());
            }

            if (utilisateurDTO.getPassword() != null && !utilisateurDTO.getPassword().isEmpty()) {
                user.setPassword(passwordEncoder.encode(utilisateurDTO.getPassword()));
            }

            utilisateurRepository.save(user);
        });
    }
    public List<UtilisateurPaiementDTO> getAllWithPaiements() {
        List<UtilisateurPaiementDTO> utilisateursPaiementsDTO = new ArrayList<>();
        List<Utilisateur> utilisateurs = utilisateurRepository.findAll();

        for (Utilisateur utilisateur : utilisateurs) {
            UtilisateurPaiementDTO utilisateurPaiementDTO = new UtilisateurPaiementDTO();
            utilisateurPaiementDTO.setId(utilisateur.getId());
            utilisateurPaiementDTO.setNom(utilisateur.getNom());
            utilisateurPaiementDTO.setPrenom(utilisateur.getPrenom());
            utilisateurPaiementDTO.setEmail(utilisateur.getEmail());
            utilisateurPaiementDTO.setPaiements(toPaiementDTOList(utilisateur.getPaiements())); // Conversion ici
            utilisateursPaiementsDTO.add(utilisateurPaiementDTO);
        }

        return utilisateursPaiementsDTO;
    }
    
    public void deleteUtilisateur(Long id) {
        utilisateurRepository.deleteById(id);
    }

    public Optional<Utilisateur> getUtilisateurEntityByEmail(String email) {
        return utilisateurRepository.findByEmail(email);
    }
    public Optional<Utilisateur> findByNomPrenom(String nom, String prenom) {
        return utilisateurRepository.findByNomIgnoreCaseAndPrenomIgnoreCase(nom, prenom);
    }


    private UtilisateurDTO toUtilisateurDTO(Utilisateur utilisateur) {
        UtilisateurDTO utilisateurDTO = new UtilisateurDTO();
        utilisateurDTO.setId(utilisateur.getId());
        utilisateurDTO.setNom(utilisateur.getNom());
        utilisateurDTO.setPrenom(utilisateur.getPrenom());
        utilisateurDTO.setDateNaissance(utilisateur.getDateNaissance());
        utilisateurDTO.setAdresse(utilisateur.getAdresse());
        utilisateurDTO.setEmail(utilisateur.getEmail());
        utilisateurDTO.setTelephone(utilisateur.getTelephone());
        utilisateurDTO.setRole(utilisateur.getRole());
        return utilisateurDTO;
    }

    private Utilisateur toUtilisateurEntity(UtilisateurDTO utilisateurDTO) {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setNom(utilisateurDTO.getNom());
        utilisateur.setPrenom(utilisateurDTO.getPrenom());
        utilisateur.setEmail(utilisateurDTO.getEmail());
        utilisateur.setDateNaissance(utilisateurDTO.getDateNaissance());
        utilisateur.setAdresse(utilisateurDTO.getAdresse());
        utilisateur.setTelephone(utilisateurDTO.getTelephone());
        utilisateur.setRole(utilisateurDTO.getRole());
        utilisateur.setPassword(utilisateurDTO.getPassword());
        return utilisateur;
    }
    private PaiementDTO toPaiementDTO(Paiement paiement) {
        PaiementDTO paiementDTO = new PaiementDTO();
        paiementDTO.setId(paiement.getId());
        paiementDTO.setMontant(paiement.getMontant());
        paiementDTO.setDatePaiement(paiement.getDatePaiement());
        return paiementDTO;
    }

    private List<PaiementDTO> toPaiementDTOList(List<Paiement> paiements) {
        List<PaiementDTO> paiementDTOs = new ArrayList<>();
        for (Paiement paiement : paiements) {
            paiementDTOs.add(toPaiementDTO(paiement));
        }
        return paiementDTOs;
    }
}
