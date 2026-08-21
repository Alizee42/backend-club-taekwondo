package club.taekwondo.service.jpa;

import club.taekwondo.entity.jpa.Membre;
import club.taekwondo.entity.jpa.Paiement;
import club.taekwondo.entity.jpa.Utilisateur;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaiementAccessServiceTest {

    @Mock
    private UtilisateurService utilisateurService;

    @Mock
    private MembreService membreService;

    private PaiementAccessService accessService;

    @BeforeEach
    void setUp() {
        accessService = new PaiementAccessService(utilisateurService, membreService);
    }

    private Authentication auth(String email, String authority) {
        TestingAuthenticationToken token = new TestingAuthenticationToken(email, null, authority);
        token.setAuthenticated(true);
        return token;
    }

    private Utilisateur user(Long id, String email) {
        Utilisateur u = new Utilisateur();
        u.setId(id);
        u.setEmail(email);
        return u;
    }

    private Membre membre(Long id) {
        Membre m = new Membre();
        m.setId(id);
        return m;
    }

    private Paiement paiement(Utilisateur payeur, Membre membre) {
        Paiement p = new Paiement();
        p.setUtilisateur(payeur);
        p.setMembre(membre);
        return p;
    }

    @Test
    void requireAuthenticatedUser_authenticationNull_leve401() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> accessService.requireAuthenticatedUser(null));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void requireAuthenticatedUser_nomVide_leve401() {
        Authentication authentication = new TestingAuthenticationToken("", null, "ROLE_PARENT");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> accessService.requireAuthenticatedUser(authentication));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void requireAuthenticatedUser_emailInconnuEnBase_leve401() {
        Authentication authentication = auth("inconnu@test.com", "ROLE_PARENT");
        when(utilisateurService.findByEmail("inconnu@test.com")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> accessService.requireAuthenticatedUser(authentication));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void requireAuthenticatedUser_trouve_retourneLUtilisateur() {
        Authentication authentication = auth("parent@test.com", "ROLE_PARENT");
        Utilisateur u = user(1L, "parent@test.com");
        when(utilisateurService.findByEmail("parent@test.com")).thenReturn(Optional.of(u));

        assertEquals(u, accessService.requireAuthenticatedUser(authentication));
    }

    @Test
    void hasAnyRole_authenticationNull_retourneFalse() {
        assertFalse(accessService.hasAnyRole(null, "ADMIN"));
    }

    @Test
    void hasAnyRole_roleCorrespondant_retourneTrue() {
        Authentication authentication = auth("admin@test.com", "ROLE_ADMIN");
        assertTrue(accessService.hasAnyRole(authentication, "ADMIN", "SUPER_ADMIN"));
    }

    @Test
    void hasAnyRole_aucunRoleCorrespondant_retourneFalse() {
        Authentication authentication = auth("parent@test.com", "ROLE_PARENT");
        assertFalse(accessService.hasAnyRole(authentication, "ADMIN", "SUPER_ADMIN"));
    }

    @Test
    void assertParentOwnsMember_membreEtranger_leve403() {
        Utilisateur parent = user(1L, "parent@test.com");
        when(membreService.getEnfantsDuParent(1L)).thenReturn(List.of(membre(10L)));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> accessService.assertParentOwnsMember(parent, 999L));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void assertParentOwnsMember_membrePossede_neLevePasException() {
        Utilisateur parent = user(1L, "parent@test.com");
        when(membreService.getEnfantsDuParent(1L)).thenReturn(List.of(membre(10L)));

        accessService.assertParentOwnsMember(parent, 10L);
        // pas d'exception = succes
    }

    @Test
    void assertCanAccessPaiement_adminBypassSansVerifierPropriete() {
        Authentication authentication = auth("admin@test.com", "ROLE_ADMIN");
        Paiement p = paiement(user(2L, "autre@test.com"), membre(20L));

        accessService.assertCanAccessPaiement(authentication, p);
        // pas d'exception, aucun appel a utilisateurService/membreService necessaire
    }

    @Test
    void assertCanAccessPaiement_superAdminBypass() {
        Authentication authentication = auth("super@test.com", "ROLE_SUPER_ADMIN");
        Paiement p = paiement(user(2L, "autre@test.com"), membre(20L));

        accessService.assertCanAccessPaiement(authentication, p);
    }

    @Test
    void assertCanAccessPaiement_proprietaireDirectDuPaiement_estAutorise() {
        Authentication authentication = auth("parent@test.com", "ROLE_PARENT");
        Utilisateur payeur = user(1L, "parent@test.com");
        Paiement p = paiement(payeur, null);
        when(utilisateurService.findByEmail("parent@test.com")).thenReturn(Optional.of(payeur));

        accessService.assertCanAccessPaiement(authentication, p);
    }

    @Test
    void assertCanAccessPaiement_membreRattacheAuCompte_estAutorise() {
        Authentication authentication = auth("membre@test.com", "ROLE_MEMBRE");
        Utilisateur membreUser = user(5L, "membre@test.com");
        Paiement p = paiement(user(1L, "autre@test.com"), membre(30L));
        when(utilisateurService.findByEmail("membre@test.com")).thenReturn(Optional.of(membreUser));
        when(membreService.getMembreEntityByIdUtilisateur(5L)).thenReturn(Optional.of(membre(30L)));
        lenient().when(membreService.getEnfantsDuParent(5L)).thenReturn(List.of());

        accessService.assertCanAccessPaiement(authentication, p);
    }

    @Test
    void assertCanAccessPaiement_enfantDuParentConnecte_estAutorise() {
        Authentication authentication = auth("parent@test.com", "ROLE_PARENT");
        Utilisateur parent = user(1L, "parent@test.com");
        Paiement p = paiement(user(2L, "autre@test.com"), membre(30L));
        when(utilisateurService.findByEmail("parent@test.com")).thenReturn(Optional.of(parent));
        when(membreService.getMembreEntityByIdUtilisateur(1L)).thenReturn(Optional.empty());
        when(membreService.getEnfantsDuParent(1L)).thenReturn(List.of(membre(30L)));

        accessService.assertCanAccessPaiement(authentication, p);
    }

    @Test
    void assertCanAccessPaiement_aucunLienAvecLePaiement_leve403() {
        Authentication authentication = auth("parent@test.com", "ROLE_PARENT");
        Utilisateur parent = user(1L, "parent@test.com");
        Paiement p = paiement(user(2L, "autre@test.com"), membre(30L));
        when(utilisateurService.findByEmail("parent@test.com")).thenReturn(Optional.of(parent));
        when(membreService.getMembreEntityByIdUtilisateur(1L)).thenReturn(Optional.empty());
        when(membreService.getEnfantsDuParent(1L)).thenReturn(List.of(membre(999L)));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> accessService.assertCanAccessPaiement(authentication, p));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void assertCanAccessPaiement_paiementSansMembreEtSansProprietaireCorrespondant_leve403() {
        Authentication authentication = auth("parent@test.com", "ROLE_PARENT");
        Utilisateur parent = user(1L, "parent@test.com");
        Paiement p = paiement(user(2L, "autre@test.com"), null);
        when(utilisateurService.findByEmail("parent@test.com")).thenReturn(Optional.of(parent));
        when(membreService.getMembreEntityByIdUtilisateur(1L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> accessService.assertCanAccessPaiement(authentication, p));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void getChildMemberIds_retourneLesIdsDesEnfants() {
        Utilisateur parent = user(1L, "parent@test.com");
        when(membreService.getEnfantsDuParent(1L)).thenReturn(List.of(membre(10L), membre(11L)));

        assertEquals(List.of(10L, 11L), accessService.getChildMemberIds(parent));
    }

    @Test
    void getAttachedMembreId_utilisateurSansMembreAssocie_retourneNull() {
        Utilisateur u = user(1L, "membre@test.com");
        when(membreService.getMembreEntityByIdUtilisateur(1L)).thenReturn(Optional.empty());

        assertEquals(null, accessService.getAttachedMembreId(u));
    }
}
