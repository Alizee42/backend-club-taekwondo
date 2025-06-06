package club.taekwondo.service.jpa;

import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.repository.jpa.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UtilisateurService {

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<Utilisateur> getAllUtilisateurs() {
        return utilisateurRepository.findAll();
    }

    public Optional<Utilisateur> getUtilisateurByEmail(String email) {
        System.out.println("Recherche de l'utilisateur avec l'email : " + email);
        Optional<Utilisateur> utilisateur = utilisateurRepository.findByEmail(email);
        if (utilisateur.isPresent()) {
            System.out.println("Utilisateur trouvé : " + utilisateur.get().getEmail());
        } else {
            System.out.println("Aucun utilisateur trouvé pour l'email : " + email);
        }
        return utilisateur;
    }

    public Optional<Utilisateur> getUtilisateurById(Long id) {
        System.out.println("Recherche de l'utilisateur avec l'ID : " + id);
        Optional<Utilisateur> utilisateur = utilisateurRepository.findById(id);
        if (utilisateur.isPresent()) {
            System.out.println("Utilisateur trouvé : " + utilisateur.get().getNom());
        } else {
            System.out.println("Aucun utilisateur trouvé avec l'ID : " + id);
        }
        return utilisateur;
    }

    public Utilisateur createUtilisateur(Utilisateur utilisateur) {

        if (utilisateur.getPassword() != null && !utilisateur.getPassword().isEmpty()) {
            utilisateur.setPassword(passwordEncoder.encode(utilisateur.getPassword()));
        } else {
            throw new IllegalArgumentException("Le mot de passe est requis.");
        }
        return utilisateurRepository.save(utilisateur);
    }
    
    public Utilisateur updateUtilisateur(Long id, Utilisateur updatedUser) {
        return utilisateurRepository.findById(id).map(user -> {
            user.setNom(updatedUser.getNom());
            user.setPrenom(updatedUser.getPrenom());
            user.setEmail(updatedUser.getEmail());
            user.setTelephone(updatedUser.getTelephone());
            user.setRole(updatedUser.getRole());

            // Vérifiez si le mot de passe est déjà haché
            if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
                if (!updatedUser.getPassword().startsWith("$2a$")) {
                    System.out.println("Hachage du nouveau mot de passe...");
                    user.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
                } else {
                    System.out.println("Le mot de passe est déjà haché, aucun hachage supplémentaire n'est nécessaire.");
                    user.setPassword(updatedUser.getPassword());
                }
            }

            return utilisateurRepository.save(user);
        }).orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec ID : " + id));
    }

    public void deleteUtilisateur(Long id) {
        utilisateurRepository.deleteById(id);
    }
}