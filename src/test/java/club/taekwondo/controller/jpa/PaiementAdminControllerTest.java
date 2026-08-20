package club.taekwondo.controller.jpa;

import club.taekwondo.dto.AnnulationRequestDTO;
import club.taekwondo.dto.PaiementDTO;
import club.taekwondo.entity.jpa.Club;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.service.jpa.PaiementAccessService;
import club.taekwondo.service.jpa.PaiementService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaiementAdminControllerTest {

    @Mock
    private PaiementService paiementService;

    @Mock
    private PaiementAccessService paiementAccessService;

    private PaiementAdminController controller;

    @BeforeEach
    void setUp() {
        controller = new PaiementAdminController(paiementService, paiementAccessService, new ObjectMapper());
    }

    @Test
    void getPaiementsByClub_adminCannotReadOtherClubPaiements() {
        Authentication auth = auth("admin@test.com", "ROLE_ADMIN");
        Utilisateur admin = user(1L, "admin@test.com", 1L);
        when(paiementAccessService.hasAnyRole(auth, "SUPER_ADMIN")).thenReturn(false);
        when(paiementAccessService.requireAuthenticatedUser(auth)).thenReturn(admin);

        ResponseEntity<List<PaiementDTO>> response = controller.getPaiementsByClub(2L, auth);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(paiementService, never()).getPaiementsByClubId(anyLong());
    }

    @Test
    void getPaiementsByClub_adminCanReadOwnClubPaiements() {
        Authentication auth = auth("admin@test.com", "ROLE_ADMIN");
        Utilisateur admin = user(1L, "admin@test.com", 1L);
        when(paiementAccessService.hasAnyRole(auth, "SUPER_ADMIN")).thenReturn(false);
        when(paiementAccessService.requireAuthenticatedUser(auth)).thenReturn(admin);
        when(paiementService.getPaiementsByClubId(1L)).thenReturn(List.of(new PaiementDTO()));

        ResponseEntity<List<PaiementDTO>> response = controller.getPaiementsByClub(1L, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getPaiementsByClub_superAdminBypassesClubCheck() {
        Authentication auth = auth("super@test.com", "ROLE_SUPER_ADMIN");
        when(paiementAccessService.hasAnyRole(auth, "SUPER_ADMIN")).thenReturn(true);
        when(paiementService.getPaiementsByClubId(99L)).thenReturn(List.of());

        ResponseEntity<List<PaiementDTO>> response = controller.getPaiementsByClub(99L, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(paiementAccessService, never()).requireAuthenticatedUser(any());
    }

    @Test
    void ajouterPaiementManuel_missingMontantStillDelegatesToService() {
        PaiementDTO created = new PaiementDTO();
        created.setId(42L);
        when(paiementService.ajouterPaiementsCompletFromDto(any(), eq(null)))
                .thenReturn(List.of(created));

        ResponseEntity<Map<String, Object>> response = controller.ajouterPaiementManuel(Map.of(
                "utilisateurId", 5,
                "membreId", 7,
                "type", "unique",
                "modePaiement", "especes",
                "montantTotal", 100.0
        ));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(42L, response.getBody().get("paiementId"));
    }

    @Test
    void ajouterPaiementManuel_serviceThrowsIllegalArgument_returnsBadRequest() {
        when(paiementService.ajouterPaiementsCompletFromDto(any(), eq(null)))
                .thenThrow(new IllegalArgumentException("Montant invalide"));

        ResponseEntity<Map<String, Object>> response = controller.ajouterPaiementManuel(Map.of(
                "utilisateurId", 5,
                "type", "unique",
                "montantTotal", -1.0
        ));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Montant invalide", response.getBody().get("message"));
    }

    @Test
    void ajouterPaiementManuel_emptyResultFromService_returns500() {
        when(paiementService.ajouterPaiementsCompletFromDto(any(), eq(null)))
                .thenReturn(List.of());

        ResponseEntity<Map<String, Object>> response = controller.ajouterPaiementManuel(Map.of(
                "utilisateurId", 5,
                "type", "unique",
                "montantTotal", 50.0
        ));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void deletePaiement_notFound_returns404() {
        org.mockito.Mockito.doThrow(new RuntimeException("introuvable"))
                .when(paiementService).delete(999L);

        ResponseEntity<Void> response = controller.deletePaiement(999L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void deletePaiement_success() {
        ResponseEntity<Void> response = controller.deletePaiement(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(paiementService, times(1)).delete(1L);
    }

    @Test
    void annulerPaiement_delegatesToService() {
        AnnulationRequestDTO req = new AnnulationRequestDTO();
        PaiementDTO annule = new PaiementDTO();
        annule.setId(3L);
        annule.setStatut("annulé");
        when(paiementService.annulerPaiement(3L, req)).thenReturn(annule);

        ResponseEntity<PaiementDTO> response = controller.annulerPaiement(3L, req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("annulé", response.getBody().getStatut());
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
}
