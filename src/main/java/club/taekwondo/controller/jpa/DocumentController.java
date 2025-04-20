package club.taekwondo.controller.jpa;

import club.taekwondo.entity.jpa.Document;
import club.taekwondo.service.jpa.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    // Endpoint pour récupérer tous les documents
    @GetMapping
    public List<Document> getAllDocuments() {
        return documentService.getAllDocuments();
    }

    // Endpoint pour récupérer un document par son ID
    @GetMapping("/{id}")
    public ResponseEntity<Document> getDocumentById(@PathVariable Long id) {
        Optional<Document> document = documentService.getDocumentById(id);
        return document.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Endpoint pour créer un nouveau document
    @PostMapping
    public ResponseEntity<Document> createDocument(@RequestBody Document document) {
        Document newDocument = documentService.createDocument(document);
        return new ResponseEntity<>(newDocument, HttpStatus.CREATED);
    }

    // Endpoint pour mettre à jour un document existant
    @PutMapping("/{id}")
    public ResponseEntity<Document> updateDocument(@PathVariable Long id, @RequestBody Document document) {
        Document updatedDocument = documentService.updateDocument(id, document);
        return updatedDocument != null ? ResponseEntity.ok(updatedDocument) : ResponseEntity.notFound().build();
    }

    // Endpoint pour supprimer un document
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id) {
        documentService.deleteDocument(id);
        return ResponseEntity.noContent().build();
    }
}
