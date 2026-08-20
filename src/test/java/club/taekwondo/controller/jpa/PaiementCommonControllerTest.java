package club.taekwondo.controller.jpa;

import club.taekwondo.dto.DashboardStatsDTO;
import club.taekwondo.dto.PaiementDTO;
import club.taekwondo.entity.jpa.Club;
import club.taekwondo.entity.jpa.Echeance;
import club.taekwondo.entity.jpa.Paiement;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.security.JwtUtil;
import club.taekwondo.service.StripeService;
import club.taekwondo.service.jpa.PaiementAccessService;
import club.taekwondo.service.jpa.PaiementService;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaiementCommonControllerTest {

    @Mock
    private PaiementService paiementService;

    @Mock
    private PaiementAccessService paiementAccessService;

    @Mock
    private UtilisateurService utilisateurService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private StripeService stripeService;

    private PaiementCommonController controller;

    @BeforeEach
    void setUp() {
        controller = new PaiementCommonController(paiementService, paiementAccessService, utilisateurService, jwtUtil, stripeService);
    }

    @Test
    void payerEcheance_notFound_throws404() {
        when(paiementService.getById(1L)).thenReturn(Optional.empty());
        Authentication auth = auth("admin@test.com", "ROLE_ADMIN");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> controller.payerEcheance(1L, List.of(), auth));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void payerEcheance_userWithoutAccess_isRejected() {
        Paiement paiement = paiement(1L, "en attente", null, 100.0, 100.0, 0.0);
        when(paiementService.getById(1L)).thenReturn(Optional.of(paiement));
        Authentication auth = auth("membre@test.com", "ROLE_MEMBRE");
        org.mockito.Mockito.doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN))
                .when(paiementAccessService).assertCanAccessPaiement(auth, paiement);

        assertThrows(ResponseStatusException.class, () -> controller.payerEcheance(1L, List.of(), auth));
        verify(paiementService, never()).persisterEtat(any());
    }

    @Test
    void payerEcheance_noEcheances_returnsBadRequest() {
        Paiement paiement = paiement(1L, "en attente", null, 100.0, 100.0, 0.0);
        paiement.setEcheances(new ArrayList<>());
        when(paiementService.getById(1L)).thenReturn(Optional.of(paiement));
        Authentication auth = auth("admin@test.com", "ROLE_ADMIN");

        ResponseEntity<PaiementDTO> response = controller.payerEcheance(1L, List.of(), auth);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void payerEcheance_paysOneOfTwo_updatesMontantsAndKeepsEnAttente() {
        Echeance e1 = echeance(11L, 50.0, "en attente");
        Echeance e2 = echeance(12L, 50.0, "en attente");
        Paiement paiement = paiement(1L, "en attente", null, 100.0, 100.0, 0.0);
        paiement.setEcheances(new ArrayList<>(List.of(e1, e2)));
        paiement.setEcheancesRestantes(2);
        when(paiementService.getById(1L)).thenReturn(Optional.of(paiement));
        when(paiementService.persisterEtat(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paiementService.toPaiementDTO(any())).thenReturn(new PaiementDTO());
        Authentication auth = auth("admin@test.com", "ROLE_ADMIN");

        ResponseEntity<PaiementDTO> response = controller.payerEcheance(1L, List.of(Map.of("id", 11L)), auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("payé", e1.getStatut());
        assertEquals("en attente", e2.getStatut());
        assertEquals(50.0, paiement.getMontantPaye());
        assertEquals(50.0, paiement.getMontantRestant());
        assertEquals(1, paiement.getEcheancesRestantes());
        assertEquals("en attente", paiement.getStatut());
    }

    @Test
    void payerEcheance_paysAllRemaining_marksPaiementAsPaye() {
        Echeance e1 = echeance(11L, 50.0, "payé");
        Echeance e2 = echeance(12L, 50.0, "en attente");
        Paiement paiement = paiement(1L, "en attente", null, 100.0, 50.0, 50.0);
        paiement.setEcheances(new ArrayList<>(List.of(e1, e2)));
        paiement.setEcheancesRestantes(1);
        when(paiementService.getById(1L)).thenReturn(Optional.of(paiement));
        when(paiementService.persisterEtat(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paiementService.toPaiementDTO(any())).thenReturn(new PaiementDTO());
        Authentication auth = auth("admin@test.com", "ROLE_ADMIN");

        controller.payerEcheance(1L, List.of(Map.of("id", 12L)), auth);

        assertEquals("payé", paiement.getStatut());
        assertEquals(0.0, paiement.getMontantRestant());
        assertEquals(0, paiement.getEcheancesRestantes());
    }

    @Test
    void payerEcheance_unknownEcheanceId_returnsBadRequest() {
        Echeance e1 = echeance(11L, 50.0, "en attente");
        Paiement paiement = paiement(1L, "en attente", null, 50.0, 50.0, 0.0);
        paiement.setEcheances(new ArrayList<>(List.of(e1)));
        when(paiementService.getById(1L)).thenReturn(Optional.of(paiement));
        Authentication auth = auth("admin@test.com", "ROLE_ADMIN");

        ResponseEntity<PaiementDTO> response = controller.payerEcheance(1L, List.of(Map.of("id", 999L)), auth);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(paiementService, never()).persisterEtat(any());
    }

    @Test
    void getDashboardStats_adminOnlyScopedToOwnClub() {
        Authentication auth = auth("admin@test.com", "ROLE_ADMIN");
        Utilisateur admin = new Utilisateur();
        admin.setId(1L);
        Club club = new Club();
        club.setId(3L);
        admin.setClub(club);
        when(paiementAccessService.hasAnyRole(auth, "ADMIN")).thenReturn(true);
        when(paiementAccessService.hasAnyRole(auth, "SUPER_ADMIN")).thenReturn(false);
        when(paiementAccessService.requireAuthenticatedUser(auth)).thenReturn(admin);
        when(paiementService.buildDashboardStats(3L)).thenReturn(new DashboardStatsDTO(0.0, 0.0, 0.0, 0.0, List.of(), List.of()));

        ResponseEntity<DashboardStatsDTO> response = controller.getDashboardStats(auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(paiementService).buildDashboardStats(3L);
    }

    @Test
    void getDashboardStats_superAdminSeesAllClubs() {
        Authentication auth = auth("super@test.com", "ROLE_SUPER_ADMIN");
        when(paiementAccessService.hasAnyRole(auth, "ADMIN")).thenReturn(false);
        when(paiementService.buildDashboardStats(null)).thenReturn(new DashboardStatsDTO(0.0, 0.0, 0.0, 0.0, List.of(), List.of()));

        ResponseEntity<DashboardStatsDTO> response = controller.getDashboardStats(auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(paiementAccessService, never()).requireAuthenticatedUser(any());
        verify(paiementService).buildDashboardStats(null);
    }

    private Authentication auth(String email, String authority) {
        TestingAuthenticationToken token = new TestingAuthenticationToken(email, null, authority);
        token.setAuthenticated(true);
        return token;
    }

    private Paiement paiement(Long id, String statut, Club club, Double total, Double restant, Double paye) {
        Paiement p = new Paiement();
        p.setId(id);
        p.setStatut(statut);
        p.setMontantTotal(total);
        p.setMontantRestant(restant);
        p.setMontantPaye(paye);
        return p;
    }

    private Echeance echeance(Long id, Double montant, String statut) {
        Echeance e = new Echeance();
        e.setId(id);
        e.setMontant(montant);
        e.setStatut(statut);
        return e;
    }
}
