package club.taekwondo.service.jpa;

import club.taekwondo.dto.InscriptionEvenementDTO;
import club.taekwondo.entity.jpa.Evenement;
import club.taekwondo.entity.jpa.InscriptionEvenement;
import club.taekwondo.entity.jpa.Membre;
import club.taekwondo.enums.StatutInscription;
import club.taekwondo.repository.jpa.EvenementRepository;
import club.taekwondo.repository.jpa.InscriptionEvenementRepository;
import club.taekwondo.repository.jpa.MembreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class InscriptionEvenementService {

    @Autowired
    private InscriptionEvenementRepository inscriptionRepository;

    @Autowired
    private MembreRepository membreRepository;

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
            return inscriptionRepository.findByEvenementIdAndStatutWithMembre(evenementId, StatutInscription.valueOf(statut))
                    .stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } else {
            return inscriptionRepository.findByEvenementIdWithMembre(evenementId)
                    .stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        }
    }

    // 🔹 Récupérer une inscription par ID
    public Optional<InscriptionEvenementDTO> getInscriptionById(Long id) {
        return inscriptionRepository.findById(id).map(this::convertToDTO);
    }

    // 🔹 Créer une nouvelle inscription pour plusieurs enfants
    public List<InscriptionEvenementDTO> inscrireMembres(Long evenementId, List<Long> membresIds, String commentaire) {
        Evenement evenement = evenementRepository.findById(evenementId)
                .orElseThrow(() -> new RuntimeException("Événement non trouvé"));

        // Vérifier la capacité
        long dejaInscrits = inscriptionRepository.countActiveByEvenementId(evenementId);
        if (dejaInscrits + membresIds.size() > evenement.getCapacite()) {
            throw new RuntimeException("L'événement est complet.");
        }

        List<InscriptionEvenementDTO> resultats = new ArrayList<>();

        for (Long membreId : membresIds) {
            Membre membre = membreRepository.findById(membreId)
                    .orElseThrow(() -> new RuntimeException("Membre non trouvé : " + membreId));

            // Vérifier si ce membre est déjà inscrit
            boolean dejaInscrit = inscriptionRepository
                    .existsByMembreIdAndEvenementIdAndStatutNot(
                            membreId,
                            evenementId,
                            StatutInscription.ANNULEE
                    );
            if (dejaInscrit) {
                throw new RuntimeException("L’enfant " + membre.getPrenom() + " est déjà inscrit.");
            }

            // Créer l’inscription
            InscriptionEvenement inscription = new InscriptionEvenement();
            inscription.setMembre(membre);
            inscription.setEvenement(evenement);
            inscription.setDateInscription(LocalDateTime.now());
            inscription.setStatut(StatutInscription.EN_ATTENTE);
            inscription.setPresence(null);
            inscription.setCommentaire(commentaire);

            resultats.add(convertToDTO(inscriptionRepository.save(inscription)));
        }

        return resultats;
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
        dto.setEvenementId(entity.getEvenement().getId());
        dto.setDateInscription(entity.getDateInscription());
        dto.setStatut(entity.getStatut());
        dto.setPresence(entity.getPresence());
        dto.setCommentaire(entity.getCommentaire());

        Membre membre = entity.getMembre();
        if (membre != null) {
            dto.setMembreId(membre.getId());
            dto.setMembreNom(membre.getNom() != null ? membre.getNom() : "Nom non disponible");
            dto.setMembrePrenom(membre.getPrenom() != null ? membre.getPrenom() : "Prenom non disponible");

            try {
                if (membre.getCompteUtilisateur() != null && membre.getCompteUtilisateur().getEmail() != null) {
                    dto.setMembreEmail(membre.getCompteUtilisateur().getEmail());
                } else if (membre.getParent() != null && membre.getParent().getEmail() != null) {
                    dto.setMembreEmail(membre.getParent().getEmail() + " (parent)");
                } else {
                    dto.setMembreEmail("Email non disponible");
                }
            } catch (Exception e) {
                dto.setMembreEmail("Email non disponible");
            }
        } else {
            dto.setMembreNom("Nom non disponible");
            dto.setMembrePrenom("Prenom non disponible");
            dto.setMembreEmail("Email non disponible");
        }

        dto.setEvenementTitre(entity.getEvenement().getTitre());
        return dto;
    }
    // ?? Convertir DTO → Entity
    private InscriptionEvenement convertToEntity(InscriptionEvenementDTO dto) {
        InscriptionEvenement entity = new InscriptionEvenement();
        entity.setId(dto.getId());
        entity.setDateInscription(dto.getDateInscription() != null ? dto.getDateInscription() : LocalDateTime.now());
        entity.setStatut(dto.getStatut() != null ? dto.getStatut() : StatutInscription.EN_ATTENTE);
        entity.setPresence(dto.getPresence());
        entity.setCommentaire(dto.getCommentaire());

        Membre membre = membreRepository.findById(dto.getMembreId())
                .orElseThrow(() -> new RuntimeException("Membre non trouvé"));
        Evenement evenement = evenementRepository.findById(dto.getEvenementId())
                .orElseThrow(() -> new RuntimeException("Événement non trouvé"));

        entity.setMembre(membre);
        entity.setEvenement(evenement);

        return entity;
    }

    // 🔹 Récupérer les inscriptions des enfants d'un parent connecté
    public List<InscriptionEvenementDTO> getInscriptionsByParent(Long parentId) {
        return inscriptionRepository.findByParentIdWithMembreAndEvenement(parentId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<InscriptionEvenementDTO> getInscriptionsByMembreId(Long membreId) {
        return inscriptionRepository.findActiveByMembreIdWithEvenement(membreId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // 🔹 Récupérer toutes les inscriptions d'un club
    public List<InscriptionEvenementDTO> getInscriptionsByClubId(Long clubId) {
        return inscriptionRepository.findByMembre_Club_Id(clubId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
}

