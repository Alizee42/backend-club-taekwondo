package club.taekwondo.service.jpa;

import club.taekwondo.dto.DocumentDTO;
import club.taekwondo.dto.UtilisateurDTO;
import club.taekwondo.entity.jpa.Document;
import club.taekwondo.entity.jpa.Membre;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.repository.jpa.DocumentRepository;
import club.taekwondo.repository.jpa.MembreRepository;
import club.taekwondo.repository.jpa.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    /** Tous les documents (+ mapping utilisateur & membre) */
    public List<DocumentDTO> getAllDocumentsWithUtilisateur() {
        System.out.println("Récupération de tous les documents...");
        List<DocumentDTO> documents = documentRepository.findAllWithUtilisateurAndMembre().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        System.out.println("Documents récupérés: " + documents.size());
        return documents;
    }

    /** Par utilisateur (parent ou adulte) */
    public List<DocumentDTO> getDocumentsByUtilisateurId(Long utilisateurId) {
        System.out.println("Récupération des documents pour l'utilisateur ID: " + utilisateurId);
        validatePositiveId(utilisateurId, "L'ID de l'utilisateur doit être valide et supérieur à 0.");
        List<DocumentDTO> documents = documentRepository.findByUtilisateurId(utilisateurId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        System.out.println("Documents trouvés: " + documents.size());
        return documents;
    }

    /** Par enfant (membre) */
    public List<DocumentDTO> getDocumentsByMembreId(Long membreId) {
        System.out.println("Récupération des documents pour le membre ID: " + membreId);
        validatePositiveId(membreId, "L'ID du membre doit être valide et supérieur à 0.");
        List<DocumentDTO> documents = documentRepository.findByMembreId(membreId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        System.out.println("Documents trouvés: " + documents.size());
        return documents;
    }

    public Optional<DocumentDTO> getDocumentById(Long id) {
        System.out.println("Récupération du document avec l'ID: " + id);
        validatePositiveId(id, "L'ID du document doit être valide et supérieur à 0.");
        return documentRepository.findById(id).map(this::toDTO);
    }

    public List<DocumentDTO> getDocumentsByStatus(String status) {
        System.out.println("Récupération des documents avec le statut: " + status);
        if (status == null || status.isEmpty()) {
            throw new IllegalArgumentException("Le statut ne peut pas être null ou vide.");
        }
        List<DocumentDTO> documents = documentRepository.findByStatus(status).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        System.out.println("Documents trouvés: " + documents.size());
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

        // Compat : on renvoie aussi un UtilisateurDTO si besoin ailleurs
        Utilisateur utilisateur = document.getUtilisateur();
        if (utilisateur != null) {
            UtilisateurDTO utilisateurDTO = new UtilisateurDTO();
            utilisateurDTO.setId(utilisateur.getId());
            utilisateurDTO.setNom(utilisateur.getNom());
            utilisateurDTO.setPrenom(utilisateur.getPrenom());
            utilisateurDTO.setEmail(utilisateur.getEmail());
            utilisateurDTO.setTelephone(utilisateur.getTelephone());
            utilisateurDTO.setRole(utilisateur.getRole() != null ? utilisateur.getRole().name() : null);
            documentDTO.setUtilisateur(utilisateurDTO);
        }

        return documentDTO;
    }

    private Document toEntity(DocumentDTO dto) {
        System.out.println("Conversion du DTO en Document...");
        if (dto == null) {
            throw new IllegalArgumentException("Le document ne peut pas être null.");
        }

        // ===== utilisateurId "effectivement final" =====
        final Long utilisateurId =
            (dto.getUtilisateur() != null && dto.getUtilisateur().getId() != null)
                ? dto.getUtilisateur().getId()
                : dto.getUtilisateurId();

        validatePositiveId(utilisateurId, "Un utilisateur valide est requis pour créer/modifier un document.");

        final Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé: " + utilisateurId));

        // ===== Membre optionnel =====
        Membre membre = null;
        if (dto.getMembreId() != null) {
            final Long membreId = dto.getMembreId();
            validatePositiveId(membreId, "L'ID du membre doit être valide et supérieur à 0.");

            membre = membreRepository.findById(membreId)
                    .orElseThrow(() -> new IllegalArgumentException("Membre non trouvé: " + membreId));

            // Vérification via la relation parent (Membre.getParent())
            if (membre.getParent() == null || membre.getParent().getId() == null
                    || !membre.getParent().getId().equals(utilisateurId)) {
                throw new IllegalArgumentException("Ce membre n'appartient pas à l'utilisateur " + utilisateurId);
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

        System.out.println("Document créé avec ID: " + doc.getId());
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
