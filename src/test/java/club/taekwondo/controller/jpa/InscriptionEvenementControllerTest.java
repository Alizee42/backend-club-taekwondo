package club.taekwondo.controller.jpa;

import club.taekwondo.dto.InscriptionEvenementDTO;
import club.taekwondo.dto.InscriptionRequestDTO;
import club.taekwondo.dto.MembreDTO;
import club.taekwondo.entity.jpa.Club;
import club.taekwondo.entity.jpa.Membre;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.service.jpa.InscriptionEvenementService;
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
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InscriptionEvenementControllerTest {

    @Mock
    private InscriptionEvenementService inscriptionService;

    @Mock
    private MembreService membreService;

    @Mock
    private UtilisateurService utilisateurService;

    private InscriptionEvenementController controller;

    @BeforeEach
    void setUp() {
        controller = new InscriptionEvenementController(inscriptionService, membreService, utilisateurService);
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

    private Membre membre(Long id, Long clubId) {
        Membre m = new Membre();
        m.setId(id);
        if (clubId != null) {
            Club club = new Club();
            club.setId(clubId);
            m.setClub(club);
        }
        return m;
    }

    @Test
    void getAllInscriptions_delegueAuService() {
        when(inscriptionService.getAllInscriptions()).thenReturn(List.of(new InscriptionEvenementDTO()));

        ResponseEntity<List<InscriptionEvenementDTO>> response = controller.getAllInscriptions();

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getInscriptionsByEvenement_delegueAuService() {
        when(inscriptionService.getInscriptionsByEvenementAndStatut(1L, "confirme"))
                .thenReturn(List.of(new InscriptionEvenementDTO()));

        ResponseEntity<List<InscriptionEvenementDTO>> response = controller.getInscriptionsByEvenement(1L, "confirme");

        assertEquals(1, response.getBody().size());
    }

    @Test
    void getInscriptionById_superAdmin_retourneOk() {
        InscriptionEvenementDTO dto = new InscriptionEvenementDTO();
        dto.setMembreId(5L);
        when(inscriptionService.getInscriptionById(1L)).thenReturn(Optional.of(dto));

        ResponseEntity<InscriptionEvenementDTO> response = controller.getInscriptionById(1L, auth("super@test.com", "SUPER_ADMIN"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getInscriptionById_absent_retourneNotFound() {
        when(inscriptionService.getInscriptionById(1L)).thenReturn(Optional.empty());

        ResponseEntity<InscriptionEvenementDTO> response = controller.getInscriptionById(1L, auth("super@test.com", "SUPER_ADMIN"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void getInscriptionById_adminAutreClub_leveForbidden() {
        InscriptionEvenementDTO dto = new InscriptionEvenementDTO();
        dto.setMembreId(5L);
        when(inscriptionService.getInscriptionById(1L)).thenReturn(Optional.of(dto));
        when(utilisateurService.findByEmail("admin@test.com")).thenReturn(Optional.of(user(1L, 10L)));
        when(membreService.findById(5L)).thenReturn(Optional.of(membre(5L, 20L)));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.getInscriptionById(1L, auth("admin@test.com", "ADMIN")));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void inscrireMembres_succes_retourneCreated() {
        when(inscriptionService.inscrireMembres(eq(1L), anyList(), anyString()))
                .thenReturn(List.of(new InscriptionEvenementDTO()));

        InscriptionRequestDTO req = new InscriptionRequestDTO();
        req.setEvenementId(1L);
        req.setMembreIds(List.of(5L));
        req.setCommentaire("hello");

        ResponseEntity<?> response = controller.inscrireMembres(req, auth("super@test.com", "SUPER_ADMIN"));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    void inscrireMembres_utiliseEnfantsIdsSiMembreIdsVide() {
        when(inscriptionService.inscrireMembres(eq(1L), eq(List.of(7L)), any()))
                .thenReturn(List.of(new InscriptionEvenementDTO()));

        InscriptionRequestDTO req = new InscriptionRequestDTO();
        req.setEvenementId(1L);
        req.setEnfantsIds(List.of(7L));

        ResponseEntity<?> response = controller.inscrireMembres(req, auth("super@test.com", "SUPER_ADMIN"));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    void inscrireMembres_aucunMembre_retourneBadRequest() {
        InscriptionRequestDTO req = new InscriptionRequestDTO();
        req.setEvenementId(1L);

        ResponseEntity<?> response = controller.inscrireMembres(req, auth("super@test.com", "SUPER_ADMIN"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void inscrireMembres_membreInaccessible_retourneErreurDuResponseStatusException() {
        when(utilisateurService.findByEmail("admin@test.com")).thenReturn(Optional.of(user(1L, 10L)));
        when(membreService.findById(5L)).thenReturn(Optional.of(membre(5L, 99L)));

        InscriptionRequestDTO req = new InscriptionRequestDTO();
        req.setEvenementId(1L);
        req.setMembreIds(List.of(5L));

        ResponseEntity<?> response = controller.inscrireMembres(req, auth("admin@test.com", "ADMIN"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void inscrireMembres_erreurService_retourneBadRequest() {
        when(inscriptionService.inscrireMembres(eq(1L), anyList(), any()))
                .thenThrow(new IllegalStateException("deja inscrit"));

        InscriptionRequestDTO req = new InscriptionRequestDTO();
        req.setEvenementId(1L);
        req.setMembreIds(List.of(5L));

        ResponseEntity<?> response = controller.inscrireMembres(req, auth("super@test.com", "SUPER_ADMIN"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void inscrireMembreConnecte_succes_retourneCreated() {
        when(utilisateurService.findByEmail("membre@test.com")).thenReturn(Optional.of(user(3L, null)));
        when(membreService.getMembreEntityByIdUtilisateur(3L)).thenReturn(Optional.of(membre(8L, 10L)));
        when(inscriptionService.inscrireMembres(eq(1L), eq(List.of(8L)), any()))
                .thenReturn(List.of(new InscriptionEvenementDTO()));

        InscriptionRequestDTO req = new InscriptionRequestDTO();
        req.setEvenementId(1L);

        ResponseEntity<?> response = controller.inscrireMembreConnecte(req, auth("membre@test.com", "MEMBRE"));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    void inscrireMembreConnecte_membreIntrouvable_retourneNotFound() {
        when(utilisateurService.findByEmail("membre@test.com")).thenReturn(Optional.of(user(3L, null)));
        when(membreService.getMembreEntityByIdUtilisateur(3L)).thenReturn(Optional.empty());

        InscriptionRequestDTO req = new InscriptionRequestDTO();
        req.setEvenementId(1L);

        ResponseEntity<?> response = controller.inscrireMembreConnecte(req, auth("membre@test.com", "MEMBRE"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void getMesInscriptions_delegueAuService() {
        when(utilisateurService.findByEmail("membre@test.com")).thenReturn(Optional.of(user(3L, null)));
        when(membreService.getMembreEntityByIdUtilisateur(3L)).thenReturn(Optional.of(membre(8L, 10L)));
        when(inscriptionService.getInscriptionsByMembreId(8L)).thenReturn(List.of(new InscriptionEvenementDTO()));

        ResponseEntity<List<InscriptionEvenementDTO>> response = controller.getMesInscriptions(auth("membre@test.com", "MEMBRE"));

        assertEquals(1, response.getBody().size());
    }

    @Test
    void updateInscription_avecMembreIdDansDto_retourneOk() {
        InscriptionEvenementDTO dto = new InscriptionEvenementDTO();
        dto.setMembreId(5L);
        when(inscriptionService.updateInscription(eq(1L), any())).thenReturn(dto);

        ResponseEntity<?> response = controller.updateInscription(1L, dto, auth("super@test.com", "SUPER_ADMIN"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void updateInscription_sansMembreIdDansDto_resoutViaService() {
        InscriptionEvenementDTO existing = new InscriptionEvenementDTO();
        existing.setMembreId(5L);
        when(inscriptionService.getInscriptionById(1L)).thenReturn(Optional.of(existing));
        when(inscriptionService.updateInscription(eq(1L), any())).thenReturn(existing);

        InscriptionEvenementDTO dto = new InscriptionEvenementDTO();
        ResponseEntity<?> response = controller.updateInscription(1L, dto, auth("super@test.com", "SUPER_ADMIN"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void updateInscription_inscriptionIntrouvable_retourneErreur() {
        InscriptionEvenementDTO dto = new InscriptionEvenementDTO();
        when(inscriptionService.getInscriptionById(1L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.updateInscription(1L, dto, auth("super@test.com", "SUPER_ADMIN"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void updateStatutInscription_succes_retourneOk() {
        InscriptionEvenementDTO existing = new InscriptionEvenementDTO();
        existing.setMembreId(5L);
        when(inscriptionService.getInscriptionById(1L)).thenReturn(Optional.of(existing));

        ResponseEntity<?> response = controller.updateStatutInscription(1L, "confirme", auth("super@test.com", "SUPER_ADMIN"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void annulerInscription_succes_retourneNoContent() {
        InscriptionEvenementDTO existing = new InscriptionEvenementDTO();
        existing.setMembreId(5L);
        when(inscriptionService.getInscriptionById(1L)).thenReturn(Optional.of(existing));

        ResponseEntity<?> response = controller.annulerInscription(1L, auth("super@test.com", "SUPER_ADMIN"));

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void getInscriptionsByClub_adminPropreClub_retourneOk() {
        when(utilisateurService.findByEmail("admin@test.com")).thenReturn(Optional.of(user(1L, 10L)));
        when(inscriptionService.getInscriptionsByClubId(10L)).thenReturn(List.of(new InscriptionEvenementDTO()));

        ResponseEntity<List<InscriptionEvenementDTO>> response = controller.getInscriptionsByClub(10L, auth("admin@test.com", "ADMIN"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getInscriptionsByClub_adminAutreClub_leveForbidden() {
        when(utilisateurService.findByEmail("admin@test.com")).thenReturn(Optional.of(user(1L, 10L)));

        assertThrows(ResponseStatusException.class,
                () -> controller.getInscriptionsByClub(99L, auth("admin@test.com", "ADMIN")));
    }
}
