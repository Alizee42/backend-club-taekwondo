package club.taekwondo.controller.jpa;

import club.taekwondo.dto.DocumentDTO;
import club.taekwondo.service.common.FileUploadService;
import club.taekwondo.service.jpa.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = "*")
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private FileUploadService fileUploadService;

    @GetMapping
    public ResponseEntity<List<DocumentDTO>> getAllDocuments() {
        List<DocumentDTO> documents = documentService.getAllDocumentsWithUtilisateur();
        if (documents.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentDTO> getDocumentById(@PathVariable Long id) {
        Optional<DocumentDTO> document = documentService.getDocumentById(id);
        return document.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createDocument(@RequestParam("typeDocument") String typeDocument,
                                            @RequestParam("file") MultipartFile file,
                                            @RequestParam("utilisateurId") Long utilisateurId) {
        try {
            if (typeDocument == null || typeDocument.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Le type de document est requis.");
            }
            if (file == null || file.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Le fichier est requis.");
            }

            String cheminFichier = fileUploadService.uploadFile(file);

            DocumentDTO dto = new DocumentDTO();
            dto.setTypeDocument(typeDocument);
            dto.setNomDocument(file.getOriginalFilename());
            dto.setCheminFichier(cheminFichier);
            dto.setStatus("en attente");
            dto.setUtilisateur(new club.taekwondo.dto.UtilisateurDTO());
            dto.getUtilisateur().setId(utilisateurId);

            DocumentDTO newDocument = documentService.createDocument(dto);
            return new ResponseEntity<>(newDocument, HttpStatus.CREATED);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur lors du téléversement du fichier.");
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<DocumentDTO> updateDocument(@PathVariable Long id, @RequestBody DocumentDTO dto) {
        DocumentDTO updatedDocument = documentService.updateDocument(id, dto);
        return updatedDocument != null ? ResponseEntity.ok(updatedDocument) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id) {
        Optional<DocumentDTO> document = documentService.getDocumentById(id);
        if (document.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        documentService.deleteDocument(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/utilisateur/{utilisateurId}")
    public ResponseEntity<?> getDocumentsByUtilisateur(@PathVariable Long utilisateurId) {
        List<DocumentDTO> documents = documentService.getDocumentsByUtilisateurId(utilisateurId);
        if (documents.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body("Aucun document trouvé pour cet utilisateur.");
        }
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/en-attente")
    public ResponseEntity<List<DocumentDTO>> getDocumentsEnAttente() {
        List<DocumentDTO> documents = documentService.getDocumentsByStatus("en attente");
        if (documents.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(documents);
    }

    @PutMapping("/{id}/valider")
    public ResponseEntity<Void> validerDocument(@PathVariable Long id) {
        Optional<DocumentDTO> document = documentService.getDocumentById(id);
        if (document.isPresent()) {
            DocumentDTO dto = document.get();
            dto.setStatus("validé");
            documentService.updateDocument(id, dto);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}/refuser")
    public ResponseEntity<Void> refuserDocument(@PathVariable Long id) {
        Optional<DocumentDTO> document = documentService.getDocumentById(id);
        if (document.isPresent()) {
            DocumentDTO dto = document.get();
            dto.setStatus("refusé");
            documentService.updateDocument(id, dto);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}
