package club.taekwondo.service.jpa;

import club.taekwondo.dto.DocumentDTO;
import club.taekwondo.dto.MembreDTO;
import club.taekwondo.dto.UtilisateurDTO;
import club.taekwondo.entity.jpa.Document;
import club.taekwondo.entity.jpa.Membre;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.repository.jpa.DocumentRepository;
import club.taekwondo.repository.jpa.MembreRepository;
import club.taekwondo.repository.jpa.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DocumentService {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private MembreRepository membreRepository;

    // ====================== READ ======================

    /** Tous les documents (+ mapping utilisateur & membre/enfant) */
    @Transactional(readOnly = true)
    public List<DocumentDTO> getAllDocumentsWithUtilisateur() {
        System.out.println("Récupération de tous les documents...");
        List<DocumentDTO> documents = documentRepository
                .findAllWithUtilisateurAndMembre() // ⚠️ doit charger utilisateur + membre (enfant)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        System.out.println("Documents récupérés: " + documents.size());
        return documents;
    }

    /** Par utilisateur (parent ou adulte) */
    @Transactional(readOnly = true)
    public List<DocumentDTO> getDocumentsByUtilisateurId(Long utilisateurId) {
        System.out.println("Récupération des documents pour l'utilisateur ID: " + utilisateurId);
        validatePositiveId(utilisateurId, "L'ID de l'utilisateur doit être valide et supérieur à 0.");
        List<DocumentDTO> documents = documentRepository
                .findByUtilisateurIdWithFetch(utilisateurId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        System.out.println("Documents trouvés: " + documents.size());
        return documents;
    }

    /** Par enfant (membre) */
    @Transactional(readOnly = true)
    public List<DocumentDTO> getDocumentsByMembreId(Long membreId) {
        System.out.println("Récupération des documents pour le membre ID: " + membreId);
        validatePositiveId(membreId, "L'ID du membre doit être valide et supérieur à 0.");
        List<DocumentDTO> documents = documentRepository
                .findByMembreIdWithFetch(membreId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        System.out.println("Documents trouvés: " + documents.size());
        return documents;
    }

    @Transactional(readOnly = true)
    public Optional<DocumentDTO> getDocumentById(Long id) {
        System.out.println("Récupération du document avec l'ID: " + id);
        validatePositiveId(id, "L'ID du document doit être valide et supérieur à 0.");
        return documentRepository.findByIdWithFetch(id).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public List<DocumentDTO> getDocumentsByStatus(String status) {
        System.out.println("Récupération des documents avec le statut: " + status);
        if (status == null || status.isEmpty()) {
            throw new IllegalArgumentException("Le statut ne peut pas être null ou vide.");
        }
        List<DocumentDTO> documents = documentRepository
                .findByStatusWithFetch(status)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        System.out.println("Documents trouvés: " + documents.size());
        return documents;
    }

    /** Par club (admin ou staff) */
    @Transactional(readOnly = true)
    public List<DocumentDTO> getDocumentsByClubId(Long clubId) {
        System.out.println("Récupération des documents pour le club ID: " + clubId);
        validatePositiveId(clubId, "L'ID du club doit être valide et supérieur à 0.");
        List<DocumentDTO> documents = documentRepository.findAllWithUtilisateurAndMembre()
                .stream()
                .filter(doc -> doc.getUtilisateur() != null && doc.getUtilisateur().getClub() != null && doc.getUtilisateur().getClub().getId().equals(clubId))
                .map(this::toDTO)
                .collect(Collectors.toList());
        System.out.println("Documents trouvés pour le club: " + documents.size());
        return documents;
    }

    // ====================== WRITE ======================

    /** Création depuis un DTO (le contrôleur a déjà stocké le fichier et rempli cheminFichier) */
    public DocumentDTO createDocument(DocumentDTO documentDTO) {
        System.out.println("Création d'un nouveau document pour l'utilisateur ID: " + documentDTO.getUtilisateurId());
        Document saved = documentRepository.save(toEntity(documentDTO));
        System.out.println("Document créé avec succès, ID: " + saved.getId());
        return toDTO(saved);
    }

    public DocumentDTO updateDocument(Long id, DocumentDTO documentDTO) {
        System.out.println("Mise à jour du document avec l'ID: " + id);
        validatePositiveId(id, "L'ID du document doit être valide et supérieur à 0.");
        if (!documentRepository.existsById(id)) {
            throw new IllegalArgumentException("Le document avec l'ID spécifié n'existe pas.");
        }
        Document document = toEntity(documentDTO);
        document.setId(id);
        DocumentDTO updated = toDTO(documentRepository.save(document));
        System.out.println("Document mis à jour, ID: " + updated.getId());
        return updated;
    }

    public void deleteDocument(Long id) {
        System.out.println("Suppression du document avec l'ID: " + id);
        validatePositiveId(id, "L'ID du document doit être valide et supérieur à 0.");
        if (!documentRepository.existsById(id)) {
            throw new IllegalArgumentException("Le document avec l'ID spécifié n'existe pas.");
        }
        documentRepository.deleteById(id);
        System.out.println("Document supprimé avec succès, ID: " + id);
    }

    // ====================== MAPPERS ======================

    private DocumentDTO toDTO(Document document) {
        System.out.println("Conversion du document ID: " + document.getId() + " en DTO...");
        DocumentDTO documentDTO = new DocumentDTO();
        documentDTO.setId(document.getId());
        documentDTO.setTypeDocument(document.getTypeDocument());
        documentDTO.setNomDocument(document.getNomDocument());
        documentDTO.setCheminFichier(document.getCheminFichier());
        documentDTO.setDateDepot(document.getDateDepot());
        documentDTO.setStatus(document.getStatus());

        // commentaire côté front = description côté entity
        documentDTO.setCommentaire(document.getDescription());

        // IDs à plat (pratiques côté front)
        documentDTO.setUtilisateurId(document.getUtilisateur() != null ? document.getUtilisateur().getId() : null);
        documentDTO.setMembreId(document.getMembre() != null ? document.getMembre().getId() : null);

        // ✅ Nouvel alias enfantId (lisible par le front) = membreId
        if (document.getMembre() != null) {
            documentDTO.setEnfantId(document.getMembre().getId());
        } else {
            documentDTO.setEnfantId(null);
        }

        // Utilisateur (léger)
        Utilisateur utilisateur = document.getUtilisateur();
        if (utilisateur != null) {
            UtilisateurDTO utilisateurDTO = new UtilisateurDTO();
            utilisateurDTO.setId(utilisateur.getId());
            utilisateurDTO.setNom(utilisateur.getNom());
            utilisateurDTO.setPrenom(utilisateur.getPrenom());
            utilisateurDTO.setEmail(utilisateur.getEmail());
            utilisateurDTO.setTelephone(utilisateur.getTelephone());
            utilisateurDTO.setRole(utilisateur.getRole() != null ? utilisateur.getRole().name() : null);
            // ✅ propager le clubId pour les vues Super Admin
            if (utilisateur.getClub() != null) {
                utilisateurDTO.setClubId(utilisateur.getClub().getId());
            }
            documentDTO.setUtilisateur(utilisateurDTO);
        }

        // ✅ Enfant (Membre → MembreDTO) pour afficher prénom/nom/licence
        Membre membre = document.getMembre();
        if (membre != null) {
            MembreDTO enfantDTO = new MembreDTO();
            enfantDTO.setId(membre.getId());
            enfantDTO.setPrenom(membre.getPrenom());
            enfantDTO.setNom(membre.getNom());
            enfantDTO.setNumeroLicence(membre.getNumeroLicence());
            // champs optionnels si présents dans MembreDTO :
            enfantDTO.setDateNaissance(membre.getDateNaissance());
            enfantDTO.setEstAdulte(membre.isEstAdulte());
            if (membre.getParent() != null) {
                enfantDTO.setUtilisateurId(membre.getParent().getId());
            }
            documentDTO.setEnfant(enfantDTO);
        } else {
            documentDTO.setEnfant(null);
        }

        return documentDTO;
    }

    private Document toEntity(DocumentDTO dto) {
        System.out.println("Conversion du DTO en Document...");
        if (dto == null) {
            throw new IllegalArgumentException("Le document ne peut pas être null.");
        }

        // ===== utilisateurId effectif (final) =====
        final Long uid =
            (dto.getUtilisateur() != null && dto.getUtilisateur().getId() != null)
                ? dto.getUtilisateur().getId()
                : dto.getUtilisateurId();

        validatePositiveId(uid, "Un utilisateur valide est requis pour créer/modifier un document.");
        final Utilisateur utilisateur = utilisateurRepository.findById(uid)
            .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé: " + uid));

        // ===== Membre (enfant) optionnel : accepte membreId, enfantId, ou dto.enfant.id =====
        final Long resolvedMembreId =
            (dto.getEnfant() != null && dto.getEnfant().getId() != null) ? dto.getEnfant().getId()
            : (dto.getEnfantId() != null ? dto.getEnfantId()
            :  dto.getMembreId());

        Membre membre = null;
        if (resolvedMembreId != null) {
            validatePositiveId(resolvedMembreId, "L'ID du membre doit être valide et supérieur à 0.");
            final Long mid = resolvedMembreId; // ✅ final pour la lambda
            membre = membreRepository.findById(mid)
                .orElseThrow(() -> new IllegalArgumentException("Membre non trouvé: " + mid));

            // Vérifie l'appartenance à l'utilisateur parent
            if (membre.getParent() == null || membre.getParent().getId() == null
                    || !membre.getParent().getId().equals(uid)) {
                throw new IllegalArgumentException("Ce membre (id=" + mid + ") n'appartient pas à l'utilisateur " + uid);
            }
        }

        Document doc = new Document();
        doc.setId(dto.getId());
        doc.setTypeDocument(dto.getTypeDocument());
        doc.setNomDocument(dto.getNomDocument());
        doc.setCheminFichier(dto.getCheminFichier());

        // dateDepot : garde la date existante sinon now()
        doc.setDateDepot(dto.getDateDepot() != null ? dto.getDateDepot() : LocalDateTime.now());

        // statut : garde celui reçu sinon "en attente"
        doc.setStatus(dto.getStatus() != null && !dto.getStatus().isBlank() ? dto.getStatus() : "en attente");

        // commentaire -> description
        doc.setDescription(dto.getCommentaire());

        doc.setUtilisateur(utilisateur);
        doc.setMembre(membre);

        System.out.println("Document prêt pour persistance; id=" + doc.getId()
                + ", utilisateurId=" + (utilisateur != null ? utilisateur.getId() : null)
                + ", membreId=" + (membre != null ? membre.getId() : null));
        return doc;
    }

    // ====================== UTILS ======================

    private void validatePositiveId(Long id, String message) {
        if (id == null || id <= 0) {
            System.out.println("Validation échouée pour l'ID: " + id);
            throw new IllegalArgumentException(message);
        }
    }
}
