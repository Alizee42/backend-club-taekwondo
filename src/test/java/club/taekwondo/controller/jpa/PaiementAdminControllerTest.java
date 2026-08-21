package club.taekwondo.controller.jpa;

import club.taekwondo.dto.AnnulationRequestDTO;
import club.taekwondo.dto.PaiementDTO;
import club.taekwondo.dto.PaiementRequestDTO;
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

    private PaiementDTO paiement(Long id) {
        PaiementDTO dto = new PaiementDTO();
        dto.setId(id);
        return dto;
    }

    // ---- getPaiementsByClub ----

    @Test
    void getPaiementsByClub_superAdmin_retourneOk() {
        when(paiementAccessService.hasAnyRole(any(), org.mockito.ArgumentMatchers.eq("SUPER_ADMIN"))).thenReturn(true);
        when(paiementService.getPaiementsByClubId(10L)).thenReturn(List.of(paiement(1L)));

        ResponseEntity<List<PaiementDTO>> response = controller.getPaiementsByClub(10L, auth("super@test.com", "SUPER_ADMIN"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getPaiementsByClub_adminMemeClub_retourneOk() {
        when(paiementAccessService.hasAnyRole(any(), org.mockito.ArgumentMatchers.eq("SUPER_ADMIN"))).thenReturn(false);
        when(paiementAccessService.requireAuthenticatedUser(any())).thenReturn(user(1L, 10L));
        when(paiementService.getPaiementsByClubId(10L)).thenReturn(List.of(paiement(1L)));

        ResponseEntity<List<PaiementDTO>> response = controller.getPaiementsByClub(10L, auth("admin@test.com", "ADMIN"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getPaiementsByClub_adminAutreClub_retourneForbidden() {
        when(paiementAccessService.hasAnyRole(any(), org.mockito.ArgumentMatchers.eq("SUPER_ADMIN"))).thenReturn(false);
        when(paiementAccessService.requireAuthenticatedUser(any())).thenReturn(user(1L, 99L));

        ResponseEntity<List<PaiementDTO>> response = controller.getPaiementsByClub(10L, auth("admin@test.com", "ADMIN"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    // ---- ajouterPaiementManuel ----

    @Test
    void ajouterPaiementManuel_succes_retourneCreated() {
        PaiementDTO created = paiement(5L);
        when(paiementService.ajouterPaiementsCompletFromDto(any(PaiementRequestDTO.class), org.mockito.ArgumentMatchers.isNull()))
                .thenReturn(List.of(created));

        Map<String, Object> body = Map.of(
                "utilisateurId", 1,
                "membreId", 2,
                "type", "cotisation unique",
                "modePaiement", "carte",
                "montantTotal", 100.0
        );

        ResponseEntity<Map<String, Object>> response = controller.ajouterPaiementManuel(body);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(5L, response.getBody().get("paiementId"));
    }

    @Test
    void ajouterPaiementManuel_aucunPaiementCree_retourneInternalServerError() {
        when(paiementService.ajouterPaiementsCompletFromDto(any(PaiementRequestDTO.class), org.mockito.ArgumentMatchers.isNull()))
                .thenReturn(List.of());

        ResponseEntity<Map<String, Object>> response = controller.ajouterPaiementManuel(Map.of("utilisateurId", 1));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void ajouterPaiementManuel_erreurValidation_retourneBadRequest() {
        when(paiementService.ajouterPaiementsCompletFromDto(any(PaiementRequestDTO.class), org.mockito.ArgumentMatchers.isNull()))
                .thenThrow(new IllegalArgumentException("montant invalide"));

        ResponseEntity<Map<String, Object>> response = controller.ajouterPaiementManuel(Map.of("utilisateurId", 1));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void ajouterPaiementManuel_avecEcheances_retourneCreated() {
        PaiementDTO created = paiement(7L);
        when(paiementService.ajouterPaiementsCompletFromDto(any(PaiementRequestDTO.class), org.mockito.ArgumentMatchers.isNull()))
                .thenReturn(List.of(created));

        Map<String, Object> echeance = Map.of("dateEcheance", "2026-01-01", "montant", 50.0);
        Map<String, Object> body = Map.of(
                "parentId", 1,
                "typePaiement", "echelonne",
                "modePaiement", "virement",
                "echeances", List.of(echeance)
        );

        ResponseEntity<Map<String, Object>> response = controller.ajouterPaiementManuel(body);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    void ajouterPaiementManuel_erreurInattendue_retourneInternalServerError() {
        when(paiementService.ajouterPaiementsCompletFromDto(any(PaiementRequestDTO.class), org.mockito.ArgumentMatchers.isNull()))
                .thenThrow(new RuntimeException("boom"));

        ResponseEntity<Map<String, Object>> response = controller.ajouterPaiementManuel(Map.of("utilisateurId", 1));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    // ---- ajouterPaiementCompletJson ----

    @Test
    void ajouterPaiementCompletJson_succes_retourneCreated() {
        PaiementDTO created = paiement(9L);
        when(paiementService.ajouterPaiementsCompletFromDto(any(PaiementRequestDTO.class), org.mockito.ArgumentMatchers.isNull()))
                .thenReturn(List.of(created));

        PaiementRequestDTO req = new PaiementRequestDTO();
        req.setUtilisateurId(1L);
        req.setMontantTotal(100.0);

        ResponseEntity<Map<String, Object>> response = controller.ajouterPaiementCompletJson(req);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(9L, response.getBody().get("paiementId"));
    }

    @Test
    void ajouterPaiementCompletJson_erreurValidation_retourneBadRequest() {
        when(paiementService.ajouterPaiementsCompletFromDto(any(PaiementRequestDTO.class), org.mockito.ArgumentMatchers.isNull()))
                .thenThrow(new IllegalArgumentException("invalide"));

        ResponseEntity<Map<String, Object>> response = controller.ajouterPaiementCompletJson(new PaiementRequestDTO());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void ajouterPaiementCompletJson_erreurInattendue_retourneInternalServerError() {
        when(paiementService.ajouterPaiementsCompletFromDto(any(PaiementRequestDTO.class), org.mockito.ArgumentMatchers.isNull()))
                .thenThrow(new RuntimeException("boom"));

        ResponseEntity<Map<String, Object>> response = controller.ajouterPaiementCompletJson(new PaiementRequestDTO());

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    // ---- ajouterPaiementCompletMultipart ----

    @Test
    void ajouterPaiementCompletMultipart_succes_retourneCreated() throws Exception {
        PaiementDTO created = paiement(11L);
        when(paiementService.ajouterPaiementsCompletFromDto(any(PaiementRequestDTO.class), org.mockito.ArgumentMatchers.isNull()))
                .thenReturn(List.of(created));

        ResponseEntity<Map<String, Object>> response = controller.ajouterPaiementCompletMultipart(
                "Dupont", "Jean", "jean@test.com", "unique", "150.0", "cb", "2026-01-15", null, null);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    void ajouterPaiementCompletMultipart_montantInvalide_retourneBadRequest() {
        ResponseEntity<Map<String, Object>> response = controller.ajouterPaiementCompletMultipart(
                "Dupont", "Jean", "jean@test.com", "unique", "pas-un-nombre", "cb", "2026-01-15", null, null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    // ---- deletePaiement ----

    @Test
    void deletePaiement_succes_retourneOk() {
        ResponseEntity<Void> response = controller.deletePaiement(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void deletePaiement_absent_retourneNotFound() {
        org.mockito.Mockito.doThrow(new RuntimeException("absent")).when(paiementService).delete(1L);

        ResponseEntity<Void> response = controller.deletePaiement(1L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // ---- backfill ----

    @Test
    void backfillClub_delegueAuService() {
        when(paiementService.backfillClubForExistingPaiements()).thenReturn(5);

        ResponseEntity<Map<String, Object>> response = controller.backfillClub();

        assertEquals(5, response.getBody().get("updated"));
    }

    @Test
    void backfillCharge_delegueAuService() {
        when(paiementService.backfillStripeChargeInfoForExistingPaiements()).thenReturn(3);

        ResponseEntity<Map<String, Object>> response = controller.backfillCharge();

        assertEquals(3, response.getBody().get("updated"));
    }

    // ---- annulerPaiement ----

    @Test
    void annulerPaiement_delegueAuService() {
        AnnulationRequestDTO dto = new AnnulationRequestDTO();
        dto.setMotif("erreur de saisie");
        when(paiementService.annulerPaiement(1L, dto)).thenReturn(paiement(1L));

        ResponseEntity<PaiementDTO> response = controller.annulerPaiement(1L, dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
