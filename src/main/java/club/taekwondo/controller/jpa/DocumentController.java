package club.taekwondo.controller.jpa;

import club.taekwondo.dto.DocumentDTO;
import club.taekwondo.dto.UtilisateurDTO;
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
        return documents.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(documents);
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
            if (typeDocument == null || typeDocument.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Le type de document est requis.");
            }
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body("Le fichier est requis.");
            }
            if (utilisateurId == null || utilisateurId <= 0) {
                return ResponseEntity.badRequest().body("L'ID de l'utilisateur est requis et doit être valide.");
            }

            // 1. Upload du fichier
            String cheminFichier = fileUploadService.uploadFile(file, "documents");

            // 2. Création du DTO
            UtilisateurDTO utilisateurDTO = new UtilisateurDTO();
            utilisateurDTO.setId(utilisateurId);

            DocumentDTO dto = new DocumentDTO();
            dto.setTypeDocument(typeDocument);
            dto.setNomDocument(file.getOriginalFilename());
            dto.setCheminFichier(cheminFichier);
            dto.setStatus("en attente");
            dto.setUtilisateur(utilisateurDTO);

            // 3. Sauvegarde
            DocumentDTO nouveauDocument = documentService.createDocument(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(nouveauDocument);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors du téléversement du fichier : " + e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Erreur de validation : " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur inattendue : " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateDocument(@PathVariable Long id, @RequestBody DocumentDTO dto) {
        try {
            DocumentDTO updated = documentService.updateDocument(id, dto);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDocument(@PathVariable Long id) {
        Optional<DocumentDTO> document = documentService.getDocumentById(id);
        if (document.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Document introuvable.");
        }
        documentService.deleteDocument(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/utilisateur/{utilisateurId}")
    public ResponseEntity<?> getDocumentsByUtilisateur(@PathVariable Long utilisateurId) {
        List<DocumentDTO> documents = documentService.getDocumentsByUtilisateurId(utilisateurId);
        return documents.isEmpty()
                ? ResponseEntity.status(HttpStatus.NO_CONTENT).body("Aucun document trouvé pour cet utilisateur.")
                : ResponseEntity.ok(documents);
    }

    @GetMapping("/en-attente")
    public ResponseEntity<List<DocumentDTO>> getDocumentsEnAttente() {
        List<DocumentDTO> documents = documentService.getDocumentsByStatus("en attente");
        return documents.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(documents);
    }

    @PutMapping("/{id}/valider")
    public ResponseEntity<?> validerDocument(@PathVariable Long id) {
        Optional<DocumentDTO> document = documentService.getDocumentById(id);
        if (document.isPresent()) {
            DocumentDTO dto = document.get();
            dto.setStatus("validé");
            documentService.updateDocument(id, dto);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Document introuvable.");
    }

    @PutMapping("/{id}/refuser")
    public ResponseEntity<?> refuserDocument(@PathVariable Long id) {
        Optional<DocumentDTO> document = documentService.getDocumentById(id);
        if (document.isPresent()) {
            DocumentDTO dto = document.get();
            dto.setStatus("refusé");
            documentService.updateDocument(id, dto);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Document introuvable.");
    }
}

