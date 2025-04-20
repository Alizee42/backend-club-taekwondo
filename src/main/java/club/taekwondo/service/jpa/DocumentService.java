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

    // Méthode pour obtenir tous les documents
    public List<Document> getAllDocuments() {
        return documentRepository.findAll();
    }

    // Méthode pour obtenir un document par son ID
    public Optional<Document> getDocumentById(Long id) {
        return documentRepository.findById(id);
    }

    // Méthode pour créer un nouveau document
    public Document createDocument(Document document) {
        return documentRepository.save(document);
    }

    // Méthode pour mettre à jour un document existant
    public Document updateDocument(Long id, Document document) {
        if (documentRepository.existsById(id)) {
            document.setId(id);
            return documentRepository.save(document);
        }
        return null; // Ou gérer l'exception
    }

    // Méthode pour supprimer un document
    public void deleteDocument(Long id) {
        documentRepository.deleteById(id);
    }
}
