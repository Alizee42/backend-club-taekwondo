package club.taekwondo.controller.jpa;

import club.taekwondo.dto.InscriptionEvenementDTO;
import club.taekwondo.dto.InscriptionRequestDTO;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

    @Test
    void inscrireMembres_parentCannotRegisterForeignChild() {
        Authentication auth = auth("parent@test.com", "ROLE_PARENT");
        Utilisateur parent = user(10L, "parent@test.com", 1L);

        InscriptionRequestDTO request = new InscriptionRequestDTO();
        request.setEvenementId(5L);
        request.setEnfantsIds(List.of(99L));

        when(utilisateurService.findByEmail("parent@test.com")).thenReturn(Optional.of(parent));
        when(membreService.findById(99L)).thenReturn(Optional.of(member(99L, 1L)));
        when(membreService.getEnfantsDuParent(10L)).thenReturn(List.of(member(11L, 1L)));

        ResponseEntity<?> response = controller.inscrireMembres(request, auth);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(inscriptionService, never()).inscrireMembres(5L, List.of(99L), null);
    }

    @Test
    void getInscriptionsByClub_adminCannotReadAnotherClub() {
        Authentication auth = auth("admin@test.com", "ROLE_ADMIN");
        Utilisateur admin = user(1L, "admin@test.com", 3L);

        when(utilisateurService.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> controller.getInscriptionsByClub(7L, auth));

        assertEquals(HttpStatus.FORBIDDEN.value(), exception.getStatusCode().value());
        verify(inscriptionService, never()).getInscriptionsByClubId(7L);
    }

    @Test
    void annulerInscription_memberCannotCancelAnotherMemberInscription() {
        Authentication auth = auth("membre@test.com", "ROLE_MEMBRE");
        Utilisateur memberUser = user(40L, "membre@test.com", 2L);
        InscriptionEvenementDTO dto = new InscriptionEvenementDTO();
        dto.setId(8L);
        dto.setMembreId(88L);

        when(utilisateurService.findByEmail("membre@test.com")).thenReturn(Optional.of(memberUser));
        when(inscriptionService.getInscriptionById(8L)).thenReturn(Optional.of(dto));
        when(membreService.findById(88L)).thenReturn(Optional.of(member(88L, 2L)));
        when(membreService.getMembreEntityByIdUtilisateur(40L)).thenReturn(Optional.of(member(77L, 2L)));

        ResponseEntity<?> response = controller.annulerInscription(8L, auth);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(inscriptionService, never()).annulerInscription(8L);
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

    private Membre member(Long id, Long clubId) {
        Membre membre = new Membre();
        membre.setId(id);
        if (clubId != null) {
            Club club = new Club();
            club.setId(clubId);
            club.setName("Club " + clubId);
            membre.setClub(club);
        }
        return membre;
    }
}
