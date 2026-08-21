package club.taekwondo.controller.mongo;

import club.taekwondo.dto.GalerieDTO;
import club.taekwondo.entity.jpa.Club;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.enums.Role;
import club.taekwondo.service.jpa.GalerieService;
import club.taekwondo.service.jpa.UtilisateurService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GalerieControllerTest {

    @Mock
    private GalerieService galerieService;

    @Mock
    private UtilisateurService utilisateurService;

    private GalerieController controller;

    @BeforeEach
    void setUp() {
        controller = new GalerieController(galerieService, utilisateurService);
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
    void getByClubId_delegueAuService() {
        when(galerieService.getByClubId(10L)).thenReturn(List.of(new GalerieDTO()));

        List<GalerieDTO> result = controller.getByClubId(10L);

        assertEquals(1, result.size());
    }

    @Test
    void getAll_delegueAuService() {
        when(galerieService.getAll()).thenReturn(List.of(new GalerieDTO(), new GalerieDTO()));

        List<GalerieDTO> result = controller.getAll();

        assertEquals(2, result.size());
    }

    @Test
    void getById_trouve_retourneOk() {
        when(galerieService.getById("1")).thenReturn(Optional.of(new GalerieDTO()));

        ResponseEntity<GalerieDTO> response = controller.getById("1");

        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void getById_absent_retourneNotFound() {
        when(galerieService.getById("1")).thenReturn(Optional.empty());

        ResponseEntity<GalerieDTO> response = controller.getById("1");

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void createMultipart_resoutUtilisateurEtDelegue() {
        when(utilisateurService.findByEmail("admin@test.com")).thenReturn(Optional.of(user(10L, Role.ADMIN)));
        GalerieDTO created = new GalerieDTO();
        when(galerieService.createMultipart(eq("Titre"), eq("Desc"), eq(10L), any(MultipartFile.class), eq("ADMIN"), eq(10L)))
                .thenReturn(created);
        MockMultipartFile image = new MockMultipartFile("image", "img.jpg", "image/jpeg", new byte[]{1});

        GalerieDTO result = controller.createMultipart("Titre", "Desc", 10L, image, auth("admin@test.com"));

        assertEquals(created, result);
    }

    @Test
    void createMultipart_utilisateurSansClub_passeClubIdNull() {
        when(utilisateurService.findByEmail("super@test.com")).thenReturn(Optional.of(user(null, Role.SUPER_ADMIN)));
        GalerieDTO created = new GalerieDTO();
        when(galerieService.createMultipart(eq("Titre"), eq("Desc"), eq(10L), any(MultipartFile.class), eq("SUPER_ADMIN"), eq(null)))
                .thenReturn(created);
        MockMultipartFile image = new MockMultipartFile("image", "img.jpg", "image/jpeg", new byte[]{1});

        GalerieDTO result = controller.createMultipart("Titre", "Desc", 10L, image, auth("super@test.com"));

        assertEquals(created, result);
    }

    @Test
    void createMultipart_utilisateurIntrouvable_leveIllegalState() {
        when(utilisateurService.findByEmail("inconnu@test.com")).thenReturn(Optional.empty());
        MockMultipartFile image = new MockMultipartFile("image", "img.jpg", "image/jpeg", new byte[]{1});

        assertThrows(IllegalStateException.class,
                () -> controller.createMultipart("Titre", "Desc", 10L, image, auth("inconnu@test.com")));
    }

    @Test
    void update_trouve_retourneOk() {
        when(utilisateurService.findByEmail("admin@test.com")).thenReturn(Optional.of(user(10L, Role.ADMIN)));
        GalerieDTO dto = new GalerieDTO();
        when(galerieService.update(eq("1"), any(GalerieDTO.class), eq("ADMIN"), eq(10L))).thenReturn(dto);

        ResponseEntity<GalerieDTO> response = controller.update("1", dto, auth("admin@test.com"));

        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void update_absent_retourneNotFound() {
        when(utilisateurService.findByEmail("admin@test.com")).thenReturn(Optional.of(user(10L, Role.ADMIN)));
        when(galerieService.update(eq("1"), any(GalerieDTO.class), eq("ADMIN"), eq(10L))).thenReturn(null);

        ResponseEntity<GalerieDTO> response = controller.update("1", new GalerieDTO(), auth("admin@test.com"));

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void delete_appelleLeServiceEtRetourneNoContent() {
        ResponseEntity<Void> response = controller.delete("1");

        assertEquals(204, response.getStatusCode().value());
        verify(galerieService).delete("1");
    }
}
