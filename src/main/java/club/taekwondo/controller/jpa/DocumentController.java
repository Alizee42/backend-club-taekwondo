package club.taekwondo.controller.jpa;

import club.taekwondo.dto.DocumentDTO;
import club.taekwondo.dto.UtilisateurDTO;
import club.taekwondo.service.common.GoogleDriveUploadService;
import club.taekwondo.service.jpa.DocumentService;
import club.taekwondo.service.jpa.UtilisateurService;
import club.taekwondo.entity.jpa.Utilisateur;
import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.UrlResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.io.IOException;
import java.io.InputStream;
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
    private GoogleDriveUploadService googleDriveUploadService;

    @Value("${upload.dir:uploads}")
    private String uploadDir;

    // ================== READ ==================

    @GetMapping
    public ResponseEntity<List<DocumentDTO>> getAllDocuments(Authentication authentication) {
        Utilisateur user = utilisateurService.findByEmail(authentication.getName()).orElseThrow();
        Long clubId = user.getClub().getId();
        List<DocumentDTO> documents = documentService.getDocumentsByClubId(clubId);
        if (documents.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(documents);
    }

    // ✅ Super Admin: tous les documents (tous clubs)
    @GetMapping("/all")
    public ResponseEntity<List<DocumentDTO>> getAllDocumentsAllClubs(@RequestParam(value = "clubId", required = false) Long clubId) {
        List<DocumentDTO> documents = (clubId != null && clubId > 0)
                ? documentService.getDocumentsByClubId(clubId)
                : documentService.getAllDocumentsWithUtilisateur();
        if (documents.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(documents);
    }

    // Téléchargement/redirect : si cheminFichier est une URL Drive, on redirige; sinon on sert le fichier local
    @GetMapping("/{id}/download")
    public ResponseEntity<?> downloadDocument(@PathVariable Long id) {
        Optional<DocumentDTO> opt = documentService.getDocumentById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        DocumentDTO dto = opt.get();
        String chemin = dto.getCheminFichier();
        if (chemin == null || chemin.isBlank()) {
            return ResponseEntity.badRequest().body("Aucun chemin disponible pour ce document.");
        }

        // Si c'est une URL (http/https), redirection
        if (chemin.startsWith("http://") || chemin.startsWith("https://")) {
            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(chemin)).build();
        }

        // Sinon on tente de servir le fichier local depuis uploadDir/documents
        try {
            Path base = Paths.get(uploadDir).toAbsolutePath().normalize().resolve("documents").normalize();
            Path file = base.resolve(chemin).normalize();
            if (!file.startsWith(base) || !Files.exists(file)) {
                return ResponseEntity.notFound().build();
            }
            String mime = Files.probeContentType(file);
            if (mime == null) mime = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            UrlResource resource = new UrlResource(file.toUri());
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(mime))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getFileName().toString().replace("\"", "") + "\"")
                    .cacheControl(CacheControl.noCache())
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur lors de la récupération du fichier: " + e.getMessage());
        }
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

            // 1) Upload du fichier sur Google Drive (fallback vers stockage local si credentials invalides)
            String cheminFichier;
            try {
                cheminFichier = googleDriveUploadService.uploadFileToDrive(file);
            } catch (Exception ex) {
                System.out.println("Erreur Google Drive: " + ex.getMessage());
                // si l'erreur provient d'un mauvais fichier de credentials, on bascule en stockage local
                if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("credentials")) {
                    try {
                        cheminFichier = saveFileLocally(file);
                        System.out.println("Fichier sauvegardé localement (fallback): " + cheminFichier);
                    } catch (Exception saveEx) {
                        System.out.println("Échec du fallback local: " + saveEx.getMessage());
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body("Erreur Google Drive et échec du fallback local: " + saveEx.getMessage());
                    }
                } else {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Erreur Google Drive : " + ex.getMessage());
                }
            }
            System.out.println("Fichier téléchargé sur Google Drive: " + cheminFichier);

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
            String cheminFichier;
            try {
                cheminFichier = googleDriveUploadService.uploadFileToDrive(file);
            } catch (Exception ex) {
                System.out.println("Erreur Google Drive: " + ex.getMessage());
                if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("credentials")) {
                    try {
                        cheminFichier = saveFileLocally(file);
                        System.out.println("Fichier sauvegardé localement (fallback): " + cheminFichier);
                    } catch (Exception saveEx) {
                        System.out.println("Échec du fallback local: " + saveEx.getMessage());
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body("Erreur Google Drive et échec du fallback local: " + saveEx.getMessage());
                    }
                } else {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Erreur Google Drive : " + ex.getMessage());
                }
            }
            System.out.println("Fichier téléchargé sur Google Drive pour remplacement: " + cheminFichier);

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

        } catch (IllegalArgumentException e) {
            System.out.println("Erreur de validation: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            System.out.println("Erreur inattendue: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur inattendue : " + e.getMessage());
        }
    }

    // Helper - save the uploaded file under uploadDir/documents and return the filename
    private String saveFileLocally(MultipartFile file) throws IOException {
        Path base = Paths.get(uploadDir).toAbsolutePath().normalize().resolve("documents").normalize();
        Files.createDirectories(base);
        String safeName = System.currentTimeMillis() + "_" + file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_");
        Path target = base.resolve(safeName);
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        return safeName; // chemin relatif stocké dans DB
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
