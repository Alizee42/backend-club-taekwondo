package club.taekwondo.controller.jpa;

import club.taekwondo.dto.DocumentDTO;
import club.taekwondo.dto.MembreDTO;
import club.taekwondo.entity.jpa.Club;
import club.taekwondo.entity.jpa.Membre;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.service.common.GoogleDriveUploadService;
import club.taekwondo.service.jpa.DocumentService;
import club.taekwondo.service.jpa.MembreService;
import club.taekwondo.service.jpa.UtilisateurService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentControllerTest {

    @Mock
    private DocumentService documentService;

    @Mock
    private UtilisateurService utilisateurService;

    @Mock
    private MembreService membreService;

    @Mock
    private GoogleDriveUploadService googleDriveUploadService;

    @TempDir
    Path tempUploadDir;

    private DocumentController controller;

    @BeforeEach
    void setUp() {
        controller = new DocumentController(documentService, utilisateurService, membreService, googleDriveUploadService);
    }

    private Authentication auth(String email, String role) {
        return new TestingAuthenticationToken(email, null, "ROLE_" + role);
    }

    private Utilisateur user(Long id, Long clubId) {
        Utilisateur u = new Utilisateur();
        u.setId(id);
        if (clubId != null) {
            Club club = new Club();
            club.setId(clubId);
            u.setClub(club);
        }
        return u;
    }

    private Membre membre(Long id, Long clubId) {
        Membre m = new Membre();
        m.setId(id);
        if (clubId != null) {
            Club club = new Club();
            club.setId(clubId);
            m.setClub(club);
        }
        return m;
    }

    private DocumentDTO doc(Long utilisateurId, Long membreId) {
        DocumentDTO dto = new DocumentDTO();
        dto.setUtilisateurId(utilisateurId);
        dto.setMembreId(membreId);
        dto.setStatus("en attente");
        return dto;
    }

    // ---- getAllDocuments ----

    @Test
    void getAllDocuments_superAdmin_retourneTous() {
        when(documentService.getAllDocumentsWithUtilisateur()).thenReturn(List.of(new DocumentDTO()));

        ResponseEntity<List<DocumentDTO>> response = controller.getAllDocuments(auth("super@test.com", "SUPER_ADMIN"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getAllDocuments_adminAvecClub_filtreParClub() {
        when(utilisateurService.findByEmail("admin@test.com")).thenReturn(Optional.of(user(1L, 10L)));
        when(documentService.getDocumentsByClubId(10L)).thenReturn(List.of(new DocumentDTO()));

        ResponseEntity<List<DocumentDTO>> response = controller.getAllDocuments(auth("admin@test.com", "ADMIN"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getAllDocuments_adminSansClub_retourneForbidden() {
        when(utilisateurService.findByEmail("admin@test.com")).thenReturn(Optional.of(user(1L, null)));

        ResponseEntity<List<DocumentDTO>> response = controller.getAllDocuments(auth("admin@test.com", "ADMIN"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void getAllDocuments_listeVide_retourneNoContent() {
        when(documentService.getAllDocumentsWithUtilisateur()).thenReturn(List.of());

        ResponseEntity<List<DocumentDTO>> response = controller.getAllDocuments(auth("super@test.com", "SUPER_ADMIN"));

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    // ---- getAllDocumentsAllClubs ----

    @Test
    void getAllDocumentsAllClubs_avecClubId_filtreParClub() {
        when(documentService.getDocumentsByClubId(10L)).thenReturn(List.of(new DocumentDTO()));

        ResponseEntity<List<DocumentDTO>> response = controller.getAllDocumentsAllClubs(10L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getAllDocumentsAllClubs_sansClubId_retourneTous() {
        when(documentService.getAllDocumentsWithUtilisateur()).thenReturn(List.of(new DocumentDTO()));

        ResponseEntity<List<DocumentDTO>> response = controller.getAllDocumentsAllClubs(null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // ---- getDocumentById ----

    @Test
    void getDocumentById_absent_retourneNotFound() {
        when(documentService.getDocumentById(1L)).thenReturn(Optional.empty());

        ResponseEntity<DocumentDTO> response = controller.getDocumentById(1L, auth("super@test.com", "SUPER_ADMIN"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void getDocumentById_proprietaire_retourneOk() {
        DocumentDTO document = doc(3L, null);
        when(documentService.getDocumentById(1L)).thenReturn(Optional.of(document));

        ResponseEntity<DocumentDTO> response = controller.getDocumentById(1L, withId(3L));

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    private Authentication withId(Long userId) {
        String email = "user" + userId + "@test.com";
        when(utilisateurService.findByEmail(email)).thenReturn(Optional.of(user(userId, null)));
        return new TestingAuthenticationToken(email, null, "ROLE_MEMBRE");
    }

    @Test
    void getDocumentById_nonProprietaire_leveForbidden() {
        DocumentDTO document = doc(3L, null);
        when(documentService.getDocumentById(1L)).thenReturn(Optional.of(document));

        ResponseStatusException ex = org.junit.jupiter.api.Assertions.assertThrows(ResponseStatusException.class,
                () -> controller.getDocumentById(1L, auth("autre@test.com", "MEMBRE")));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    // ---- getDocumentsByUtilisateur ----

    @Test
    void getDocumentsByUtilisateur_superAdmin_retourneOk() {
        when(documentService.getDocumentsByUtilisateurId(3L)).thenReturn(List.of(new DocumentDTO()));

        ResponseEntity<?> response = controller.getDocumentsByUtilisateur(3L, auth("super@test.com", "SUPER_ADMIN"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getDocumentsByUtilisateur_vide_retourneNoContent() {
        when(documentService.getDocumentsByUtilisateurId(3L)).thenReturn(List.of());

        ResponseEntity<?> response = controller.getDocumentsByUtilisateur(3L, auth("super@test.com", "SUPER_ADMIN"));

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void getDocumentsByUtilisateur_adminMemeClub_retourneOk() {
        when(utilisateurService.findByEmail("admin@test.com")).thenReturn(Optional.of(user(1L, 10L)));
        when(utilisateurService.getUtilisateurEntityById(3L)).thenReturn(Optional.of(user(3L, 10L)));
        when(documentService.getDocumentsByUtilisateurId(3L)).thenReturn(List.of(new DocumentDTO()));

        ResponseEntity<?> response = controller.getDocumentsByUtilisateur(3L, auth("admin@test.com", "ADMIN"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getDocumentsByUtilisateur_adminAutreClub_leveForbidden() {
        when(utilisateurService.findByEmail("admin@test.com")).thenReturn(Optional.of(user(1L, 10L)));
        when(utilisateurService.getUtilisateurEntityById(3L)).thenReturn(Optional.of(user(3L, 99L)));

        ResponseStatusException ex = org.junit.jupiter.api.Assertions.assertThrows(ResponseStatusException.class,
                () -> controller.getDocumentsByUtilisateur(3L, auth("admin@test.com", "ADMIN")));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    // ---- getDocumentsByMembre ----

    @Test
    void getDocumentsByMembre_parentProprietaire_retourneOk() {
        when(utilisateurService.findByEmail("parent@test.com")).thenReturn(Optional.of(user(1L, null)));
        MembreDTO membreDTO = new MembreDTO();
        membreDTO.setId(8L);
        when(membreService.getMembresByParentEmail("parent@test.com")).thenReturn(List.of(membreDTO));
        when(documentService.getDocumentsByMembreId(8L)).thenReturn(List.of(new DocumentDTO()));

        ResponseEntity<?> response = controller.getDocumentsByMembre(8L, auth("parent@test.com", "PARENT"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getDocumentsByMembre_parentNonProprietaire_leveForbidden() {
        when(utilisateurService.findByEmail("parent@test.com")).thenReturn(Optional.of(user(1L, null)));
        when(membreService.getMembresByParentEmail("parent@test.com")).thenReturn(List.of());

        ResponseStatusException ex = org.junit.jupiter.api.Assertions.assertThrows(ResponseStatusException.class,
                () -> controller.getDocumentsByMembre(8L, auth("parent@test.com", "PARENT")));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void getDocumentsByMembre_membreProprietaire_retourneOk() {
        when(utilisateurService.findByEmail("membre@test.com")).thenReturn(Optional.of(user(3L, null)));
        MembreDTO membreDTO = new MembreDTO();
        membreDTO.setId(8L);
        when(membreService.getMembreByUtilisateurEmail("membre@test.com")).thenReturn(Optional.of(membreDTO));
        when(documentService.getDocumentsByMembreId(8L)).thenReturn(List.of(new DocumentDTO()));

        ResponseEntity<?> response = controller.getDocumentsByMembre(8L, auth("membre@test.com", "MEMBRE"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // ---- getDocumentsEnAttente ----

    @Test
    void getDocumentsEnAttente_superAdmin_retourneOk() {
        DocumentDTO d = doc(1L, null);
        d.setStatus("en attente");
        when(documentService.getDocumentsByStatus("en attente")).thenReturn(List.of(d));

        ResponseEntity<List<DocumentDTO>> response = controller.getDocumentsEnAttente(auth("super@test.com", "SUPER_ADMIN"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getDocumentsEnAttente_adminFiltreParClub() {
        when(utilisateurService.findByEmail("admin@test.com")).thenReturn(Optional.of(user(1L, 10L)));
        DocumentDTO d = doc(1L, null);
        d.setStatus("en attente");
        when(documentService.getDocumentsByClubId(10L)).thenReturn(List.of(d));

        ResponseEntity<List<DocumentDTO>> response = controller.getDocumentsEnAttente(auth("admin@test.com", "ADMIN"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getDocumentsEnAttente_adminSansClub_retourneForbidden() {
        when(utilisateurService.findByEmail("admin@test.com")).thenReturn(Optional.of(user(1L, null)));

        ResponseEntity<List<DocumentDTO>> response = controller.getDocumentsEnAttente(auth("admin@test.com", "ADMIN"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    // ---- createDocument ----

    @Test
    void createDocument_sansTypeDocument_retourneBadRequest() {
        MultipartFile file = new MockMultipartFile("file", "f.pdf", "application/pdf", new byte[]{1});

        ResponseEntity<?> response = controller.createDocument("", file, 3L, null, auth("membre@test.com", "MEMBRE"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void createDocument_sansFichier_retourneBadRequest() {
        MultipartFile empty = new MockMultipartFile("file", "f.pdf", "application/pdf", new byte[]{});

        ResponseEntity<?> response = controller.createDocument("licence", empty, 3L, null, auth("membre@test.com", "MEMBRE"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void createDocument_utilisateurIdInvalide_retourneBadRequest() {
        MultipartFile file = new MockMultipartFile("file", "f.pdf", "application/pdf", new byte[]{1});

        ResponseEntity<?> response = controller.createDocument("licence", file, 0L, null, auth("membre@test.com", "MEMBRE"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void createDocument_membreCreePourSoiMeme_retourneCreated() throws Exception {
        MultipartFile file = new MockMultipartFile("file", "f.pdf", "application/pdf", new byte[]{1});
        when(googleDriveUploadService.uploadFileToDrive(any())).thenReturn("drive/f.pdf");
        DocumentDTO created = doc(3L, null);
        created.setId(99L);
        when(documentService.createDocument(any(DocumentDTO.class))).thenReturn(created);

        ResponseEntity<?> response = controller.createDocument("licence", file, 3L, null, withId(3L));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    void createDocument_membrePourAutrui_retourneForbiddenViaResponseStatusException() throws Exception {
        MultipartFile file = new MockMultipartFile("file", "f.pdf", "application/pdf", new byte[]{1});

        ResponseEntity<?> response = controller.createDocument("licence", file, 999L, null, withId(3L));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void createDocument_googleDriveEchecCredentials_bascculeEnLocalEtReussitAvecRepertoireConfigure() throws Exception {
        ReflectionTestUtils.setField(controller, "uploadDir", tempUploadDir.toString());
        MultipartFile file = new MockMultipartFile("file", "f.pdf", "application/pdf", "content".getBytes());
        when(googleDriveUploadService.uploadFileToDrive(any())).thenThrow(new RuntimeException("credentials invalides"));
        DocumentDTO created = doc(3L, null);
        when(documentService.createDocument(any(DocumentDTO.class))).thenReturn(created);

        ResponseEntity<?> response = controller.createDocument("licence", file, 3L, null, withId(3L));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    void createDocument_googleDriveEchecCredentials_sansRepertoireConfigure_retourneInternalServerError() throws Exception {
        MultipartFile file = new MockMultipartFile("file", "f.pdf", "application/pdf", "content".getBytes());
        when(googleDriveUploadService.uploadFileToDrive(any())).thenThrow(new RuntimeException("credentials invalides"));

        ResponseEntity<?> response = controller.createDocument("licence", file, 3L, null, withId(3L));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void createDocument_googleDriveAutreErreur_retourneInternalServerError() throws Exception {
        MultipartFile file = new MockMultipartFile("file", "f.pdf", "application/pdf", new byte[]{1});
        when(googleDriveUploadService.uploadFileToDrive(any())).thenThrow(new RuntimeException("quota depasse"));

        ResponseEntity<?> response = controller.createDocument("licence", file, 3L, null, withId(3L));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    // ---- updateDocument ----

    @Test
    void updateDocument_absent_retourneNotFound() {
        when(documentService.getDocumentById(1L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.updateDocument(1L, new DocumentDTO(), auth("super@test.com", "SUPER_ADMIN"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void updateDocument_succes_retourneOk() {
        DocumentDTO existing = doc(3L, null);
        existing.setNomDocument("ancien.pdf");
        existing.setCheminFichier("chemin/ancien.pdf");
        existing.setTypeDocument("licence");
        existing.setStatus("en attente");
        when(documentService.getDocumentById(1L)).thenReturn(Optional.of(existing));
        when(documentService.updateDocument(eq(1L), any(DocumentDTO.class))).thenReturn(existing);

        ResponseEntity<?> response = controller.updateDocument(1L, new DocumentDTO(), withId(3L));

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // ---- validerDocument / refuserDocument ----

    @Test
    void validerDocument_absent_retourneNotFound() {
        when(documentService.getDocumentById(1L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.validerDocument(1L, auth("super@test.com", "SUPER_ADMIN"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void validerDocument_succes_retourneOk() {
        DocumentDTO existing = doc(3L, null);
        when(documentService.getDocumentById(1L)).thenReturn(Optional.of(existing));

        ResponseEntity<?> response = controller.validerDocument(1L, auth("super@test.com", "SUPER_ADMIN"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void refuserDocument_avecMotif_appelleLeServiceAvecMotif() {
        DocumentDTO existing = doc(3L, null);
        when(documentService.getDocumentById(1L)).thenReturn(Optional.of(existing));

        ResponseEntity<?> response = controller.refuserDocument(1L, Map.of("motifRefus", "illisible"), auth("super@test.com", "SUPER_ADMIN"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        org.mockito.Mockito.verify(documentService).updateDocumentStatus(1L, "refusé", "illisible");
    }

    @Test
    void refuserDocument_absent_retourneNotFound() {
        when(documentService.getDocumentById(1L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.refuserDocument(1L, null, auth("super@test.com", "SUPER_ADMIN"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // ---- deleteDocument ----

    @Test
    void deleteDocument_absent_retourneNotFound() {
        when(documentService.getDocumentById(1L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.deleteDocument(1L, auth("super@test.com", "SUPER_ADMIN"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void deleteDocument_succes_retourneNoContent() {
        DocumentDTO existing = doc(3L, null);
        when(documentService.getDocumentById(1L)).thenReturn(Optional.of(existing));

        ResponseEntity<?> response = controller.deleteDocument(1L, withId(3L));

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        org.mockito.Mockito.verify(documentService).deleteDocument(1L);
    }

    // ---- downloadDocument ----

    @Test
    void downloadDocument_absent_retourneNotFound() {
        when(documentService.getDocumentById(1L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.downloadDocument(1L, auth("super@test.com", "SUPER_ADMIN"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void downloadDocument_cheminVide_retourneBadRequest() {
        DocumentDTO document = doc(3L, null);
        document.setCheminFichier("");
        when(documentService.getDocumentById(1L)).thenReturn(Optional.of(document));

        ResponseEntity<?> response = controller.downloadDocument(1L, withId(3L));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void downloadDocument_cheminUrlDistante_redirige() {
        DocumentDTO document = doc(3L, null);
        document.setCheminFichier("https://drive.google.com/file123");
        when(documentService.getDocumentById(1L)).thenReturn(Optional.of(document));

        ResponseEntity<?> response = controller.downloadDocument(1L, withId(3L));

        assertEquals(HttpStatus.FOUND, response.getStatusCode());
    }

    @Test
    void downloadDocument_fichierLocalInexistant_retourneNotFound() {
        ReflectionTestUtils.setField(controller, "uploadDir", tempUploadDir.toString());
        DocumentDTO document = doc(3L, null);
        document.setCheminFichier("inexistant.pdf");
        when(documentService.getDocumentById(1L)).thenReturn(Optional.of(document));

        ResponseEntity<?> response = controller.downloadDocument(1L, withId(3L));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void downloadDocument_uploadDirNonConfigure_retourneInternalServerError() {
        DocumentDTO document = doc(3L, null);
        document.setCheminFichier("inexistant.pdf");
        when(documentService.getDocumentById(1L)).thenReturn(Optional.of(document));

        ResponseEntity<?> response = controller.downloadDocument(1L, withId(3L));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }
}
