package club.taekwondo.service.jpa;

import club.taekwondo.entity.jpa.Membre;
import club.taekwondo.repository.jpa.MembreRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MembreService {

    private final MembreRepository membreRepository;

    public MembreService(MembreRepository membreRepository) {
        this.membreRepository = membreRepository;
    }

    public List<Membre> getAllMembres() {
        return membreRepository.findAll();
    }

    public Optional<Membre> getMembreById(Long id) {
        return membreRepository.findById(id);
    }

    public Membre createMembre(Membre membre) {
        // Ici tu peux ajouter une logique de validation si besoin (ex: email déjà utilisé)
        return membreRepository.save(membre);
    }

    public Membre saveMembre(Membre membre) {
        return membreRepository.save(membre);
    }

    public Membre updateMembre(Long id, Membre updatedMembre) {
        return membreRepository.findById(id).map(membre -> {
            membre.setNom(updatedMembre.getNom());
            membre.setPrenom(updatedMembre.getPrenom());
            membre.setEmail(updatedMembre.getEmail());
            membre.setPassword(updatedMembre.getPassword());
            membre.setTelephone(updatedMembre.getTelephone());
            membre.setRole(updatedMembre.getRole());
            membre.setCeinture(updatedMembre.getCeinture());
            membre.setDateNaissance(updatedMembre.getDateNaissance());
            membre.setAdresse(updatedMembre.getAdresse());
            membre.setStatutSante(updatedMembre.getStatutSante());
            return membreRepository.save(membre);
        }).orElseThrow(() -> new RuntimeException("Membre non trouvé avec ID : " + id));
    }

    public void deleteMembre(Long id) {
        membreRepository.deleteById(id);
    }
}

