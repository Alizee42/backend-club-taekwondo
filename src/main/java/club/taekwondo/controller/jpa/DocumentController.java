package club.taekwondo.controller.jpa;

import club.taekwondo.dto.DocumentDTO;
import club.taekwondo.dto.UtilisateurDTO;
import club.taekwondo.entity.jpa.Document;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.service.jpa.DocumentService;
import club.taekwondo.service.jpa.UtilisateurService;
import club.taekwondo.service.common.FileUploadService;
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
    private UtilisateurService utilisateurService;

    @Autowired
    private FileUploadService fileUploadService;

    @GetMapping
    public ResponseEntity<List<DocumentDTO>> getAllDocuments() {
        List<Document> documents = documentService.getAllDocumentsWithUtilisateur();
        if (documents.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        List<DocumentDTO> documentDTOs = documents.stream().map(document -> {
            DocumentDTO dto = new DocumentDTO();
            dto.setId(document.getId());
            dto.setTypeDocument(document.getTypeDocument());
            dto.setNomDocument(document.getNomDocument());
            dto.setCheminFichier(document.getCheminFichier());
            dto.setDateDepot(document.getDateDepot());
            dto.setStatus(document.getStatus());

            Utilisateur utilisateur = document.getUtilisateur();
            if (utilisateur != null) {
                UtilisateurDTO utilisateurDTO = new UtilisateurDTO();
                utilisateurDTO.setId(utilisateur.getId());
                utilisateurDTO.setNom(utilisateur.getNom());
                utilisateurDTO.setPrenom(utilisateur.getPrenom());
                utilisateurDTO.setEmail(utilisateur.getEmail());
                utilisateurDTO.setTelephone(utilisateur.getTelephone());
                dto.setUtilisateur(utilisateurDTO);
            }

            return dto;
        }).toList();

        return ResponseEntity.ok(documentDTOs);
    }
    // Récupérer un document par son ID
    @GetMapping("/{id}")
    public ResponseEntity<Document> getDocumentById(@PathVariable Long id) {
        Optional<Document> document = documentService.getDocumentById(id);
        return document.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Créer un nouveau document
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

            Optional<Utilisateur> utilisateurOptional = utilisateurService.getUtilisateurById(utilisateurId);
            if (utilisateurOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Utilisateur non trouvé avec l'ID : " + utilisateurId);
            }

            String cheminFichier = fileUploadService.uploadFile(file);

            Document document = new Document();
            document.setTypeDocument(typeDocument);
            document.setNomDocument(file.getOriginalFilename());
            document.setCheminFichier(cheminFichier);
            document.setUtilisateur(utilisateurOptional.get());

            Document newDocument = documentService.createDocument(document);
            return new ResponseEntity<>(newDocument, HttpStatus.CREATED);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur lors du téléversement du fichier.");
        }
    }
    
    // Mettre à jour un document existant
    @PutMapping("/{id}")
    public ResponseEntity<Document> updateDocument(@PathVariable Long id, @RequestBody Document document) {
        Document updatedDocument = documentService.updateDocument(id, document);
        return updatedDocument != null ? ResponseEntity.ok(updatedDocument) : ResponseEntity.notFound().build();
    }

    // Supprimer un document
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id) {
        Optional<Document> document = documentService.getDocumentById(id);
        if (document.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        documentService.deleteDocument(id);
        return ResponseEntity.noContent().build();
    }

    // Récupérer les documents d'un utilisateur
    @GetMapping("/utilisateur/{utilisateurId}")
    public ResponseEntity<?> getDocumentsByUtilisateur(@PathVariable Long utilisateurId) {
        Optional<Utilisateur> utilisateur = utilisateurService.getUtilisateurById(utilisateurId);
        if (utilisateur.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Utilisateur non trouvé avec l'ID : " + utilisateurId);
        }

        List<Document> documents = documentService.getDocumentsByUtilisateurId(utilisateurId);
        if (documents.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body("Aucun document trouvé pour cet utilisateur.");
        }

        return ResponseEntity.ok(documents);
    }

    // Récupérer les documents en attente
    @GetMapping("/en-attente")
    public ResponseEntity<List<Document>> getDocumentsEnAttente() {
        List<Document> documents = documentService.getDocumentsByStatus("en attente");
        if (documents.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(documents);
    }

    // Valider un document
    @PutMapping("/{id}/valider")
    public ResponseEntity<Void> validerDocument(@PathVariable Long id) {
        Optional<Document> document = documentService.getDocumentById(id);
        if (document.isPresent()) {
            Document doc = document.get();
            doc.setStatus("validé");
            documentService.updateDocument(doc.getId(), doc);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    // Refuser un document
    @PutMapping("/{id}/refuser")
    public ResponseEntity<Void> refuserDocument(@PathVariable Long id) {
        Optional<Document> document = documentService.getDocumentById(id);
        if (document.isPresent()) {
            Document doc = document.get();
            doc.setStatus("refusé");
            documentService.updateDocument(doc.getId(), doc);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}