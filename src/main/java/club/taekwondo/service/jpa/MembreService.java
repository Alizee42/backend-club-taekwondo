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

    // Récupérer tous les membres
    public List<Membre> getAllMembres() {
        return membreRepository.findAll();
    }

    // Récupérer un membre par ID
    public Optional<Membre> getMembreById(Long id) {
        return membreRepository.findById(id);
    }

    // Récupérer un membre par email
    public Optional<Membre> getMembreByEmail(String email) {
        return membreRepository.findByEmail(email);
    }

    // Créer un nouveau membre
    public Membre createMembre(Membre membre) {
        // Encodage du mot de passe ou autres validations si nécessaire
        return membreRepository.save(membre);
    }

    // Mettre à jour un membre existant
    public Membre updateMembre(Long id, Membre membreDetails) {
        Optional<Membre> membreOptional = membreRepository.findById(id);
        if (membreOptional.isPresent()) {
            Membre membre = membreOptional.get();
            membre.setNom(membreDetails.getNom());
            membre.setPrenom(membreDetails.getPrenom());
            membre.setEmail(membreDetails.getEmail());
            membre.setAdresse(membreDetails.getAdresse());
            membre.setCeinture(membreDetails.getCeinture());
            membre.setDateNaissance(membreDetails.getDateNaissance());
            membre.setNumeroLicence(membreDetails.getNumeroLicence());

            return membreRepository.save(membre);
        } else {
            throw new RuntimeException("Membre non trouvé avec l'ID : " + id);
        }
    }

    // Supprimer un membre
    public void deleteMembre(Long id) {
        Optional<Membre> membreOptional = membreRepository.findById(id);
        if (membreOptional.isPresent()) {
            membreRepository.deleteById(id);
        } else {
            throw new RuntimeException("Membre non trouvé avec l'ID : " + id);
        }
    }
}