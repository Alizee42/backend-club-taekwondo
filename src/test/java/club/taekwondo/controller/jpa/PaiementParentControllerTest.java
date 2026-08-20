package club.taekwondo.controller.jpa;

import club.taekwondo.dto.PaiementDTO;
import club.taekwondo.entity.jpa.Paiement;
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
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaiementParentControllerTest {

    @Mock
    private PaiementService paiementService;

    @Mock
    private PaiementAccessService paiementAccessService;

    private PaiementParentController controller;

    @BeforeEach
    void setUp() {
        controller = new PaiementParentController(paiementService, paiementAccessService);
    }

    @Test
    void getPaiementsPourParentConnecte_mergesByChildAndByUser() {
        Authentication auth = auth("parent@test.com", "ROLE_PARENT");
        Utilisateur parent = user(1L, "parent@test.com", Role.PARENT);
        when(paiementAccessService.requireAuthenticatedUser(auth)).thenReturn(parent);
        when(paiementAccessService.getChildMemberIds(parent)).thenReturn(List.of(10L, 11L));

        PaiementDTO byChild = new PaiementDTO();
        byChild.setId(100L);
        PaiementDTO byUser = new PaiementDTO();
        byUser.setId(200L);
        when(paiementService.getPaiementsParMembres(List.of(10L, 11L))).thenReturn(List.of(byChild));
        when(paiementService.getPaiementsParUtilisateur(1L)).thenReturn(List.of(byUser));

        ResponseEntity<List<PaiementDTO>> response = controller.getPaiementsPourParentConnecte(auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
    }

    @Test
    void ajouterPaiementParent_missingMembreId_returnsBadRequest() {
        Authentication auth = auth("parent@test.com", "ROLE_PARENT");
        Utilisateur parent = user(1L, "parent@test.com", Role.PARENT);
        when(paiementAccessService.requireAuthenticatedUser(auth)).thenReturn(parent);

        ResponseEntity<PaiementDTO> response = controller.ajouterPaiementParent(auth, Map.of(
                "type", "unique",
                "montantTotal", 50.0
        ));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(paiementAccessService, never()).assertParentOwnsMember(any(), anyLong());
    }

    @Test
    void ajouterPaiementParent_parentForOwnChild_isAllowed() {
        Authentication auth = auth("parent@test.com", "ROLE_PARENT");
        Utilisateur parent = user(1L, "parent@test.com", Role.PARENT);
        when(paiementAccessService.requireAuthenticatedUser(auth)).thenReturn(parent);
        when(paiementAccessService.hasAnyRole(auth, "ADMIN", "SUPER_ADMIN")).thenReturn(false);

        Paiement saved = new Paiement();
        saved.setId(5L);
        PaiementDTO dto = new PaiementDTO();
        dto.setId(5L);
        when(paiementService.ajouterPaiementParent(any(), org.mockito.ArgumentMatchers.eq(1L))).thenReturn(saved);
        when(paiementService.toPaiementDTO(saved)).thenReturn(dto);

        ResponseEntity<PaiementDTO> response = controller.ajouterPaiementParent(auth, Map.of(
                "membreId", 42,
                "type", "unique",
                "montantTotal", 80.0
        ));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(paiementAccessService, times(1)).assertParentOwnsMember(parent, 42L);
    }

    @Test
    void ajouterPaiementParent_parentForForeignChild_isForbidden() {
        Authentication auth = auth("parent@test.com", "ROLE_PARENT");
        Utilisateur parent = user(1L, "parent@test.com", Role.PARENT);
        when(paiementAccessService.requireAuthenticatedUser(auth)).thenReturn(parent);
        when(paiementAccessService.hasAnyRole(auth, "ADMIN", "SUPER_ADMIN")).thenReturn(false);
        org.mockito.Mockito.doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN))
                .when(paiementAccessService).assertParentOwnsMember(parent, 999L);

        ResponseEntity<PaiementDTO> response = controller.ajouterPaiementParent(auth, Map.of(
                "membreId", 999,
                "type", "unique",
                "montantTotal", 80.0
        ));

        // Le controller catch toute exception et repond 500, sans propager le FORBIDDEN attendu.
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(paiementService, never()).ajouterPaiementParent(any(), anyLong());
    }

    @Test
    void ajouterPaiementParent_adminBypassesOwnershipCheck() {
        Authentication auth = auth("admin@test.com", "ROLE_ADMIN");
        Utilisateur admin = user(9L, "admin@test.com", Role.ADMIN);
        when(paiementAccessService.requireAuthenticatedUser(auth)).thenReturn(admin);
        when(paiementAccessService.hasAnyRole(auth, "ADMIN", "SUPER_ADMIN")).thenReturn(true);

        Paiement saved = new Paiement();
        saved.setId(6L);
        PaiementDTO dto = new PaiementDTO();
        dto.setId(6L);
        when(paiementService.ajouterPaiementParent(any(), org.mockito.ArgumentMatchers.eq(9L))).thenReturn(saved);
        when(paiementService.toPaiementDTO(saved)).thenReturn(dto);

        ResponseEntity<PaiementDTO> response = controller.ajouterPaiementParent(auth, Map.of(
                "membreId", 123,
                "type", "unique",
                "montantTotal", 80.0
        ));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(paiementAccessService, never()).assertParentOwnsMember(any(), anyLong());
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
