package club.taekwondo.service.jpa;

import club.taekwondo.dto.EvenementDTO;
import club.taekwondo.entity.jpa.Evenement;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.repository.jpa.EvenementRepository;
import club.taekwondo.repository.jpa.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EvenementService {

    @Autowired
    private EvenementRepository evenementRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    // 🔹 Récupérer tous les événements
    public List<EvenementDTO> getAllEvenements() {
        return evenementRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // 🔹 Récupérer un événement par ID
    public Optional<EvenementDTO> getEvenementById(Long id) {
        return evenementRepository.findById(id).map(this::convertToDTO);
    }

    // 🔹 Créer un événement
    public EvenementDTO createEvenement(EvenementDTO dto) {
        Evenement evenement = convertToEntity(dto);
        Evenement saved = evenementRepository.save(evenement);
        return convertToDTO(saved);
    }

    // 🔹 Mettre à jour un événement
    public EvenementDTO updateEvenement(Long id, EvenementDTO evenementDTO) {
        Evenement existing = evenementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Événement non trouvé avec l'ID : " + id));

        existing.setTitre(evenementDTO.getTitre());
        existing.setDateDebut(evenementDTO.getDateDebut());
        existing.setDateFin(evenementDTO.getDateFin());
        existing.setLieu(evenementDTO.getLieu());
        existing.setCapacite(evenementDTO.getCapacite());
        existing.setDescription(evenementDTO.getDescription());

        if (evenementDTO.getOrganisateurId() != null) {
            Utilisateur organisateur = utilisateurRepository.findById(evenementDTO.getOrganisateurId())
                    .orElseThrow(() -> new RuntimeException("Organisateur non trouvé avec l'ID : " + evenementDTO.getOrganisateurId()));
            existing.setOrganisateur(organisateur);
        }

        Evenement updated = evenementRepository.save(existing);
        return convertToDTO(updated);
    }

    // 🔹 Supprimer un événement
    public void deleteEvenement(Long id) {
        evenementRepository.deleteById(id);
    }

    // 🔁 Convertisseur : Entity -> DTO
    private EvenementDTO convertToDTO(Evenement evenement) {
        EvenementDTO evenementDTO = new EvenementDTO();
        evenementDTO.setId(evenement.getId());
        evenementDTO.setTitre(evenement.getTitre());
        evenementDTO.setDateDebut(evenement.getDateDebut());
        evenementDTO.setDateFin(evenement.getDateFin());
        evenementDTO.setLieu(evenement.getLieu());
        evenementDTO.setCapacite(evenement.getCapacite());
        evenementDTO.setDescription(evenement.getDescription());
        evenementDTO.setOrganisateurId(evenement.getOrganisateur() != null ? evenement.getOrganisateur().getId() : null);
        return evenementDTO;
    }

    // 🔁 Convertisseur : DTO -> Entity
    private Evenement convertToEntity(EvenementDTO evenementDTO) {
        Evenement evenement = new Evenement();
        evenement.setTitre(evenementDTO.getTitre());
        evenement.setDateDebut(evenementDTO.getDateDebut());
        evenement.setDateFin(evenementDTO.getDateFin());
        evenement.setLieu(evenementDTO.getLieu());
        evenement.setCapacite(evenementDTO.getCapacite());
        evenement.setDescription(evenementDTO.getDescription());

        if (evenementDTO.getOrganisateurId() != null) {
            Utilisateur organisateur = utilisateurRepository.findById(evenementDTO.getOrganisateurId())
                    .orElseThrow(() -> new RuntimeException("Organisateur non trouvé avec l'ID : " + evenementDTO.getOrganisateurId()));
            evenement.setOrganisateur(organisateur);
        }

        return evenement;
    }
}

