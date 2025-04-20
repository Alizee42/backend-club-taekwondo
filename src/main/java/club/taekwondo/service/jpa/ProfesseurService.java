package club.taekwondo.service.jpa;

import club.taekwondo.entity.jpa.Professeur;
import club.taekwondo.repository.jpa.ProfesseurRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProfesseurService {

    private final ProfesseurRepository professeurRepository;

    public ProfesseurService(ProfesseurRepository professeurRepository) {
        this.professeurRepository = professeurRepository;
    }

    public List<Professeur> getAllProfesseurs() {
        return professeurRepository.findAll();
    }

    public Optional<Professeur> getProfesseurById(Long id) {
        return professeurRepository.findById(id);
    }

    public Professeur createProfesseur(Professeur professeur) {
        return professeurRepository.save(professeur);
    }

    public Professeur updateProfesseur(Long id, Professeur updatedProf) {
        return professeurRepository.findById(id).map(prof -> {
            prof.setNom(updatedProf.getNom());
            prof.setPrenom(updatedProf.getPrenom());
            prof.setEmail(updatedProf.getEmail());
            prof.setPassword(updatedProf.getPassword());
            prof.setTelephone(updatedProf.getTelephone());
            prof.setRole(updatedProf.getRole());
            prof.setCeinture(updatedProf.getCeinture());
            prof.setDateNaissance(updatedProf.getDateNaissance());
            prof.setAdresse(updatedProf.getAdresse());
            prof.setStatutSante(updatedProf.getStatutSante());
            prof.setSpecialite(updatedProf.getSpecialite());
            prof.setDescription(updatedProf.getDescription());
            return professeurRepository.save(prof);
        }).orElseThrow(() -> new RuntimeException("Professeur non trouvé avec ID : " + id));
    }

    public void deleteProfesseur(Long id) {
        professeurRepository.deleteById(id);
    }

    public List<Professeur> getProfesseursBySpecialite(String specialite) {
        return professeurRepository.findBySpecialite(specialite);
    }
}
