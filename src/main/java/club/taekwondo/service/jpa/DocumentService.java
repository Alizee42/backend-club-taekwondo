package club.taekwondo.service.jpa;

import club.taekwondo.entity.jpa.Document;
import club.taekwondo.repository.jpa.DocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DocumentService {

    @Autowired
    private DocumentRepository documentRepository;

    // Récupérer tous les documents avec leurs utilisateurs
    public List<Document> getAllDocumentsWithUtilisateur() {
        return documentRepository.findAllWithUtilisateur();
    }

    // Récupérer les documents par utilisateur
    public List<Document> getDocumentsByUtilisateurId(Long utilisateurId) {
        if (utilisateurId == null || utilisateurId <= 0) {
            throw new IllegalArgumentException("L'ID de l'utilisateur doit être valide et supérieur à 0.");
        }
        return documentRepository.findByUtilisateurId(utilisateurId);
    }

    // Récupérer un document par son ID
    public Optional<Document> getDocumentById(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("L'ID du document doit être valide et supérieur à 0.");
        }
        return documentRepository.findById(id);
    }

    // Créer un nouveau document
    public Document createDocument(Document document) {
        if (document == null) {
            throw new IllegalArgumentException("Le document ne peut pas être null.");
        }
        if (document.getUtilisateur() == null || document.getUtilisateur().getId() == null || document.getUtilisateur().getId() <= 0) {
            throw new IllegalArgumentException("Un utilisateur valide est requis pour créer un document.");
        }
        return documentRepository.save(document);
    }

    public Document updateDocument(Long id, Document document) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("L'ID du document doit être valide et supérieur à 0.");
        }
        if (document == null) {
            throw new IllegalArgumentException("Le document ne peut pas être null.");
        }
        if (document.getUtilisateur() == null || document.getUtilisateur().getId() == null || document.getUtilisateur().getId() <= 0) {
            throw new IllegalArgumentException("Un utilisateur valide est requis pour mettre à jour un document.");
        }
        if (!documentRepository.existsById(id)) {
            throw new IllegalArgumentException("Le document avec l'ID spécifié n'existe pas.");
        }
        document.setId(id);
        return documentRepository.save(document);
    }
    
    // Supprimer un document
    public void deleteDocument(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("L'ID du document doit être valide et supérieur à 0.");
        }
        if (!documentRepository.existsById(id)) {
            throw new IllegalArgumentException("Le document avec l'ID spécifié n'existe pas.");
        }
        documentRepository.deleteById(id);
    }

    // Récupérer les documents par statut
    public List<Document> getDocumentsByStatus(String status) {
        if (status == null || status.isEmpty()) {
            throw new IllegalArgumentException("Le statut ne peut pas être null ou vide.");
        }
        return documentRepository.findByStatus(status);
    }
}