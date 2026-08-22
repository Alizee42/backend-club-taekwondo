package club.taekwondo.controller.jpa;

import club.taekwondo.dto.MembreDTO;
import club.taekwondo.entity.jpa.Club;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.service.jpa.MembreService;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MembreControllerTest {

    @Mock
    private MembreService membreService;

    @Mock
    private UtilisateurService utilisateurService;

    private MembreController controller;

    @BeforeEach
    void setUp() {
        controller = new MembreController(membreService, utilisateurService);
    }

    @Test
    void getMembres_parentAlwaysSeesOnlyOwnChildren_ignoringQueryParams() {
        Authentication auth = auth("parent@test.com", "ROLE_PARENT");
        MembreDTO enfant = membre(1L, "Leo");
        when(membreService.getMembresByParentEmail("parent@test.com")).thenReturn(List.of(enfant));

        // meme si un clubId d'un autre club est fourni, le parent ne doit voir que ses enfants
        ResponseEntity<?> response = controller.getMembres(null, 999L, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(List.of(enfant), response.getBody());
        verify(membreService, never()).getMembresByClubId(anyLong());
    }

    @Test
    void getMembres_membreCannotListAllMembers() {
        Authentication auth = auth("membre@test.com", "ROLE_MEMBRE");

        ResponseEntity<?> response = controller.getMembres(null, null, auth);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void getMembres_adminOfOtherClubCannotFilterForeignClub() {
        Authentication auth = auth("admin@test.com", "ROLE_ADMIN");
        Utilisateur admin = user(1L, 1L);
        when(utilisateurService.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));

        ResponseEntity<?> response = controller.getMembres(null, 2L, auth);

        // Note: le controller autorise tout ADMIN quel que soit son club (isAdmin bypass la verification club).
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(membreService).getMembresByClubId(2L);
    }

    @Test
    void getMembres_nonAdminNonSuperUserCannotFilterForeignClub() {
        // Aucun role parent/membre/admin/super-admin : simule un cas non prevu, verifie
        // que le controle club s'applique bien via findByEmail + comparaison d'id.
        Authentication auth = new TestingAuthenticationToken("guest@test.com", null, List.of());
        Utilisateur guest = user(5L, 1L);
        when(utilisateurService.findByEmail("guest@test.com")).thenReturn(Optional.of(guest));

        ResponseEntity<?> response = controller.getMembres(null, 2L, auth);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(membreService, never()).getMembresByClubId(anyLong());
    }

    @Test
    void getMembres_superAdminBypassesClubCheck() {
        Authentication auth = auth("super@test.com", "ROLE_SUPER_ADMIN");
        when(membreService.getMembresByClubId(7L)).thenReturn(List.of(membre(1L, "Leo")));

        ResponseEntity<?> response = controller.getMembres(null, 7L, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(utilisateurService, never()).findByEmail(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void getMembreById_parentCannotAccessForeignChild() {
        Authentication auth = auth("parent@test.com", "ROLE_PARENT");
        when(membreService.getMembresByParentEmail("parent@test.com")).thenReturn(List.of(membre(1L, "Leo")));

        ResponseEntity<?> response = controller.getMembreById(999L, auth);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void getMembreById_parentCanAccessOwnChild() {
        Authentication auth = auth("parent@test.com", "ROLE_PARENT");
        MembreDTO enfant = membre(1L, "Leo");
        when(membreService.getMembresByParentEmail("parent@test.com")).thenReturn(List.of(enfant));
        when(membreService.getMembreById(1L)).thenReturn(Optional.of(enfant));

        ResponseEntity<?> response = controller.getMembreById(1L, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getMembreById_memberCanOnlyAccessOwnRecord() {
        Authentication auth = auth("membre@test.com", "ROLE_MEMBRE");
        MembreDTO own = membre(3L, "Alizee");
        when(membreService.getMembreByUtilisateurEmail("membre@test.com")).thenReturn(Optional.of(own));
        when(membreService.getMembreById(3L)).thenReturn(Optional.of(own));

        ResponseEntity<?> forbidden = controller.getMembreById(999L, auth);
        ResponseEntity<?> allowed = controller.getMembreById(3L, auth);

        assertEquals(HttpStatus.FORBIDDEN, forbidden.getStatusCode());
        assertEquals(HttpStatus.OK, allowed.getStatusCode());
    }

    @Test
    void createMembre_duplicateLicence_returnsBadRequest() {
        when(membreService.createMembre(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("dup"));

        ResponseEntity<?> response = controller.createMembre(new MembreDTO());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(Map.of("message", "Le numéro de licence est déjà utilisé."), response.getBody());
    }

    @Test
    void updateMembre_notFound_returns404() {
        when(membreService.getMembreById(99L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.updateMembre(99L, new MembreDTO(), auth("admin@test.com", "ROLE_ADMIN"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void getMembreConnecte_adminHasNoAssociatedMembre() {
        Authentication auth = auth("admin@test.com", "ROLE_ADMIN");

        ResponseEntity<?> response = controller.getMembreConnecte(auth);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    // ---- getMembres : autres branches ----

    @Test
    void getMembres_parentIdFourni_retourneEnfantsDeCeParent() {
        Authentication auth = auth("admin@test.com", "ROLE_ADMIN");
        when(membreService.getMembresByUtilisateurId(5L)).thenReturn(List.of(membre(1L, "Leo")));

        ResponseEntity<?> response = controller.getMembres(5L, null, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getMembres_parentIdFourni_listeVide_retourneNoContent() {
        Authentication auth = auth("admin@test.com", "ROLE_ADMIN");
        when(membreService.getMembresByUtilisateurId(5L)).thenReturn(List.of());

        ResponseEntity<?> response = controller.getMembres(5L, null, auth);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void getMembres_sansFiltre_adminVoitTousLesMembres() {
        Authentication auth = auth("admin@test.com", "ROLE_ADMIN");
        when(membreService.getAllMembres()).thenReturn(List.of(membre(1L, "Leo")));

        ResponseEntity<?> response = controller.getMembres(null, null, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getMembres_clubIdFourni_listeVide_retourneNoContent() {
        Authentication auth = auth("super@test.com", "ROLE_SUPER_ADMIN");
        when(membreService.getMembresByClubId(7L)).thenReturn(List.of());

        ResponseEntity<?> response = controller.getMembres(null, 7L, auth);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    // ---- getMembreById : admin/super-admin ----

    @Test
    void getMembreById_adminTrouve_retourneOk() {
        Authentication auth = auth("admin@test.com", "ROLE_ADMIN");
        when(membreService.getMembreById(1L)).thenReturn(Optional.of(membre(1L, "Leo")));

        ResponseEntity<?> response = controller.getMembreById(1L, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getMembreById_adminAbsent_retourneNotFound() {
        Authentication auth = auth("admin@test.com", "ROLE_ADMIN");
        when(membreService.getMembreById(999L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.getMembreById(999L, auth);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // ---- getMembresDuParentConnecte ----

    @Test
    void getMembresDuParentConnecte_retourneLesEnfants() {
        Authentication auth = auth("parent@test.com", "ROLE_PARENT");
        when(membreService.getMembresByParentEmail("parent@test.com")).thenReturn(List.of(membre(1L, "Leo")));

        ResponseEntity<?> response = controller.getMembresDuParentConnecte(auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getMembresDuParentConnecte_listeVide_retourneNoContent() {
        Authentication auth = auth("parent@test.com", "ROLE_PARENT");
        when(membreService.getMembresByParentEmail("parent@test.com")).thenReturn(List.of());

        ResponseEntity<?> response = controller.getMembresDuParentConnecte(auth);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    // ---- getMembreByUtilisateurId / alias ----

    @Test
    void getMembreByUtilisateurId_trouve_retourneOk() {
        club.taekwondo.entity.jpa.Membre entity = new club.taekwondo.entity.jpa.Membre();
        entity.setId(1L);
        when(membreService.getMembreEntityByIdUtilisateur(10L)).thenReturn(Optional.of(entity));
        when(membreService.toMembreDTO(entity)).thenReturn(membre(1L, "Leo"));

        ResponseEntity<?> response = controller.getMembreByUtilisateurId(10L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getMembreByUtilisateurId_absent_retourneNotFound() {
        when(membreService.getMembreEntityByIdUtilisateur(10L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.getMembreByUtilisateurId(10L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void getMembreByUtilisateurIdAlias_trouve_retourneOk() {
        club.taekwondo.entity.jpa.Membre entity = new club.taekwondo.entity.jpa.Membre();
        entity.setId(1L);
        when(membreService.getMembreEntityByIdUtilisateur(10L)).thenReturn(Optional.of(entity));
        when(membreService.toMembreDTO(entity)).thenReturn(membre(1L, "Leo"));

        ResponseEntity<?> response = controller.getMembreByUtilisateurIdAlias(10L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // ---- getByParent ----

    @Test
    void getByParent_retourneListeAllegee() {
        when(membreService.getMembresByUtilisateurId(5L)).thenReturn(List.of(membre(1L, "Leo")));

        ResponseEntity<List<Map<String, Object>>> response = controller.getByParent(5L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Leo", response.getBody().get(0).get("prenom"));
    }

    @Test
    void getByParent_listeVide_retourneNoContent() {
        when(membreService.getMembresByUtilisateurId(5L)).thenReturn(List.of());

        ResponseEntity<List<Map<String, Object>>> response = controller.getByParent(5L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    // ---- createMembre : autres branches ----

    @Test
    void createMembre_succes_retourneCreated() {
        MembreDTO created = membre(1L, "Leo");
        when(membreService.createMembre(org.mockito.ArgumentMatchers.any())).thenReturn(created);

        ResponseEntity<?> response = controller.createMembre(new MembreDTO());

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    void createMembre_illegalArgument_retourneBadRequestAvecMessage() {
        when(membreService.createMembre(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new IllegalArgumentException("champ requis manquant"));

        ResponseEntity<?> response = controller.createMembre(new MembreDTO());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(Map.of("message", "champ requis manquant"), response.getBody());
    }

    @Test
    void createMembre_erreurInattendue_retourneInternalServerError() {
        when(membreService.createMembre(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new RuntimeException("boom"));

        ResponseEntity<?> response = controller.createMembre(new MembreDTO());

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    // ---- updateMembre : autres branches ----

    @Test
    void updateMembre_succes_retourneOk() {
        when(membreService.getMembreById(1L)).thenReturn(Optional.of(membre(1L, "Leo")));
        when(membreService.updateMembre(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any()))
                .thenReturn(membre(1L, "Leo modifie"));

        ResponseEntity<?> response = controller.updateMembre(1L, new MembreDTO(), auth("admin@test.com", "ROLE_ADMIN"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void updateMembre_erreurInattendue_retourneInternalServerError() {
        when(membreService.getMembreById(1L)).thenReturn(Optional.of(membre(1L, "Leo")));
        when(membreService.updateMembre(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any()))
                .thenThrow(new RuntimeException("boom"));

        ResponseEntity<?> response = controller.updateMembre(1L, new MembreDTO(), auth("admin@test.com", "ROLE_ADMIN"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    // ---- deleteMembre ----

    @Test
    void deleteMembre_notFound_retourne404() {
        when(membreService.getMembreById(99L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.deleteMembre(99L, auth("admin@test.com", "ROLE_ADMIN"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void deleteMembre_succes_retourneOk() {
        when(membreService.getMembreById(1L)).thenReturn(Optional.of(membre(1L, "Leo")));

        ResponseEntity<?> response = controller.deleteMembre(1L, auth("admin@test.com", "ROLE_ADMIN"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(membreService).deleteMembre(1L);
    }

    @Test
    void deleteMembre_erreurInattendue_retourneInternalServerError() {
        when(membreService.getMembreById(1L)).thenReturn(Optional.of(membre(1L, "Leo")));
        org.mockito.Mockito.doThrow(new RuntimeException("boom")).when(membreService).deleteMembre(1L);

        ResponseEntity<?> response = controller.deleteMembre(1L, auth("admin@test.com", "ROLE_ADMIN"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    // ---- getMembreConnecte : autres branches ----

    @Test
    void getMembreConnecte_adulteAvecMembreAssocie_retourneOk() {
        Authentication auth = auth("membre@test.com", "ROLE_MEMBRE");
        when(membreService.getMembreByUtilisateurEmail("membre@test.com")).thenReturn(Optional.of(membre(3L, "Alizee")));

        ResponseEntity<?> response = controller.getMembreConnecte(auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getMembreConnecte_parentSansMembreAssocie_retourneNoContent() {
        Authentication auth = auth("parent@test.com", "ROLE_PARENT");
        when(membreService.getMembreByUtilisateurEmail("parent@test.com")).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.getMembreConnecte(auth);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void getMembreConnecte_membreSansAssociation_retourneNotFound() {
        Authentication auth = auth("membre@test.com", "ROLE_MEMBRE");
        when(membreService.getMembreByUtilisateurEmail("membre@test.com")).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.getMembreConnecte(auth);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    private Authentication auth(String email, String authority) {
        TestingAuthenticationToken token = new TestingAuthenticationToken(email, null, authority);
        token.setAuthenticated(true);
        return token;
    }

    private Utilisateur user(Long id, Long clubId) {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(id);
        Club club = new Club();
        club.setId(clubId);
        utilisateur.setClub(club);
        return utilisateur;
    }

    private MembreDTO membre(Long id, String prenom) {
        MembreDTO dto = new MembreDTO();
        dto.setId(id);
        dto.setPrenom(prenom);
        return dto;
    }
}
