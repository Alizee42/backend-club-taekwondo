package club.taekwondo.controller.jpa;

import club.taekwondo.dto.AboutConfigDto;
import club.taekwondo.entity.jpa.Club;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.service.jpa.AboutConfigService;
import club.taekwondo.service.jpa.UtilisateurService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AboutConfigControllerTest {

    @Mock
    private AboutConfigService aboutConfigService;

    @Mock
    private UtilisateurService utilisateurService;

    private AboutConfigController controller;

    @BeforeEach
    void setUp() {
        controller = new AboutConfigController();
        ReflectionTestUtils.setField(controller, "aboutConfigService", aboutConfigService);
        ReflectionTestUtils.setField(controller, "utilisateurService", utilisateurService);
    }

    private Authentication auth(String email) {
        return new TestingAuthenticationToken(email, null, "ROLE_ADMIN");
    }

    private Utilisateur user(Long clubId) {
        Utilisateur u = new Utilisateur();
        if (clubId != null) {
            Club club = new Club();
            club.setId(clubId);
            u.setClub(club);
        }
        return u;
    }

    @Test
    void get_sansClubId_retourneBadRequest() {
        ResponseEntity<AboutConfigDto> response = controller.get(null);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void get_avecClubId_retourneOk() {
        when(aboutConfigService.get(1L)).thenReturn(new AboutConfigDto());

        ResponseEntity<AboutConfigDto> response = controller.get(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void update_adminAvecClub_forceLeClubDeLAppelant() {
        when(utilisateurService.findByEmail("admin@test.com")).thenReturn(Optional.of(user(5L)));
        when(aboutConfigService.update(anyLong(), any(AboutConfigDto.class))).thenReturn(new AboutConfigDto());

        AboutConfigDto dto = new AboutConfigDto();
        dto.setClubId(999L);
        controller.update(dto, auth("admin@test.com"));

        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
        verify(aboutConfigService).update(captor.capture(), any(AboutConfigDto.class));
        assertEquals(5L, captor.getValue());
    }

    @Test
    void update_aucunClubResolvable_retourneBadRequest() {
        when(utilisateurService.findByEmail("inconnu@test.com")).thenReturn(Optional.empty());

        AboutConfigDto dto = new AboutConfigDto();
        dto.setClubId(null);
        ResponseEntity<AboutConfigDto> response = controller.update(dto, auth("inconnu@test.com"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void uploadImage_succes_retourneOk() throws IOException {
        when(utilisateurService.findByEmail("admin@test.com")).thenReturn(Optional.of(user(3L)));
        when(aboutConfigService.uploadImage(eq(3L), any(MultipartFile.class))).thenReturn(new AboutConfigDto());
        MockMultipartFile file = new MockMultipartFile("image", "photo.png", "image/png", new byte[]{1});

        ResponseEntity<?> response = controller.uploadImage(file, null, auth("admin@test.com"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void uploadImage_aucunClubResolvable_retourneBadRequest() throws IOException {
        when(utilisateurService.findByEmail("inconnu@test.com")).thenReturn(Optional.empty());
        MockMultipartFile file = new MockMultipartFile("image", "photo.png", "image/png", new byte[]{1});

        ResponseEntity<?> response = controller.uploadImage(file, null, auth("inconnu@test.com"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void uploadImage_erreurService_retourneInternalServerError() throws IOException {
        when(utilisateurService.findByEmail("admin@test.com")).thenReturn(Optional.of(user(3L)));
        when(aboutConfigService.uploadImage(eq(3L), any(MultipartFile.class)))
                .thenThrow(new IOException("Disque plein"));
        MockMultipartFile file = new MockMultipartFile("image", "photo.png", "image/png", new byte[]{1});

        ResponseEntity<?> response = controller.uploadImage(file, null, auth("admin@test.com"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    private static Long eq(Long value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }
}
