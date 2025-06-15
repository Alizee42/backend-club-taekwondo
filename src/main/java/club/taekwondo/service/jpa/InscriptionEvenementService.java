package club.taekwondo.service.jpa;

import club.taekwondo.dto.InscriptionEvenementDTO;
import club.taekwondo.entity.jpa.Evenement;
import club.taekwondo.entity.jpa.InscriptionEvenement;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.repository.jpa.EvenementRepository;
import club.taekwondo.repository.jpa.InscriptionEvenementRepository;
import club.taekwondo.repository.jpa.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class InscriptionEvenementService {

    @Autowired
    private InscriptionEvenementRepository inscriptionRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private EvenementRepository evenementRepository;

    // 🔹 Récupérer toutes les inscriptions
    public List<InscriptionEvenementDTO> getAllInscriptions() {
        return inscriptionRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // 🔹 Récupérer une inscription par ID
    public Optional<InscriptionEvenementDTO> getInscriptionById(Long id) {
        return inscriptionRepository.findById(id).map(this::convertToDTO);
    }

    // 🔹 Créer une nouvelle inscription
    public InscriptionEvenementDTO inscrireMembre(InscriptionEvenementDTO inscriptionEvenement) {
        InscriptionEvenement inscription = convertToEntity(inscriptionEvenement);
        if (inscription.getDateInscription() == null) {
            inscription.setDateInscription(LocalDate.now());
        }
        return convertToDTO(inscriptionRepository.save(inscription));
    }

    // 🔹 Mettre à jour une inscription
    public InscriptionEvenementDTO updateInscription(Long id, InscriptionEvenementDTO inscriptionEvenement) {
        if (!inscriptionRepository.existsById(id)) {
            throw new RuntimeException("Inscription non trouvée avec l'ID : " + id);
        }
        InscriptionEvenement entity = convertToEntity(inscriptionEvenement);
        entity.setId(id);
        return convertToDTO(inscriptionRepository.save(entity));
    }

    // 🔹 Supprimer une inscription
    public void annulerInscription(Long id) {
        inscriptionRepository.deleteById(id);
    }

    // 🔁 Convertir Entity → DTO
    private InscriptionEvenementDTO convertToDTO(InscriptionEvenement inscriptionEvenement) {
        InscriptionEvenementDTO inscriptionEvenementDTO = new InscriptionEvenementDTO();
        inscriptionEvenementDTO.setId(inscriptionEvenement.getId());
        inscriptionEvenementDTO.setDateInscription(inscriptionEvenement.getDateInscription());
        inscriptionEvenementDTO.setUtilisateurId(inscriptionEvenement.getUtilisateur().getId());
        inscriptionEvenementDTO.setEvenementId(inscriptionEvenement.getEvenement().getId());
        inscriptionEvenementDTO.setStatut(inscriptionEvenement.getStatut());
        inscriptionEvenementDTO.setPresence(inscriptionEvenement.getPresence());
        inscriptionEvenementDTO.setCommentaire(inscriptionEvenement.getCommentaire());
        return inscriptionEvenementDTO;
    }

    // 🔁 Convertir DTO → Entity
    private InscriptionEvenement convertToEntity(InscriptionEvenementDTO inscriptionEvenementDTO) {
        InscriptionEvenement inscriptionEvenement = new InscriptionEvenement();
        inscriptionEvenement.setDateInscription(inscriptionEvenementDTO.getDateInscription());
        inscriptionEvenement.setStatut(inscriptionEvenementDTO.getStatut());
        inscriptionEvenement.setPresence(inscriptionEvenementDTO.getPresence());
        inscriptionEvenement.setCommentaire(inscriptionEvenementDTO.getCommentaire());

        Utilisateur utilisateur = utilisateurRepository.findById(inscriptionEvenementDTO.getUtilisateurId())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        Evenement evenement = evenementRepository.findById(inscriptionEvenementDTO.getEvenementId())
                .orElseThrow(() -> new RuntimeException("Événement non trouvé"));

        inscriptionEvenement.setUtilisateur(utilisateur);
        inscriptionEvenement.setEvenement(evenement);
        return inscriptionEvenement;
    }
}
