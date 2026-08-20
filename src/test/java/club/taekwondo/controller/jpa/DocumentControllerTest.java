package club.taekwondo.controller.jpa;

import club.taekwondo.dto.DocumentDTO;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

    private DocumentController controller;

    @BeforeEach
    void setUp() {
        controller = new DocumentController(documentService, utilisateurService, membreService, googleDriveUploadService);
        ReflectionTestUtils.setField(controller, "uploadDir", "uploads");
    }

    @Test
    void getDocumentById_parentCannotAccessForeignDocument() {
        Authentication auth = auth("parent@test.com", "ROLE_PARENT");
        Utilisateur parent = user(10L, "parent@test.com", 1L);
        DocumentDTO document = document(5L, 22L, 301L, "https://example.test/doc.pdf", "en attente");

        when(utilisateurService.findByEmail("parent@test.com")).thenReturn(Optional.of(parent));
        when(documentService.getDocumentById(5L)).thenReturn(Optional.of(document));
        when(membreService.getMembresByParentEmail("parent@test.com")).thenReturn(List.of());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> controller.getDocumentById(5L, auth));

        assertEquals(HttpStatus.FORBIDDEN.value(), exception.getStatusCode().value());
    }

    @Test
    void createDocument_memberCannotCreateForAnotherUser() throws Exception {
        Authentication auth = auth("membre@test.com", "ROLE_MEMBRE");
        Utilisateur memberUser = user(7L, "membre@test.com", 1L);
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", "test".getBytes());

        when(utilisateurService.findByEmail("membre@test.com")).thenReturn(Optional.of(memberUser));

        ResponseEntity<?> response = controller.createDocument("licence", file, 99L, null, auth);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(googleDriveUploadService, never()).uploadFileToDrive(file);
    }

    @Test
    void getDocumentsEnAttente_adminUsesOwnClubScope() {
        Authentication auth = auth("admin@test.com", "ROLE_ADMIN");
        Utilisateur admin = user(1L, "admin@test.com", 7L);

        when(utilisateurService.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(documentService.getDocumentsByClubId(7L)).thenReturn(List.of(
                document(1L, 11L, null, "a.pdf", "en attente"),
                document(2L, 12L, null, "b.pdf", "validé")
        ));

        ResponseEntity<List<DocumentDTO>> response = controller.getDocumentsEnAttente(auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals(1L, response.getBody().get(0).getId());
        verify(documentService, never()).getDocumentsByStatus("en attente");
    }

    @Test
    void validerDocument_adminCannotValidateOtherClubDocument() {
        Authentication auth = auth("admin@test.com", "ROLE_ADMIN");
        Utilisateur admin = user(1L, "admin@test.com", 1L);
        DocumentDTO document = document(8L, 22L, null, "doc.pdf", "en attente");
        Utilisateur targetUser = user(22L, "other@test.com", 2L);

        when(utilisateurService.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(documentService.getDocumentById(8L)).thenReturn(Optional.of(document));
        when(utilisateurService.getUtilisateurEntityById(22L)).thenReturn(Optional.of(targetUser));

        ResponseEntity<?> response = controller.validerDocument(8L, auth);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(documentService, never()).updateDocumentStatus(anyLong(), anyString());
    }

    @Test
    void getAllDocuments_adminSeesOnlyOwnClub() {
        Authentication auth = auth("admin@test.com", "ROLE_ADMIN");
        Utilisateur admin = user(1L, "admin@test.com", 3L);
        when(utilisateurService.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(documentService.getDocumentsByClubId(3L)).thenReturn(List.of(document(1L, 10L, null, "a.pdf", "en attente")));

        ResponseEntity<List<DocumentDTO>> response = controller.getAllDocuments(auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(documentService, never()).getAllDocumentsWithUtilisateur();
        verify(documentService).getDocumentsByClubId(3L);
    }

    @Test
    void getAllDocuments_superAdminSeesEveryClub() {
        Authentication auth = auth("super@test.com", "ROLE_SUPER_ADMIN");
        when(documentService.getAllDocumentsWithUtilisateur()).thenReturn(List.of(document(1L, 10L, null, "a.pdf", "en attente")));

        ResponseEntity<List<DocumentDTO>> response = controller.getAllDocuments(auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(utilisateurService, never()).findByEmail(anyString());
    }

    @Test
    void getAllDocuments_adminWithoutClub_isForbidden() {
        Authentication auth = auth("admin@test.com", "ROLE_ADMIN");
        Utilisateur admin = user(1L, "admin@test.com", null);
        when(utilisateurService.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));

        ResponseEntity<List<DocumentDTO>> response = controller.getAllDocuments(auth);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void validerDocument_ownerCanValidateOwnClubDocument() {
        Authentication auth = auth("admin@test.com", "ROLE_ADMIN");
        Utilisateur admin = user(1L, "admin@test.com", 1L);
        DocumentDTO document = document(8L, 22L, null, "doc.pdf", "en attente");
        Utilisateur targetUser = user(22L, "other@test.com", 1L);

        when(utilisateurService.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(documentService.getDocumentById(8L)).thenReturn(Optional.of(document));
        when(utilisateurService.getUtilisateurEntityById(22L)).thenReturn(Optional.of(targetUser));

        ResponseEntity<?> response = controller.validerDocument(8L, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(documentService).updateDocumentStatus(8L, "validé");
    }

    @Test
    void refuserDocument_passesMotifToService() {
        Authentication auth = auth("admin@test.com", "ROLE_ADMIN");
        Utilisateur admin = user(1L, "admin@test.com", 1L);
        DocumentDTO document = document(8L, 22L, null, "doc.pdf", "en attente");
        Utilisateur targetUser = user(22L, "other@test.com", 1L);

        when(utilisateurService.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(documentService.getDocumentById(8L)).thenReturn(Optional.of(document));
        when(utilisateurService.getUtilisateurEntityById(22L)).thenReturn(Optional.of(targetUser));

        ResponseEntity<?> response = controller.refuserDocument(8L, java.util.Map.of("motifRefus", "Photo illisible"), auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(documentService).updateDocumentStatus(8L, "refusé", "Photo illisible");
    }

    @Test
    void deleteDocument_notFound_returns404() {
        when(documentService.getDocumentById(404L)).thenReturn(Optional.empty());
        Authentication auth = auth("admin@test.com", "ROLE_ADMIN");

        ResponseEntity<?> response = controller.deleteDocument(404L, auth);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(documentService, never()).deleteDocument(anyLong());
    }

    @Test
    void deleteDocument_ownerCanDeleteOwnDocument() {
        Authentication auth = auth("membre@test.com", "ROLE_MEMBRE");
        Utilisateur membre = user(7L, "membre@test.com", 1L);
        DocumentDTO document = document(3L, 7L, null, "doc.pdf", "en attente");

        when(documentService.getDocumentById(3L)).thenReturn(Optional.of(document));
        when(utilisateurService.findByEmail("membre@test.com")).thenReturn(Optional.of(membre));

        ResponseEntity<?> response = controller.deleteDocument(3L, auth);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(documentService).deleteDocument(3L);
    }

    @Test
    void getDocumentsByMembre_adminOfOtherClubIsForbidden() {
        Authentication auth = auth("admin@test.com", "ROLE_ADMIN");
        Utilisateur admin = user(1L, "admin@test.com", 1L);
        Membre membre = new Membre();
        membre.setId(50L);
        Club otherClub = new Club();
        otherClub.setId(2L);
        membre.setClub(otherClub);

        when(utilisateurService.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(membreService.findById(50L)).thenReturn(Optional.of(membre));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> controller.getDocumentsByMembre(50L, auth));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(documentService, never()).getDocumentsByMembreId(anyLong());
    }

    @Test
    void getDocumentsByMembre_unknownMembre_returns404() {
        Authentication auth = auth("admin@test.com", "ROLE_ADMIN");
        Utilisateur admin = user(1L, "admin@test.com", 1L);
        when(utilisateurService.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(membreService.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> controller.getDocumentsByMembre(999L, auth));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void getDocumentById_notFound_returns404() {
        when(documentService.getDocumentById(123L)).thenReturn(Optional.empty());
        Authentication auth = auth("admin@test.com", "ROLE_ADMIN");

        ResponseEntity<DocumentDTO> response = controller.getDocumentById(123L, auth);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void createDocument_missingFile_returnsBadRequest() {
        Authentication auth = auth("membre@test.com", "ROLE_MEMBRE");

        ResponseEntity<?> response = controller.createDocument("licence", null, 7L, null, auth);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    private Authentication auth(String email, String authority) {
        TestingAuthenticationToken token = new TestingAuthenticationToken(email, null, authority);
        token.setAuthenticated(true);
        return token;
    }

    private Utilisateur user(Long id, String email, Long clubId) {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(id);
        utilisateur.setEmail(email);
        if (clubId != null) {
            Club club = new Club();
            club.setId(clubId);
            club.setName("Club " + clubId);
            utilisateur.setClub(club);
        }
        return utilisateur;
    }

    private DocumentDTO document(Long id, Long utilisateurId, Long membreId, String chemin, String status) {
        DocumentDTO dto = new DocumentDTO();
        dto.setId(id);
        dto.setUtilisateurId(utilisateurId);
        dto.setMembreId(membreId);
        dto.setCheminFichier(chemin);
        dto.setStatus(status);
        dto.setNomDocument("Document " + id);
        dto.setTypeDocument("licence");
        return dto;
    }
}
