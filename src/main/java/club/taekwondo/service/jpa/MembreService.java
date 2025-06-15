package club.taekwondo.service.jpa;

import club.taekwondo.dto.MembreDTO;
import club.taekwondo.entity.jpa.Membre;
import club.taekwondo.repository.jpa.MembreRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MembreService {

    private final MembreRepository membreRepository;

    public MembreService(MembreRepository membreRepository) {
        this.membreRepository = membreRepository;
    }

    // 🔹 Récupérer tous les membres (DTO)
    
    public List<MembreDTO> getAllMembres() {
        return membreRepository.findAll()
                .stream()
                .map(this::toMembreDTO)
                .collect(Collectors.toList());
    }

    // 🔹 Récupérer un membre par ID (DTO)
    
    public Optional<MembreDTO> getMembreById(Long id) {
        return membreRepository.findById(id)
                .map(this::toMembreDTO);
    }

    // 🔹 Récupérer un membre par email (DTO)
    
    public Optional<MembreDTO> getMembreByEmail(String email) {
        return membreRepository.findByEmail(email)
                .map(this::toMembreDTO);
    }

    // 🔹 Créer un membre depuis un DTO
    
    public MembreDTO createMembre(MembreDTO membreDTO) {
        Membre membre = fromMembreDTO(membreDTO);
        return toMembreDTO(membreRepository.save(membre));
    }

    // 🔹 Mettre à jour un membre existant
    
    public MembreDTO updateMembre(Long id, MembreDTO membreDTO) {
        return membreRepository.findById(id).map(membre -> {
            membre.setNom(membreDTO.getNom());
            membre.setPrenom(membreDTO.getPrenom());
            membre.setEmail(membreDTO.getEmail());
            membre.setTelephone(membreDTO.getTelephone());
            membre.setAdresse(membreDTO.getAdresse());
            membre.setDateNaissance(membreDTO.getDateNaissance());
            membre.setNumeroLicence(membreDTO.getNumeroLicence());
            membre.setCeinture(membreDTO.getCeinture());
            membre.setRole(membreDTO.getRole());

            return toMembreDTO(membreRepository.save(membre));
        }).orElseThrow(() -> new RuntimeException("Membre non trouvé avec l'ID : " + id));
    }

    // 🔹 Supprimer un membre
    public void deleteMembre(Long id) {
        if (!membreRepository.existsById(id)) {
            throw new RuntimeException("Membre non trouvé avec l'ID : " + id);
        }
        membreRepository.deleteById(id);
    }

    //  Membre → MembreDTO
    public MembreDTO toMembreDTO(Membre membre) {
        MembreDTO membreDTO = new MembreDTO();
        membreDTO.setId(membre.getId());
        membreDTO.setNom(membre.getNom());
        membreDTO.setPrenom(membre.getPrenom());
        membreDTO.setEmail(membre.getEmail());
        membreDTO.setTelephone(membre.getTelephone());
        membreDTO.setAdresse(membre.getAdresse());
        membreDTO.setDateNaissance(membre.getDateNaissance());
        membreDTO.setNumeroLicence(membre.getNumeroLicence());
        membreDTO.setCeinture(membre.getCeinture());
        membreDTO.setRole(membre.getRole());
        return membreDTO;
    }

    //  MembreDTO → Membre
    public Membre fromMembreDTO(MembreDTO membreDTO) {
        Membre membre = new Membre();
        membre.setNom(membreDTO.getNom());
        membre.setPrenom(membreDTO.getPrenom());
        membre.setEmail(membreDTO.getEmail());
        membre.setTelephone(membreDTO.getTelephone());
        membre.setAdresse(membreDTO.getAdresse());
        membre.setDateNaissance(membreDTO.getDateNaissance());
        membre.setNumeroLicence(membreDTO.getNumeroLicence());
        membre.setCeinture(membreDTO.getCeinture());
        membre.setRole(membreDTO.getRole());
        return membre;
    }
}
