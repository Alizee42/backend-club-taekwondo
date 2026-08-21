package club.taekwondo.controller.jpa;

import club.taekwondo.dto.LigneCommandeDTO;
import club.taekwondo.entity.jpa.Club;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.service.jpa.LigneCommandeService;
import club.taekwondo.service.jpa.UtilisateurService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LigneCommandeControllerTest {

    @Mock
    private LigneCommandeService ligneCommandeService;

    @Mock
    private UtilisateurService utilisateurService;

    private LigneCommandeController controller;

    @BeforeEach
    void setUp() {
        controller = new LigneCommandeController(ligneCommandeService, utilisateurService);
    }

    private Authentication auth(String email, String role) {
        return new TestingAuthenticationToken(email, null, "ROLE_" + role);
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
    void getAllLignesCommande_delegueAuService() {
        when(ligneCommandeService.getAllLignesCommande()).thenReturn(List.of(new LigneCommandeDTO()));

        ResponseEntity<List<LigneCommandeDTO>> response = controller.getAllLignesCommande();

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getLignesCommandeByClub_superAdmin_bypassLaVerificationDeClub() {
        when(ligneCommandeService.getLignesCommandeByClubId(1L)).thenReturn(List.of(new LigneCommandeDTO()));

        ResponseEntity<List<LigneCommandeDTO>> response =
                controller.getLignesCommandeByClub(1L, auth("super@test.com", "SUPER_ADMIN"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getLignesCommandeByClub_adminPropreClub_retourneOk() {
        when(utilisateurService.findByEmail("admin@test.com")).thenReturn(Optional.of(user(5L)));
        when(ligneCommandeService.getLignesCommandeByClubId(5L)).thenReturn(List.of(new LigneCommandeDTO()));

        ResponseEntity<List<LigneCommandeDTO>> response =
                controller.getLignesCommandeByClub(5L, auth("admin@test.com", "ADMIN"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getLignesCommandeByClub_adminAutreClub_retourneForbidden() {
        when(utilisateurService.findByEmail("admin@test.com")).thenReturn(Optional.of(user(5L)));

        ResponseEntity<List<LigneCommandeDTO>> response =
                controller.getLignesCommandeByClub(99L, auth("admin@test.com", "ADMIN"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void getLignesCommandeByClub_adminSansClub_retourneForbidden() {
        when(utilisateurService.findByEmail("admin@test.com")).thenReturn(Optional.of(user(null)));

        ResponseEntity<List<LigneCommandeDTO>> response =
                controller.getLignesCommandeByClub(5L, auth("admin@test.com", "ADMIN"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void getLignesCommandeByClub_utilisateurIntrouvable_leveUnauthorized() {
        when(utilisateurService.findByEmail("inconnu@test.com")).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
                () -> controller.getLignesCommandeByClub(5L, auth("inconnu@test.com", "ADMIN")));
    }

    @Test
    void getLigneCommandeById_trouve_retourneOk() {
        when(ligneCommandeService.getLigneCommandeById(1L)).thenReturn(Optional.of(new LigneCommandeDTO()));

        ResponseEntity<LigneCommandeDTO> response = controller.getLigneCommandeById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getLigneCommandeById_absent_retourneNotFound() {
        when(ligneCommandeService.getLigneCommandeById(1L)).thenReturn(Optional.empty());

        ResponseEntity<LigneCommandeDTO> response = controller.getLigneCommandeById(1L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void createLigneCommande_retourneCreated() {
        LigneCommandeDTO dto = new LigneCommandeDTO();
        when(ligneCommandeService.createLigneCommande(dto)).thenReturn(dto);

        ResponseEntity<LigneCommandeDTO> response = controller.createLigneCommande(dto);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    void updateLigneCommande_succes_retourneOk() {
        LigneCommandeDTO dto = new LigneCommandeDTO();
        when(ligneCommandeService.updateLigneCommande(anyLong(), any(LigneCommandeDTO.class))).thenReturn(dto);

        ResponseEntity<LigneCommandeDTO> response = controller.updateLigneCommande(1L, dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void updateLigneCommande_erreur_retourneNotFound() {
        LigneCommandeDTO dto = new LigneCommandeDTO();
        when(ligneCommandeService.updateLigneCommande(anyLong(), any(LigneCommandeDTO.class)))
                .thenThrow(new RuntimeException("absente"));

        ResponseEntity<LigneCommandeDTO> response = controller.updateLigneCommande(1L, dto);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void deleteLigneCommande_retourneNoContent() {
        ResponseEntity<Void> response = controller.deleteLigneCommande(1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(ligneCommandeService).deleteLigneCommande(1L);
    }
}
