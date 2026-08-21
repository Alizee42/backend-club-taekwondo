package club.taekwondo.controller.jpa;

import club.taekwondo.dto.ParametresPaiementDTO;
import club.taekwondo.entity.jpa.Club;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.repository.jpa.UtilisateurRepository;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParametresPaiementControllerTest {

    @Mock
    private ParametresPaiementService parametresPaiementService;

    @Mock
    private UtilisateurRepository utilisateurRepository;

    private ParametresPaiementController controller;

    @BeforeEach
    void setUp() {
        controller = new ParametresPaiementController(parametresPaiementService, utilisateurRepository);
    }

    private Authentication auth(String email, String role) {
        return new TestingAuthenticationToken(email, null, role);
    }

    private Utilisateur utilisateurDuClub(Long clubId) {
        Club club = new Club();
        club.setId(clubId);
        Utilisateur u = new Utilisateur();
        u.setEmail("admin@test.com");
        u.setClub(club);
        return u;
    }

    @Test
    void getParametresPaiementPublic_delegueAuService() {
        when(parametresPaiementService.getParametresPaiementByClub(1L)).thenReturn(new ParametresPaiementDTO());

        ResponseEntity<ParametresPaiementDTO> response = controller.getParametresPaiementPublic(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getParametresPaiementByClub_adminProprietaireDuClub_delegueAuService() {
        when(utilisateurRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(utilisateurDuClub(1L)));
        when(parametresPaiementService.getParametresPaiementByClub(1L)).thenReturn(new ParametresPaiementDTO());

        ResponseEntity<ParametresPaiementDTO> response =
                controller.getParametresPaiementByClub(1L, auth("admin@test.com", "ROLE_ADMIN"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getParametresPaiementByClub_adminDunAutreClub_retourne403() {
        when(utilisateurRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(utilisateurDuClub(2L)));

        ResponseEntity<ParametresPaiementDTO> response =
                controller.getParametresPaiementByClub(1L, auth("admin@test.com", "ROLE_ADMIN"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(parametresPaiementService, never()).getParametresPaiementByClub(anyLong());
    }

    @Test
    void getParametresPaiementByClub_superAdmin_neVerifiePasLeClub() {
        when(parametresPaiementService.getParametresPaiementByClub(1L)).thenReturn(new ParametresPaiementDTO());

        ResponseEntity<ParametresPaiementDTO> response =
                controller.getParametresPaiementByClub(1L, auth("superadmin@test.com", "ROLE_SUPER_ADMIN"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getParametresPaiementByClub_authNulle_retourne401() {
        ResponseEntity<ParametresPaiementDTO> response = controller.getParametresPaiementByClub(1L, null);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(parametresPaiementService, never()).getParametresPaiementByClub(anyLong());
    }

    @Test
    void updateParametresPaiementByClub_adminProprietaireDuClub_appelleLeServiceEtRetourneLePayload() {
        ParametresPaiementDTO dto = new ParametresPaiementDTO();
        when(utilisateurRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(utilisateurDuClub(1L)));

        ResponseEntity<ParametresPaiementDTO> response =
                controller.updateParametresPaiementByClub(1L, auth("admin@test.com", "ROLE_ADMIN"), dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(dto, response.getBody());
        verify(parametresPaiementService).updateParametresPaiement(1L, dto);
    }

    @Test
    void updateParametresPaiementByClub_adminDunAutreClub_retourne403EtNeModifieRien() {
        ParametresPaiementDTO dto = new ParametresPaiementDTO();
        when(utilisateurRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(utilisateurDuClub(2L)));

        ResponseEntity<ParametresPaiementDTO> response =
                controller.updateParametresPaiementByClub(1L, auth("admin@test.com", "ROLE_ADMIN"), dto);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(parametresPaiementService, never()).updateParametresPaiement(anyLong(), org.mockito.ArgumentMatchers.any());
    }
}
