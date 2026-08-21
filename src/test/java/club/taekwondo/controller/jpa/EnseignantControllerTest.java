package club.taekwondo.controller.jpa;

import club.taekwondo.dto.EnseignantDTO;
import club.taekwondo.entity.jpa.Club;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.enums.Role;
import club.taekwondo.service.common.FileUploadService;
import club.taekwondo.service.jpa.EnseignantService;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnseignantControllerTest {

    @Mock
    private EnseignantService enseignantService;

    @Mock
    private UtilisateurService utilisateurService;

    @Mock
    private FileUploadService fileUploadService;

    private EnseignantController controller;

    @BeforeEach
    void setUp() {
        controller = new EnseignantController(enseignantService, utilisateurService, fileUploadService);
    }

    private Authentication auth(String email) {
        return new TestingAuthenticationToken(email, null, "ROLE_ADMIN");
    }

    private Utilisateur user(Long clubId, Role role) {
        Utilisateur u = new Utilisateur();
        u.setRole(role);
        if (clubId != null) {
            Club club = new Club();
            club.setId(clubId);
            u.setClub(club);
        }
        return u;
    }

    @Test
    void getByClub_delegueAuService() {
        when(enseignantService.getByClub(1L)).thenReturn(List.of(new EnseignantDTO()));

        ResponseEntity<List<EnseignantDTO>> response = controller.getByClub(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void create_resoutRoleEtClubDepuisLAppelant() {
        when(utilisateurService.getUtilisateurEntityByEmail("admin@test.com"))
                .thenReturn(Optional.of(user(5L, Role.ADMIN)));
        EnseignantDTO created = new EnseignantDTO();
        created.setId(10L);
        when(enseignantService.create(any(EnseignantDTO.class), eq("ADMIN"), eq(5L))).thenReturn(created);

        ResponseEntity<EnseignantDTO> response = controller.create(new EnseignantDTO(), auth("admin@test.com"));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(10L, response.getBody().getId());
    }

    @Test
    void create_authentificationNulle_passeRoleEtClubNuls() {
        EnseignantDTO created = new EnseignantDTO();
        created.setId(11L);
        when(enseignantService.create(any(EnseignantDTO.class), isNull(), isNull())).thenReturn(created);

        ResponseEntity<EnseignantDTO> response = controller.create(new EnseignantDTO(), null);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    void create_utilisateurIntrouvable_passeRoleEtClubNuls() {
        when(utilisateurService.getUtilisateurEntityByEmail("inconnu@test.com")).thenReturn(Optional.empty());
        EnseignantDTO created = new EnseignantDTO();
        when(enseignantService.create(any(EnseignantDTO.class), isNull(), isNull())).thenReturn(created);

        ResponseEntity<EnseignantDTO> response = controller.create(new EnseignantDTO(), auth("inconnu@test.com"));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    void update_trouve_retourneOk() {
        when(utilisateurService.getUtilisateurEntityByEmail("admin@test.com"))
                .thenReturn(Optional.of(user(5L, Role.ADMIN)));
        EnseignantDTO updated = new EnseignantDTO();
        when(enseignantService.update(eq(1L), any(EnseignantDTO.class), eq("ADMIN"), eq(5L)))
                .thenReturn(Optional.of(updated));

        ResponseEntity<EnseignantDTO> response = controller.update(1L, new EnseignantDTO(), auth("admin@test.com"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void update_absent_retourneNotFound() {
        when(utilisateurService.getUtilisateurEntityByEmail("admin@test.com"))
                .thenReturn(Optional.of(user(5L, Role.ADMIN)));
        when(enseignantService.update(eq(1L), any(EnseignantDTO.class), eq("ADMIN"), eq(5L)))
                .thenReturn(Optional.empty());

        ResponseEntity<EnseignantDTO> response = controller.update(1L, new EnseignantDTO(), auth("admin@test.com"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void delete_succes_retourneNoContent() {
        when(utilisateurService.getUtilisateurEntityByEmail("super@test.com"))
                .thenReturn(Optional.of(user(null, Role.SUPER_ADMIN)));
        when(enseignantService.delete(1L, "SUPER_ADMIN", null)).thenReturn(true);

        ResponseEntity<Void> response = controller.delete(1L, auth("super@test.com"));

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void delete_echec_retourneNotFound() {
        when(utilisateurService.getUtilisateurEntityByEmail("admin@test.com"))
                .thenReturn(Optional.of(user(5L, Role.ADMIN)));
        when(enseignantService.delete(1L, "ADMIN", 5L)).thenReturn(false);

        ResponseEntity<Void> response = controller.delete(1L, auth("admin@test.com"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void uploadPhoto_succes_retourneOkAvecPath() throws Exception {
        when(fileUploadService.uploadFile(any(MultipartFile.class), eq("enseignants")))
                .thenReturn("enseignants/photo.jpg");
        MockMultipartFile file = new MockMultipartFile("photo", "photo.jpg", "image/jpeg", new byte[]{1});

        ResponseEntity<Map<String, String>> response = controller.uploadPhoto(file);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("enseignants/photo.jpg", response.getBody().get("path"));
    }

    @Test
    void uploadPhoto_erreur_retourneBadRequestSansBody() throws Exception {
        when(fileUploadService.uploadFile(any(MultipartFile.class), eq("enseignants")))
                .thenThrow(new RuntimeException("échec"));
        MockMultipartFile file = new MockMultipartFile("photo", "photo.jpg", "image/jpeg", new byte[]{1});

        ResponseEntity<Map<String, String>> response = controller.uploadPhoto(file);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}
