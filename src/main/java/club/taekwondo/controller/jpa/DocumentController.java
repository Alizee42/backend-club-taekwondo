package club.taekwondo.controller.jpa;

import club.taekwondo.dto.DocumentDTO;
import club.taekwondo.dto.MembreDTO;
import club.taekwondo.dto.UtilisateurDTO;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.service.common.GoogleDriveUploadService;
import club.taekwondo.service.jpa.DocumentService;
import club.taekwondo.service.jpa.MembreService;
import club.taekwondo.service.jpa.UtilisateurService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.UrlResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final UtilisateurService utilisateurService;
    private final MembreService membreService;
    private final GoogleDriveUploadService googleDriveUploadService;

    @Value("${upload.dir:uploads}")
    private String uploadDir;

    public DocumentController(DocumentService documentService,
                              UtilisateurService utilisateurService,
                              MembreService membreService,
                              GoogleDriveUploadService googleDriveUploadService) {
        this.documentService = documentService;
        this.utilisateurService = utilisateurService;
        this.membreService = membreService;
        this.googleDriveUploadService = googleDriveUploadService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<DocumentDTO>> getAllDocuments(Authentication authentication) {
        List<DocumentDTO> documents;
        if (hasAnyRole(authentication, "SUPER_ADMIN")) {
            documents = documentService.getAllDocumentsWithUtilisateur();
        } else {
            Utilisateur user = requireAuthenticatedUser(authentication);
            if (user.getClub() == null || user.getClub().getId() == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            documents = documentService.getDocumentsByClubId(user.getClub().getId());
        }

        if (documents.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<DocumentDTO>> getAllDocumentsAllClubs(@RequestParam(value = "clubId", required = false) Long clubId) {
        List<DocumentDTO> documents = (clubId != null && clubId > 0)
                ? documentService.getDocumentsByClubId(clubId)
                : documentService.getAllDocumentsWithUtilisateur();
        if (documents.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','PARENT','MEMBRE')")
    public ResponseEntity<?> downloadDocument(@PathVariable Long id, Authentication authentication) {
        Optional<DocumentDTO> opt = documentService.getDocumentById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        DocumentDTO dto = opt.get();
        assertCanAccessDocument(authentication, dto);

        String chemin = dto.getCheminFichier();
        if (chemin == null || chemin.isBlank()) {
            return ResponseEntity.badRequest().body("Aucun chemin disponible pour ce document.");
        }

        if (chemin.startsWith("http://") || chemin.startsWith("https://")) {
            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(chemin)).build();
        }

        try {
            Path base = Paths.get(uploadDir).toAbsolutePath().normalize().resolve("documents").normalize();
            Path file = base.resolve(chemin).normalize();
            if (!file.startsWith(base) || !Files.exists(file)) {
                return ResponseEntity.notFound().build();
            }
            String mime = Files.probeContentType(file);
            if (mime == null) {
                mime = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }
            UrlResource resource = new UrlResource(file.toUri());
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(mime))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + file.getFileName().toString().replace("\"", "") + "\"")
                    .cacheControl(CacheControl.noCache())
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la recuperation du fichier: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','PARENT','MEMBRE')")
    public ResponseEntity<DocumentDTO> getDocumentById(@PathVariable Long id, Authentication authentication) {
        Optional<DocumentDTO> document = documentService.getDocumentById(id);
        if (document.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        DocumentDTO dto = document.get();
        assertCanAccessDocument(authentication, dto);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/utilisateur/{utilisateurId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','PARENT','MEMBRE')")
    public ResponseEntity<?> getDocumentsByUtilisateur(@PathVariable Long utilisateurId, Authentication authentication) {
        assertCanAccessUtilisateurDocuments(authentication, utilisateurId);

        List<DocumentDTO> documents = documentService.getDocumentsByUtilisateurId(utilisateurId);
        if (documents.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body("Aucun document trouve pour cet utilisateur.");
        }
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/membre/{membreId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','PARENT','MEMBRE')")
    public ResponseEntity<?> getDocumentsByMembre(@PathVariable Long membreId, Authentication authentication) {
        assertCanAccessMemberDocuments(authentication, membreId);

        List<DocumentDTO> documents = documentService.getDocumentsByMembreId(membreId);
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/en-attente")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<DocumentDTO>> getDocumentsEnAttente(Authentication authentication) {
        List<DocumentDTO> documents;
        if (hasAnyRole(authentication, "SUPER_ADMIN")) {
            documents = documentService.getDocumentsByStatus("en attente");
        } else {
            Utilisateur user = requireAuthenticatedUser(authentication);
            if (user.getClub() == null || user.getClub().getId() == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            documents = documentService.getDocumentsByClubId(user.getClub().getId()).stream()
                    .filter(document -> "en attente".equalsIgnoreCase(document.getStatus()))
                    .toList();
        }

        if (documents.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(documents);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','PARENT','MEMBRE')")
    public ResponseEntity<?> createDocument(@RequestParam("typeDocument") String typeDocument,
                                            @RequestParam("file") MultipartFile file,
                                            @RequestParam("utilisateurId") Long utilisateurId,
                                            @RequestParam(value = "membreId", required = false) Long membreId,
                                            Authentication authentication) {
        try {
            if (typeDocument == null || typeDocument.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Le type de document est requis.");
            }
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body("Le fichier est requis.");
            }
            if (utilisateurId == null || utilisateurId <= 0) {
                return ResponseEntity.badRequest().body("L'ID de l'utilisateur est requis et doit etre valide.");
            }

            assertCanCreateOrManageDocument(authentication, utilisateurId, membreId);

            String cheminFichier;
            try {
                cheminFichier = googleDriveUploadService.uploadFileToDrive(file);
            } catch (Exception ex) {
                if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("credentials")) {
                    try {
                        cheminFichier = saveFileLocally(file);
                    } catch (Exception saveEx) {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body("Erreur Google Drive et echec du fallback local: " + saveEx.getMessage());
                    }
                } else {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Erreur Google Drive : " + ex.getMessage());
                }
            }

            UtilisateurDTO utilisateurDTO = new UtilisateurDTO();
            utilisateurDTO.setId(utilisateurId);

            DocumentDTO dto = new DocumentDTO();
            dto.setTypeDocument(typeDocument);
            dto.setNomDocument(file.getOriginalFilename());
            dto.setCheminFichier(cheminFichier);
            dto.setStatus("en attente");
            dto.setUtilisateur(utilisateurDTO);
            dto.setUtilisateurId(utilisateurId);
            dto.setMembreId(membreId);

            DocumentDTO nouveauDocument = documentService.createDocument(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(nouveauDocument);

        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Erreur de validation : " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur inattendue : " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','PARENT','MEMBRE')")
    public ResponseEntity<?> updateDocument(@PathVariable Long id,
                                            @RequestBody DocumentDTO dto,
                                            Authentication authentication) {
        try {
            Optional<DocumentDTO> existingOpt = documentService.getDocumentById(id);
            if (existingOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Document introuvable.");
            }

            DocumentDTO existing = existingOpt.get();
            assertCanAccessDocument(authentication, existing);

            dto.setUtilisateur(existing.getUtilisateur());
            dto.setUtilisateurId(existing.getUtilisateurId());
            dto.setMembreId(existing.getMembreId());
            dto.setEnfantId(existing.getEnfantId());
            dto.setEnfant(existing.getEnfant());
            if (dto.getCheminFichier() == null || dto.getCheminFichier().isBlank()) {
                dto.setCheminFichier(existing.getCheminFichier());
            }
            if (dto.getNomDocument() == null || dto.getNomDocument().isBlank()) {
                dto.setNomDocument(existing.getNomDocument());
            }
            if (dto.getTypeDocument() == null || dto.getTypeDocument().isBlank()) {
                dto.setTypeDocument(existing.getTypeDocument());
            }
            if (dto.getStatus() == null || dto.getStatus().isBlank()) {
                dto.setStatus(existing.getStatus());
            }

            DocumentDTO updated = documentService.updateDocument(id, dto);
            return ResponseEntity.ok(updated);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PutMapping("/{id}/file")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','PARENT','MEMBRE')")
    public ResponseEntity<?> replaceFile(@PathVariable Long id,
                                         @RequestParam("file") MultipartFile file,
                                         Authentication authentication) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body("Le fichier est requis.");
            }

            Optional<DocumentDTO> opt = documentService.getDocumentById(id);
            if (opt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Document introuvable.");
            }

            DocumentDTO dto = opt.get();
            assertCanAccessDocument(authentication, dto);

            String cheminFichier;
            try {
                cheminFichier = googleDriveUploadService.uploadFileToDrive(file);
            } catch (Exception ex) {
                if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("credentials")) {
                    try {
                        cheminFichier = saveFileLocally(file);
                    } catch (Exception saveEx) {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body("Erreur Google Drive et echec du fallback local: " + saveEx.getMessage());
                    }
                } else {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Erreur Google Drive : " + ex.getMessage());
                }
            }

            dto.setCheminFichier(cheminFichier);
            dto.setNomDocument(file.getOriginalFilename());
            dto.setStatus("en attente");

            DocumentDTO updated = documentService.updateDocument(id, dto);
            return ResponseEntity.ok(updated);

        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur inattendue : " + e.getMessage());
        }
    }

    private String saveFileLocally(MultipartFile file) throws IOException {
        Path base = Paths.get(uploadDir).toAbsolutePath().normalize().resolve("documents").normalize();
        Files.createDirectories(base);
        String safeName = System.currentTimeMillis() + "_" + file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_");
        Path target = base.resolve(safeName);
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        return safeName;
    }

    @PutMapping("/{id}/valider")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<?> validerDocument(@PathVariable Long id, Authentication authentication) {
        Optional<DocumentDTO> document = documentService.getDocumentById(id);
        if (document.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Document introuvable.");
        }

        DocumentDTO dto = document.get();
        assertCanAccessDocument(authentication, dto);
        dto.setStatus("validé");
        documentService.updateDocument(id, dto);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/refuser")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<?> refuserDocument(@PathVariable Long id, Authentication authentication) {
        Optional<DocumentDTO> document = documentService.getDocumentById(id);
        if (document.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Document introuvable.");
        }

        DocumentDTO dto = document.get();
        assertCanAccessDocument(authentication, dto);
        dto.setStatus("refusé");
        documentService.updateDocument(id, dto);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','PARENT','MEMBRE')")
    public ResponseEntity<?> deleteDocument(@PathVariable Long id, Authentication authentication) {
        Optional<DocumentDTO> document = documentService.getDocumentById(id);
        if (document.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Document introuvable.");
        }

        assertCanAccessDocument(authentication, document.get());
        documentService.deleteDocument(id);
        return ResponseEntity.noContent().build();
    }

    private Utilisateur requireAuthenticatedUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifie");
        }
        return utilisateurService.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non trouve"));
    }

    private void assertCanAccessUtilisateurDocuments(Authentication authentication, Long utilisateurId) {
        if (hasAnyRole(authentication, "SUPER_ADMIN")) {
            return;
        }

        Utilisateur currentUser = requireAuthenticatedUser(authentication);
        if (hasAnyRole(authentication, "ADMIN")) {
            assertAdminUserScope(currentUser, utilisateurId);
            return;
        }

        if (!utilisateurId.equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acces refuse");
        }
    }

    private void assertCanAccessMemberDocuments(Authentication authentication, Long membreId) {
        if (hasAnyRole(authentication, "SUPER_ADMIN")) {
            return;
        }

        Utilisateur currentUser = requireAuthenticatedUser(authentication);
        if (hasAnyRole(authentication, "ADMIN")) {
            assertAdminMemberScope(currentUser, membreId);
            return;
        }

        if (hasAnyRole(authentication, "PARENT") && parentOwnsMember(authentication, membreId)) {
            return;
        }

        if (hasAnyRole(authentication, "MEMBRE") && memberOwnsMember(authentication, membreId)) {
            return;
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acces refuse");
    }

    private void assertCanCreateOrManageDocument(Authentication authentication, Long utilisateurId, Long membreId) {
        if (hasAnyRole(authentication, "SUPER_ADMIN")) {
            return;
        }

        Utilisateur currentUser = requireAuthenticatedUser(authentication);
        if (hasAnyRole(authentication, "ADMIN")) {
            assertAdminUserScope(currentUser, utilisateurId);
            if (membreId != null) {
                assertAdminMemberScope(currentUser, membreId);
            }
            return;
        }

        if (!utilisateurId.equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acces refuse");
        }

        if (membreId == null) {
            return;
        }

        if (hasAnyRole(authentication, "PARENT") && parentOwnsMember(authentication, membreId)) {
            return;
        }

        if (hasAnyRole(authentication, "MEMBRE") && memberOwnsMember(authentication, membreId)) {
            return;
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acces refuse");
    }

    private void assertCanAccessDocument(Authentication authentication, DocumentDTO document) {
        if (hasAnyRole(authentication, "SUPER_ADMIN")) {
            return;
        }

        Utilisateur currentUser = requireAuthenticatedUser(authentication);
        if (hasAnyRole(authentication, "ADMIN")) {
            if (document.getUtilisateurId() != null) {
                assertAdminUserScope(currentUser, document.getUtilisateurId());
            }
            if (document.getMembreId() != null) {
                assertAdminMemberScope(currentUser, document.getMembreId());
            }
            return;
        }

        if (document.getUtilisateurId() != null && document.getUtilisateurId().equals(currentUser.getId())) {
            return;
        }

        if (document.getMembreId() != null && hasAnyRole(authentication, "PARENT") && parentOwnsMember(authentication, document.getMembreId())) {
            return;
        }

        if (document.getMembreId() != null && hasAnyRole(authentication, "MEMBRE") && memberOwnsMember(authentication, document.getMembreId())) {
            return;
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acces refuse");
    }

    private void assertAdminUserScope(Utilisateur admin, Long utilisateurId) {
        Utilisateur targetUser = utilisateurService.getUtilisateurEntityById(utilisateurId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
        if (admin.getClub() == null
                || admin.getClub().getId() == null
                || targetUser.getClub() == null
                || targetUser.getClub().getId() == null
                || !admin.getClub().getId().equals(targetUser.getClub().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acces refuse");
        }
    }

    private void assertAdminMemberScope(Utilisateur admin, Long membreId) {
        var membre = membreService.findById(membreId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membre introuvable"));
        if (admin.getClub() == null
                || admin.getClub().getId() == null
                || membre.getClub() == null
                || membre.getClub().getId() == null
                || !admin.getClub().getId().equals(membre.getClub().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acces refuse");
        }
    }

    private boolean parentOwnsMember(Authentication authentication, Long membreId) {
        return membreService.getMembresByParentEmail(authentication.getName()).stream()
                .anyMatch(membre -> membreId.equals(membre.getId()));
    }

    private boolean memberOwnsMember(Authentication authentication, Long membreId) {
        Optional<MembreDTO> membre = membreService.getMembreByUtilisateurEmail(authentication.getName());
        return membre.isPresent() && membreId.equals(membre.get().getId());
    }

    private boolean hasAnyRole(Authentication authentication, String... roles) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }

        for (String role : roles) {
            String authority = "ROLE_" + role;
            boolean found = authentication.getAuthorities().stream()
                    .anyMatch(grantedAuthority -> authority.equals(grantedAuthority.getAuthority()));
            if (found) {
                return true;
            }
        }

        return false;
    }
}
