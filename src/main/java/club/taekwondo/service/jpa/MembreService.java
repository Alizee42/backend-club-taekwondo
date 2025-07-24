package club.taekwondo.service.jpa;

import club.taekwondo.dto.MembreDTO;
import club.taekwondo.entity.jpa.Membre;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.repository.jpa.MembreRepository;
import club.taekwondo.repository.jpa.UtilisateurRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MembreService {

    private final MembreRepository membreRepository;
    private final UtilisateurRepository utilisateurRepository;

    public MembreService(MembreRepository membreRepository, UtilisateurRepository utilisateurRepository) {
        this.membreRepository = membreRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    // 🔹 Récupérer tous les membres
    public List<MembreDTO> getAllMembres() {
        return membreRepository.findAll()
                .stream()
                .map(this::toMembreDTO)
                .collect(Collectors.toList());
    }

    // 🔹 Récupérer un membre par ID
    public Optional<MembreDTO> getMembreById(Long id) {
        return membreRepository.findById(id)
                .map(this::toMembreDTO);
    }

    // 🔹 Récupérer un membre par email utilisateur
    public Optional<MembreDTO> getMembreByEmail(String email) {
        return membreRepository.findByCompteUtilisateur_Email(email) 
                .map(this::toMembreDTO);
    }

    // 🔹 Créer un membre sans rattachement explicite
    public MembreDTO createMembre(MembreDTO membreDTO) {
        return createMembre(membreDTO, membreDTO.getUtilisateurId());
    }

    // 🔹 Créer un membre avec rattachement explicite
    public MembreDTO createMembre(MembreDTO membreDTO, Long utilisateurId) {
        Membre membre = fromMembreDTO(membreDTO);

        Long idParent = utilisateurId != null ? utilisateurId : membreDTO.getUtilisateurId();
        if (idParent != null) {
            Utilisateur utilisateur = utilisateurRepository.findById(idParent)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID : " + idParent));
            membre.setCompteUtilisateur(utilisateur);
        } else {
            throw new RuntimeException("Impossible de créer un membre sans utilisateur associé.");
        }

        return toMembreDTO(membreRepository.save(membre));
    }

    // 🔹 Mettre à jour un membre
    public MembreDTO updateMembre(Long id, MembreDTO membreDTO) {
        return membreRepository.findById(id).map(membre -> {
            membre.setNom(membreDTO.getNom());
            membre.setPrenom(membreDTO.getPrenom());
            membre.setDateNaissance(membreDTO.getDateNaissance());
            membre.setNumeroLicence(membreDTO.getNumeroLicence());
            membre.setCeinture(membreDTO.getCeinture());
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

    // 🔁 Membre → DTO
    public MembreDTO toMembreDTO(Membre membre) {
        MembreDTO dto = new MembreDTO();
        dto.setId(membre.getId());
        dto.setNom(membre.getNom());
        dto.setPrenom(membre.getPrenom());
        dto.setDateNaissance(membre.getDateNaissance());
        dto.setNumeroLicence(membre.getNumeroLicence());
        dto.setCeinture(membre.getCeinture());
        if (membre.getCompteUtilisateur() != null) {
            dto.setUtilisateurId(membre.getCompteUtilisateur().getId());
        }
        return dto;
    }

    // 🔁 DTO → Membre
    public Membre fromMembreDTO(MembreDTO dto) {
        Membre membre = new Membre();
        membre.setNom(dto.getNom());
        membre.setPrenom(dto.getPrenom());
        membre.setDateNaissance(dto.getDateNaissance());
        membre.setNumeroLicence(dto.getNumeroLicence());
        membre.setCeinture(dto.getCeinture());
        return membre;
    }
}