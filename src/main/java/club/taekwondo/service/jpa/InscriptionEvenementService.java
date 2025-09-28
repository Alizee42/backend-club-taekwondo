package club.taekwondo.service.jpa;

import club.taekwondo.dto.InscriptionEvenementDTO;
import club.taekwondo.entity.jpa.Evenement;
import club.taekwondo.entity.jpa.InscriptionEvenement;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.enums.StatutInscription;
import club.taekwondo.repository.jpa.EvenementRepository;
import club.taekwondo.repository.jpa.InscriptionEvenementRepository;
import club.taekwondo.repository.jpa.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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

    // 🔹 Récupérer les inscriptions par événement et statut
    public List<InscriptionEvenementDTO> getInscriptionsByEvenementAndStatut(Long evenementId, String statut) {
        if (statut != null) {
            return inscriptionRepository.findByEvenementIdAndStatut(evenementId, StatutInscription.valueOf(statut))
                    .stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } else {
            return inscriptionRepository.findByEvenementId(evenementId)
                    .stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        }
    }

    // 🔹 Récupérer une inscription par ID
    public Optional<InscriptionEvenementDTO> getInscriptionById(Long id) {
        return inscriptionRepository.findById(id).map(this::convertToDTO);
    }

    // 🔹 Créer une nouvelle inscription
    public InscriptionEvenementDTO inscrireMembre(InscriptionEvenementDTO dto) {
        Long utilisateurId = dto.getUtilisateurId();
        Long evenementId = dto.getEvenementId();

        if (utilisateurId == null || evenementId == null) {
            throw new RuntimeException("L'identifiant de l'utilisateur et de l'événement sont requis.");
        }

        Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        Evenement evenement = evenementRepository.findById(evenementId)
                .orElseThrow(() -> new RuntimeException("Événement non trouvé"));

        // 🔍 Vérifier la capacité
        long count = inscriptionRepository.countByEvenementId(evenement.getId());
        if (count >= evenement.getCapacite()) {
            throw new RuntimeException("L'événement est complet.");
        }

        // 🔍 Vérifier si déjà inscrit sauf si ANNULEE
        boolean dejaInscrit = inscriptionRepository
                .existsByUtilisateurIdAndEvenementIdAndStatutNot(
                        utilisateurId,
                        evenementId,
                        StatutInscription.ANNULEE
                );
        if (dejaInscrit) {
            throw new RuntimeException("Vous êtes déjà inscrit à cet événement.");
        }

        // ✅ Créer l’inscription
        InscriptionEvenement inscription = new InscriptionEvenement();
        inscription.setUtilisateur(utilisateur);
        inscription.setEvenement(evenement);
        inscription.setDateInscription(LocalDateTime.now());
        inscription.setStatut(StatutInscription.EN_ATTENTE); // Par défaut
        inscription.setPresence(null);
        inscription.setCommentaire(dto.getCommentaire());

        return convertToDTO(inscriptionRepository.save(inscription));
    }

    // 🔹 Mettre à jour une inscription
    public InscriptionEvenementDTO updateInscription(Long id, InscriptionEvenementDTO dto) {
        if (!inscriptionRepository.existsById(id)) {
            throw new RuntimeException("Inscription non trouvée avec l'ID : " + id);
        }

        InscriptionEvenement inscription = convertToEntity(dto);
        inscription.setId(id);
        return convertToDTO(inscriptionRepository.save(inscription));
    }

    // 🔹 Mettre à jour uniquement le statut d'une inscription
    public void updateStatutInscription(Long id, String statut) {
        InscriptionEvenement inscription = inscriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Inscription non trouvée avec l'ID : " + id));

        try {
            StatutInscription statutEnum = StatutInscription.valueOf(statut);
            inscription.setStatut(statutEnum);
            inscriptionRepository.save(inscription);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Statut invalide : " + statut);
        }
    }

    // 🔹 Annuler une inscription (soft delete)
    public void annulerInscription(Long id) {
        InscriptionEvenement inscription = inscriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Inscription non trouvée avec l'ID : " + id));

        inscription.setStatut(StatutInscription.ANNULEE);
        inscriptionRepository.save(inscription);
    }

    // 🔁 Convertir Entity → DTO
    private InscriptionEvenementDTO convertToDTO(InscriptionEvenement entity) {
        InscriptionEvenementDTO dto = new InscriptionEvenementDTO();
        dto.setId(entity.getId());
        dto.setUtilisateurId(entity.getUtilisateur().getId());
        dto.setEvenementId(entity.getEvenement().getId());
        dto.setDateInscription(entity.getDateInscription());
        dto.setStatut(entity.getStatut());
        dto.setPresence(entity.getPresence());
        dto.setCommentaire(entity.getCommentaire());

        // Ajout des infos détaillées
        dto.setUtilisateurNom(entity.getUtilisateur().getNom());
        dto.setUtilisateurPrenom(entity.getUtilisateur().getPrenom());
        dto.setUtilisateurEmail(entity.getUtilisateur().getEmail());
        dto.setEvenementTitre(entity.getEvenement().getTitre());

        return dto;
    }

    // 🔁 Convertir DTO → Entity
    private InscriptionEvenement convertToEntity(InscriptionEvenementDTO dto) {
        InscriptionEvenement entity = new InscriptionEvenement();
        entity.setId(dto.getId());
        entity.setDateInscription(dto.getDateInscription() != null ? dto.getDateInscription() : LocalDateTime.now());
        entity.setStatut(dto.getStatut() != null ? dto.getStatut() : StatutInscription.EN_ATTENTE);
        entity.setPresence(dto.getPresence());
        entity.setCommentaire(dto.getCommentaire());

        Utilisateur utilisateur = utilisateurRepository.findById(dto.getUtilisateurId())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        Evenement evenement = evenementRepository.findById(dto.getEvenementId())
                .orElseThrow(() -> new RuntimeException("Événement non trouvé"));

        entity.setUtilisateur(utilisateur);
        entity.setEvenement(evenement);

        return entity;
    }
}


