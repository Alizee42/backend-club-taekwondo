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
        long dejaInscrits = inscriptionRepository.countByEvenementIdAndStatutNot(evenementId, StatutInscription.ANNULEE);
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

        // Debug : Log des informations de l'entité
        System.out.println("=== DEBUG INSCRIPTION ===");
        System.out.println("Inscription ID: " + entity.getId());
        System.out.println("Membre: " + entity.getMembre());
        
        // Infos Membre avec gestion des erreurs
        if (entity.getMembre() != null) {
            try {
                System.out.println("Membre ID: " + entity.getMembre().getId());
                System.out.println("Membre nom: " + entity.getMembre().getNom());
                System.out.println("Membre prénom: " + entity.getMembre().getPrenom());
                
                dto.setMembreId(entity.getMembre().getId());
                dto.setMembreNom(entity.getMembre().getNom() != null ? entity.getMembre().getNom() : "Nom non disponible");
                dto.setMembrePrenom(entity.getMembre().getPrenom() != null ? entity.getMembre().getPrenom() : "Prénom non disponible");
                
                // Récupérer l'email depuis compteUtilisateur
                if (entity.getMembre().getCompteUtilisateur() != null) {
                    System.out.println("Compte utilisateur trouvé: " + entity.getMembre().getCompteUtilisateur().getEmail());
                    dto.setMembreEmail(entity.getMembre().getCompteUtilisateur().getEmail() != null ? 
                        entity.getMembre().getCompteUtilisateur().getEmail() : "Email non disponible");
                } else {
                    System.out.println("Aucun compte utilisateur trouvé pour ce membre");
                    // Essayer de récupérer l'email depuis le parent si c'est un enfant
                    if (entity.getMembre().getParent() != null && entity.getMembre().getParent().getEmail() != null) {
                        dto.setMembreEmail(entity.getMembre().getParent().getEmail() + " (parent)");
                    } else {
                        dto.setMembreEmail("Email non disponible");
                    }
                }
                
            } catch (Exception e) {
                // En cas d'erreur de lazy loading
                dto.setMembreNom("Nom non disponible");
                dto.setMembrePrenom("Prénom non disponible");
                dto.setMembreEmail("Email non disponible");
                System.err.println("Erreur lors du chargement des données du membre: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("MEMBRE EST NULL !");
            dto.setMembreNom("Nom non disponible");
            dto.setMembrePrenom("Prénom non disponible");
            dto.setMembreEmail("Email non disponible");
        }

        System.out.println("=== FIN DEBUG ===");
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
}
