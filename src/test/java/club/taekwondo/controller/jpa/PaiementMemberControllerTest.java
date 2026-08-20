package club.taekwondo.controller.jpa;

import club.taekwondo.dto.PaiementDTO;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.enums.Role;
import club.taekwondo.service.jpa.PaiementAccessService;
import club.taekwondo.service.jpa.PaiementService;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaiementMemberControllerTest {

    @Mock
    private PaiementService paiementService;

    @Mock
    private PaiementAccessService paiementAccessService;

    private PaiementMemberController controller;

    @BeforeEach
    void setUp() {
        controller = new PaiementMemberController(paiementService, paiementAccessService);
    }

    @Test
    void getPaiementsPourMembreConnecte_noAttachedMembre_returnsEmptyList() {
        Authentication auth = auth("membre@test.com", "ROLE_MEMBRE");
        Utilisateur membre = user(1L, "membre@test.com", Role.MEMBRE);
        when(paiementAccessService.requireAuthenticatedUser(auth)).thenReturn(membre);
        when(paiementAccessService.getAttachedMembreId(membre)).thenReturn(null);

        ResponseEntity<List<PaiementDTO>> response = controller.getPaiementsPourMembreConnecte(auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0, response.getBody().size());
        verify(paiementService, never()).getPaiementsParMembre(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void getPaiementsPourMembreConnecte_mergesResultsWithoutDuplicates() {
        Authentication auth = auth("membre@test.com", "ROLE_MEMBRE");
        Utilisateur membre = user(1L, "membre@test.com", Role.MEMBRE);
        when(paiementAccessService.requireAuthenticatedUser(auth)).thenReturn(membre);
        when(paiementAccessService.getAttachedMembreId(membre)).thenReturn(10L);

        PaiementDTO shared = new PaiementDTO();
        shared.setId(100L);
        PaiementDTO onlyByUser = new PaiementDTO();
        onlyByUser.setId(200L);

        when(paiementService.getPaiementsParMembre(10L)).thenReturn(List.of(shared));
        when(paiementService.getPaiementsParUtilisateur(1L)).thenReturn(List.of(shared, onlyByUser));

        ResponseEntity<List<PaiementDTO>> response = controller.getPaiementsPourMembreConnecte(auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
    }

    @Test
    void ajouterPaiementPourMembre_parentWithoutMembreId_returnsBadRequest() {
        Authentication auth = auth("parent@test.com", "ROLE_PARENT");
        Utilisateur parent = user(2L, "parent@test.com", Role.PARENT);
        when(paiementAccessService.requireAuthenticatedUser(auth)).thenReturn(parent);

        ResponseEntity<Map<String, Object>> response = controller.ajouterPaiementPourMembre(auth, Map.of(
                "montantTotal", 50.0,
                "type", "unique",
                "modePaiement", "especes"
        ));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("membreId requis pour un parent", response.getBody().get("error"));
        verify(paiementAccessService, never()).assertParentOwnsMember(any(), any());
    }

    @Test
    void ajouterPaiementPourMembre_parentForForeignChild_isRejected() {
        Authentication auth = auth("parent@test.com", "ROLE_PARENT");
        Utilisateur parent = user(2L, "parent@test.com", Role.PARENT);
        when(paiementAccessService.requireAuthenticatedUser(auth)).thenReturn(parent);
        org.mockito.Mockito.doThrow(new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN))
                .when(paiementAccessService).assertParentOwnsMember(parent, 999L);

        ResponseEntity<Map<String, Object>> response = controller.ajouterPaiementPourMembre(auth, Map.of(
                "montantTotal", 50.0,
                "type", "unique",
                "modePaiement", "especes",
                "membreId", 999
        ));

        // Le controller catch toute Exception et repond 400, pas de propagation du statut FORBIDDEN.
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(paiementService, never()).ajouterPaiementsCompletFromDto(any(), any());
    }

    @Test
    void ajouterPaiementPourMembre_missingMontant_returnsBadRequest() {
        Authentication auth = auth("membre@test.com", "ROLE_MEMBRE");
        Utilisateur membre = user(1L, "membre@test.com", Role.MEMBRE);
        when(paiementAccessService.requireAuthenticatedUser(auth)).thenReturn(membre);

        ResponseEntity<Map<String, Object>> response = controller.ajouterPaiementPourMembre(auth, Map.of(
                "type", "unique",
                "modePaiement", "especes"
        ));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("montantTotal invalide", response.getBody().get("error"));
    }

    @Test
    void ajouterPaiementPourMembre_memberAutoAttachesOwnMembreId() {
        Authentication auth = auth("membre@test.com", "ROLE_MEMBRE");
        Utilisateur membre = user(1L, "membre@test.com", Role.MEMBRE);
        when(paiementAccessService.requireAuthenticatedUser(auth)).thenReturn(membre);
        when(paiementAccessService.getAttachedMembreId(membre)).thenReturn(55L);

        PaiementDTO created = new PaiementDTO();
        created.setId(7L);
        when(paiementService.ajouterPaiementsCompletFromDto(any(), any())).thenReturn(List.of(created));

        ResponseEntity<Map<String, Object>> response = controller.ajouterPaiementPourMembre(auth, Map.of(
                "montantTotal", 100.0,
                "type", "unique",
                "modePaiement", "especes"
        ));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(7L, response.getBody().get("paiementId"));
    }

    private Authentication auth(String email, String authority) {
        TestingAuthenticationToken token = new TestingAuthenticationToken(email, null, authority);
        token.setAuthenticated(true);
        return token;
    }

    private Utilisateur user(Long id, String email, Role role) {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(id);
        utilisateur.setEmail(email);
        utilisateur.setRole(role);
        return utilisateur;
    }
}
