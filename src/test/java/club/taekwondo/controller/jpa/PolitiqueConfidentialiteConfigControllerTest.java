package club.taekwondo.controller.jpa;

import club.taekwondo.dto.PolitiqueConfidentialiteConfigDto;
import club.taekwondo.entity.jpa.Club;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.service.jpa.PolitiqueConfidentialiteConfigService;
import club.taekwondo.service.jpa.UtilisateurService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PolitiqueConfidentialiteConfigControllerTest {

    @Mock
    private PolitiqueConfidentialiteConfigService politiqueConfidentialiteConfigService;

    @Mock
    private UtilisateurService utilisateurService;

    private PolitiqueConfidentialiteConfigController controller;

    @BeforeEach
    void setUp() {
        controller = new PolitiqueConfidentialiteConfigController();
        ReflectionTestUtils.setField(controller, "politiqueConfidentialiteConfigService", politiqueConfidentialiteConfigService);
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
        ResponseEntity<PolitiqueConfidentialiteConfigDto> response = controller.get(null);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void get_avecClubId_retourneOk() {
        when(politiqueConfidentialiteConfigService.get(1L)).thenReturn(new PolitiqueConfidentialiteConfigDto());

        ResponseEntity<PolitiqueConfidentialiteConfigDto> response = controller.get(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void update_adminAvecClub_forceLeClubDeLAppelantMemeSiDtoEnFournitUnAutre() {
        when(utilisateurService.findByEmail("admin@test.com")).thenReturn(Optional.of(user(5L)));
        when(politiqueConfidentialiteConfigService.update(anyLong(), any(PolitiqueConfidentialiteConfigDto.class)))
                .thenReturn(new PolitiqueConfidentialiteConfigDto());

        PolitiqueConfidentialiteConfigDto dto = new PolitiqueConfidentialiteConfigDto();
        dto.setClubId(999L);
        controller.update(dto, auth("admin@test.com"));

        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
        verify(politiqueConfidentialiteConfigService).update(captor.capture(), any(PolitiqueConfidentialiteConfigDto.class));
        assertEquals(5L, captor.getValue());
    }

    @Test
    void update_superAdminSansClubPropre_utiliseLeClubIdFourni() {
        when(utilisateurService.findByEmail("super@test.com")).thenReturn(Optional.of(user(null)));
        when(politiqueConfidentialiteConfigService.update(anyLong(), any(PolitiqueConfidentialiteConfigDto.class)))
                .thenReturn(new PolitiqueConfidentialiteConfigDto());

        PolitiqueConfidentialiteConfigDto dto = new PolitiqueConfidentialiteConfigDto();
        dto.setClubId(7L);
        controller.update(dto, auth("super@test.com"));

        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
        verify(politiqueConfidentialiteConfigService).update(captor.capture(), any(PolitiqueConfidentialiteConfigDto.class));
        assertEquals(7L, captor.getValue());
    }

    @Test
    void update_aucunClubResolvable_retourneBadRequest() {
        when(utilisateurService.findByEmail("inconnu@test.com")).thenReturn(Optional.empty());

        PolitiqueConfidentialiteConfigDto dto = new PolitiqueConfidentialiteConfigDto();
        dto.setClubId(null);
        ResponseEntity<PolitiqueConfidentialiteConfigDto> response = controller.update(dto, auth("inconnu@test.com"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    private static <T> T any(Class<T> clazz) {
        return org.mockito.ArgumentMatchers.any(clazz);
    }
}
