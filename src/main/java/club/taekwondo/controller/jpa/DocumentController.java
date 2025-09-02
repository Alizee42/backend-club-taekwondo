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

    // ================== READ ==================

    @GetMapping
    public ResponseEntity<List<DocumentDTO>> getAllDocuments() {
        System.out.println("Récupération de tous les documents...");
        List<DocumentDTO> documents = documentService.getAllDocumentsWithUtilisateur();
        if (documents.isEmpty()) {
            System.out.println("Aucun document trouvé.");
            return ResponseEntity.noContent().build();
        }
        System.out.println("Documents récupérés: " + documents.size());
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentDTO> getDocumentById(@PathVariable Long id) {
        System.out.println("Récupération du document avec ID: " + id);
        Optional<DocumentDTO> document = documentService.getDocumentById(id);
        if (document.isEmpty()) {
            System.out.println("Document introuvable avec l'ID: " + id);
            return ResponseEntity.notFound().build();
        }
        System.out.println("Document trouvé: " + document.get().getNomDocument());
        return ResponseEntity.ok(document.get());
    }

    @GetMapping("/utilisateur/{utilisateurId}")
    public ResponseEntity<?> getDocumentsByUtilisateur(@PathVariable Long utilisateurId) {
        System.out.println("Récupération des documents pour l'utilisateur avec ID: " + utilisateurId);
        List<DocumentDTO> documents = documentService.getDocumentsByUtilisateurId(utilisateurId);
        if (documents.isEmpty()) {
            System.out.println("Aucun document trouvé pour l'utilisateur avec ID: " + utilisateurId);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body("Aucun document trouvé pour cet utilisateur.");
        }
        System.out.println("Documents trouvés pour l'utilisateur: " + documents.size());
        return ResponseEntity.ok(documents);
    }

    // ✅ NOUVEAU : Listing par enfant (membre)
    @GetMapping("/membre/{membreId}")
    public ResponseEntity<?> getDocumentsByMembre(@PathVariable Long membreId) {
        System.out.println("Récupération des documents pour le membre avec ID: " + membreId);
        List<DocumentDTO> documents = documentService.getDocumentsByMembreId(membreId);
        if (documents.isEmpty()) {
            System.out.println("Aucun document trouvé pour le membre avec ID: " + membreId);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body("Aucun document trouvé pour ce membre.");
        }
        System.out.println("Documents trouvés pour le membre: " + documents.size());
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/en-attente")
    public ResponseEntity<List<DocumentDTO>> getDocumentsEnAttente() {
        System.out.println("Récupération des documents en attente...");
        List<DocumentDTO> documents = documentService.getDocumentsByStatus("en attente");
        if (documents.isEmpty()) {
            System.out.println("Aucun document en attente.");
            return ResponseEntity.noContent().build();
        }
        System.out.println("Documents en attente récupérés: " + documents.size());
        return ResponseEntity.ok(documents);
    }

    // ================== CREATE ==================

    @PostMapping
    public ResponseEntity<?> createDocument(@RequestParam("typeDocument") String typeDocument,
                                            @RequestParam("file") MultipartFile file,
                                            @RequestParam("utilisateurId") Long utilisateurId,
                                            @RequestParam(value = "membreId", required = false) Long membreId) {
        System.out.println("Création de document: typeDocument=" + typeDocument + ", utilisateurId=" + utilisateurId + ", membreId=" + membreId);
        try {
            if (typeDocument == null || typeDocument.trim().isEmpty()) {
                System.out.println("Le type de document est requis.");
                return ResponseEntity.badRequest().body("Le type de document est requis.");
            }
            if (file == null || file.isEmpty()) {
                System.out.println("Le fichier est requis.");
                return ResponseEntity.badRequest().body("Le fichier est requis.");
            }
            if (utilisateurId == null || utilisateurId <= 0) {
                System.out.println("L'ID de l'utilisateur est requis et doit être valide.");
                return ResponseEntity.badRequest().body("L'ID de l'utilisateur est requis et doit être valide.");
            }

            // 1) Upload du fichier
            String cheminFichier = fileUploadService.uploadFile(file, "documents");
            System.out.println("Fichier téléchargé avec succès: " + cheminFichier);

            // 2) Construire le DTO
            UtilisateurDTO utilisateurDTO = new UtilisateurDTO();
            utilisateurDTO.setId(utilisateurId);

            DocumentDTO dto = new DocumentDTO();
            dto.setTypeDocument(typeDocument);
            dto.setNomDocument(file.getOriginalFilename());
            dto.setCheminFichier(cheminFichier);
            dto.setStatus("en attente");
            dto.setUtilisateur(utilisateurDTO);       // compat ancien front
            dto.setUtilisateurId(utilisateurId);      // id à plat pour le service
            dto.setMembreId(membreId);                // ✅ rattachement enfant (optionnel)

            // 3) Persist
            DocumentDTO nouveauDocument = documentService.createDocument(dto);
            System.out.println("Document créé avec succès: " + nouveauDocument.getNomDocument());
            return ResponseEntity.status(HttpStatus.CREATED).body(nouveauDocument);

        } catch (IOException e) {
            System.out.println("Erreur lors du téléversement du fichier: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors du téléversement du fichier : " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("Erreur de validation: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Erreur de validation : " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Erreur inattendue: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur inattendue : " + e.getMessage());
        }
    }

    // ================== UPDATE ==================

    @PutMapping("/{id}")
    public ResponseEntity<?> updateDocument(@PathVariable Long id, @RequestBody DocumentDTO dto) {
        System.out.println("Mise à jour du document ID: " + id);
        try {
            DocumentDTO updated = documentService.updateDocument(id, dto);
            System.out.println("Document mis à jour: " + updated.getNomDocument());
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            System.out.println("Erreur de mise à jour: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // ✅ NOUVEAU : remplacement du fichier (multipart)
    @PutMapping("/{id}/file")
    public ResponseEntity<?> replaceFile(@PathVariable Long id,
                                         @RequestParam("file") MultipartFile file) {
        System.out.println("Remplacement du fichier pour le document ID: " + id);
        try {
            if (file == null || file.isEmpty()) {
                System.out.println("Le fichier est requis.");
                return ResponseEntity.badRequest().body("Le fichier est requis.");
            }
            // upload
            String cheminFichier = fileUploadService.uploadFile(file, "documents");
            System.out.println("Fichier téléchargé pour remplacement: " + cheminFichier);

            // récupérer le DTO existant, mettre à jour le chemin + nom + statut
            Optional<DocumentDTO> opt = documentService.getDocumentById(id);
            if (opt.isEmpty()) {
                System.out.println("Document introuvable avec l'ID: " + id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Document introuvable.");
            }
            DocumentDTO dto = opt.get();
            dto.setCheminFichier(cheminFichier);
            dto.setNomDocument(file.getOriginalFilename());
            dto.setStatus("en attente"); // on remet en attente après remplacement

            DocumentDTO updated = documentService.updateDocument(id, dto);
            System.out.println("Document mis à jour après remplacement: " + updated.getNomDocument());
            return ResponseEntity.ok(updated);

        } catch (IOException e) {
            System.out.println("Erreur lors du téléversement du fichier: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors du téléversement du fichier : " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("Erreur de validation: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            System.out.println("Erreur inattendue: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur inattendue : " + e.getMessage());
        }
    }

    // ================== VALIDATION ==================

    @PutMapping("/{id}/valider")
    public ResponseEntity<?> validerDocument(@PathVariable Long id) {
        System.out.println("Validation du document ID: " + id);
        Optional<DocumentDTO> document = documentService.getDocumentById(id);
        if (document.isPresent()) {
            DocumentDTO dto = document.get();
            dto.setStatus("validé");
            documentService.updateDocument(id, dto);
            System.out.println("Document validé: " + dto.getNomDocument());
            return ResponseEntity.ok().build();
        }
        System.out.println("Document introuvable avec l'ID: " + id);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Document introuvable.");
    }

    @PutMapping("/{id}/refuser")
    public ResponseEntity<?> refuserDocument(@PathVariable Long id) {
        System.out.println("Refus du document ID: " + id);
        Optional<DocumentDTO> document = documentService.getDocumentById(id);
        if (document.isPresent()) {
            DocumentDTO dto = document.get();
            dto.setStatus("refusé");
            documentService.updateDocument(id, dto);
            System.out.println("Document refusé: " + dto.getNomDocument());
            return ResponseEntity.ok().build();
        }
        System.out.println("Document introuvable avec l'ID: " + id);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Document introuvable.");
    }

    // ================== DELETE ==================

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDocument(@PathVariable Long id) {
        System.out.println("Suppression du document ID: " + id);
        Optional<DocumentDTO> document = documentService.getDocumentById(id);
        if (document.isEmpty()) {
            System.out.println("Document introuvable avec l'ID: " + id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Document introuvable.");
        }
        documentService.deleteDocument(id);
        System.out.println("Document supprimé avec l'ID: " + id);
        return ResponseEntity.noContent().build();
    }
}
