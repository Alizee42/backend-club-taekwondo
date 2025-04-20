package club.taekwondo.service.jpa;

import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.repository.jpa.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UtilisateurService {

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    public List<Utilisateur> getAllUtilisateurs() {
        return utilisateurRepository.findAll();
    }

    public Optional<Utilisateur> getUtilisateurById(Long id) {
        return utilisateurRepository.findById(id);
    }

    public Utilisateur createUtilisateur(Utilisateur utilisateur) {
        return utilisateurRepository.save(utilisateur);
    }

    public Utilisateur updateUtilisateur(Long id, Utilisateur updatedUser) {
        return utilisateurRepository.findById(id).map(user -> {
            user.setNom(updatedUser.getNom());
            user.setPrenom(updatedUser.getPrenom());
            user.setEmail(updatedUser.getEmail());
            user.setPassword(updatedUser.getPassword());
            user.setTelephone(updatedUser.getTelephone());
            user.setRole(updatedUser.getRole());
            return utilisateurRepository.save(user);
        }).orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec ID : " + id));
    }

    public void deleteUtilisateur(Long id) {
        utilisateurRepository.deleteById(id);
    }
}

