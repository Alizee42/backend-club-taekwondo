package club.taekwondo.service.jpa;

import club.taekwondo.dto.DocumentDTO;
import club.taekwondo.dto.UtilisateurDTO;
import club.taekwondo.entity.jpa.Document;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.repository.jpa.DocumentRepository;
import club.taekwondo.repository.jpa.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DocumentService {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    public List<DocumentDTO> getAllDocumentsWithUtilisateur() {
        return documentRepository.findAllWithUtilisateur().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<DocumentDTO> getDocumentsByUtilisateurId(Long utilisateurId) {
        if (utilisateurId == null || utilisateurId <= 0) {
            throw new IllegalArgumentException("L'ID de l'utilisateur doit être valide et supérieur à 0.");
        }
        return documentRepository.findByUtilisateurId(utilisateurId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public Optional<DocumentDTO> getDocumentById(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("L'ID du document doit être valide et supérieur à 0.");
        }
        return documentRepository.findById(id).map(this::toDTO);
    }

    public DocumentDTO createDocument(DocumentDTO documentDTO) {
        Document saved = documentRepository.save(toEntity(documentDTO));
        return toDTO(saved);
    }

    public DocumentDTO updateDocument(Long id, DocumentDTO documentDTO) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("L'ID du document doit être valide et supérieur à 0.");
        }
        if (!documentRepository.existsById(id)) {
            throw new IllegalArgumentException("Le document avec l'ID spécifié n'existe pas.");
        }
        Document document = toEntity(documentDTO);
        document.setId(id);
        return toDTO(documentRepository.save(document));
    }

    public void deleteDocument(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("L'ID du document doit être valide et supérieur à 0.");
        }
        if (!documentRepository.existsById(id)) {
            throw new IllegalArgumentException("Le document avec l'ID spécifié n'existe pas.");
        }
        documentRepository.deleteById(id);
    }

    public List<DocumentDTO> getDocumentsByStatus(String status) {
        if (status == null || status.isEmpty()) {
            throw new IllegalArgumentException("Le statut ne peut pas être null ou vide.");
        }
        return documentRepository.findByStatus(status).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private DocumentDTO toDTO(Document document) {
        DocumentDTO documentDTO = new DocumentDTO();
        documentDTO.setId(document.getId());
        documentDTO.setTypeDocument(document.getTypeDocument());
        documentDTO.setNomDocument(document.getNomDocument());
        documentDTO.setCheminFichier(document.getCheminFichier());
        documentDTO.setDateDepot(document.getDateDepot());
        documentDTO.setStatus(document.getStatus());

        Utilisateur utilisateur = document.getUtilisateur();
        if (utilisateur != null) {
            UtilisateurDTO utilisateurDTO = new UtilisateurDTO();
            utilisateurDTO.setId(utilisateur.getId());
            utilisateurDTO.setNom(utilisateur.getNom());
            utilisateurDTO.setPrenom(utilisateur.getPrenom());
            utilisateurDTO.setEmail(utilisateur.getEmail());
            utilisateurDTO.setTelephone(utilisateur.getTelephone());
            utilisateurDTO.setRole(utilisateur.getRole());
        }
        return documentDTO;
    }

    private Document toEntity(DocumentDTO dto) {
        if (dto == null || dto.getUtilisateur() == null || dto.getUtilisateur().getId() == null) {
            throw new IllegalArgumentException("Un utilisateur valide est requis pour créer un document.");
        }

        Utilisateur utilisateur = utilisateurRepository.findById(dto.getUtilisateur().getId())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé."));

        Document doc = new Document();
        doc.setId(dto.getId());
        doc.setTypeDocument(dto.getTypeDocument());
        doc.setNomDocument(dto.getNomDocument());
        doc.setCheminFichier(dto.getCheminFichier());
        doc.setDateDepot(dto.getDateDepot());
        doc.setStatus(dto.getStatus());
        doc.setUtilisateur(utilisateur);
        return doc;
    }
}
