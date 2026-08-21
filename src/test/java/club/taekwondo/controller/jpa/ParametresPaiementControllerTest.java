package club.taekwondo.controller.jpa;

import club.taekwondo.dto.ParametresPaiementDTO;
import club.taekwondo.service.jpa.ParametresPaiementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParametresPaiementControllerTest {

    @Mock
    private ParametresPaiementService parametresPaiementService;

    private ParametresPaiementController controller;

    @BeforeEach
    void setUp() {
        controller = new ParametresPaiementController(parametresPaiementService);
    }

    private Authentication auth(String email) {
        return new TestingAuthenticationToken(email, null, "ROLE_ADMIN");
    }

    @Test
    void getParametresPaiementPublic_delegueAuService() {
        when(parametresPaiementService.getParametresPaiementByClub(1L)).thenReturn(new ParametresPaiementDTO());

        ResponseEntity<ParametresPaiementDTO> response = controller.getParametresPaiementPublic(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getParametresPaiementByClub_avecAuth_delegueAuService() {
        when(parametresPaiementService.getParametresPaiementByClub(1L)).thenReturn(new ParametresPaiementDTO());

        ResponseEntity<ParametresPaiementDTO> response =
                controller.getParametresPaiementByClub(1L, auth("admin@test.com"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getParametresPaiementByClub_authNulle_neLeveAucuneException() {
        when(parametresPaiementService.getParametresPaiementByClub(1L)).thenReturn(new ParametresPaiementDTO());

        ResponseEntity<ParametresPaiementDTO> response = controller.getParametresPaiementByClub(1L, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void updateParametresPaiementByClub_appelleLeServiceEtRetourneLePayload() {
        ParametresPaiementDTO dto = new ParametresPaiementDTO();

        ResponseEntity<ParametresPaiementDTO> response =
                controller.updateParametresPaiementByClub(1L, auth("admin@test.com"), dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(dto, response.getBody());
        verify(parametresPaiementService).updateParametresPaiement(1L, dto);
    }
}
