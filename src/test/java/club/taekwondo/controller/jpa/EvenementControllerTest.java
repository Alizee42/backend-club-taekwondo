package club.taekwondo.controller.jpa;

import club.taekwondo.dto.EvenementDTO;
import club.taekwondo.entity.jpa.Club;
import club.taekwondo.entity.jpa.Evenement;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.service.jpa.EvenementService;
import club.taekwondo.service.jpa.InscriptionEvenementService;
import club.taekwondo.service.jpa.UtilisateurService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvenementControllerTest {

    @Mock
    private EvenementService evenementService;

    @Mock
    private InscriptionEvenementService inscriptionService;

    @Mock
    private UtilisateurService utilisateurService;

    private EvenementController controller;

    @BeforeEach
    void setUp() {
        controller = new EvenementController(evenementService, inscriptionService, utilisateurService);
    }

    @Test
    void ajouterEvenement_adminUsesOwnClubScope() {
        Authentication auth = auth("admin@test.com", "ROLE_ADMIN");
        Utilisateur admin = user(1L, "admin@test.com", 4L);
        EvenementDTO created = new EvenementDTO();
        created.setId(15L);
        MockMultipartFile image = new MockMultipartFile("image", "event.png", "image/png", new byte[]{1});

        when(utilisateurService.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(evenementService.ajouterEvenement(
                eq("Stage"),
                eq("2026-04-20T10:00"),
                eq("2026-04-20T12:00"),
                eq("Dojo"),
                eq(30),
                eq("Description"),
                eq(image),
                eq(4L)
        )).thenReturn(created);

        ResponseEntity<EvenementDTO> response = controller.ajouterEvenement(
                "Stage",
                "2026-04-20T10:00",
                "2026-04-20T12:00",
                "Dojo",
                30,
                "Description",
                image,
                null,
                auth
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(15L, response.getBody().getId());
    }

    @Test
    void ajouterEvenement_adminCannotTargetAnotherClub() {
        Authentication auth = auth("admin@test.com", "ROLE_ADMIN");
        Utilisateur admin = user(1L, "admin@test.com", 4L);
        MockMultipartFile image = new MockMultipartFile("image", "event.png", "image/png", new byte[]{1});

        when(utilisateurService.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> controller.ajouterEvenement(
                        "Stage",
                        "2026-04-20T10:00",
                        "2026-04-20T12:00",
                        "Dojo",
                        30,
                        "Description",
                        image,
                        5L,
                        auth
                ));

        assertEquals(HttpStatus.FORBIDDEN.value(), exception.getStatusCode().value());
        verify(evenementService, never()).ajouterEvenement(
                eq("Stage"),
                eq("2026-04-20T10:00"),
                eq("2026-04-20T12:00"),
                eq("Dojo"),
                eq(30),
                eq("Description"),
                eq(image),
                eq(5L)
        );
    }

    @Test
    void getInscriptionsEnfants_parentCannotReadAnotherParent() {
        Authentication auth = auth("parent@test.com", "ROLE_PARENT");
        Utilisateur parent = user(10L, "parent@test.com", 3L);

        when(utilisateurService.findByEmail("parent@test.com")).thenReturn(Optional.of(parent));

        ResponseEntity<List<club.taekwondo.dto.InscriptionEvenementDTO>> response =
                controller.getInscriptionsEnfants(11L, auth);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(inscriptionService, never()).getInscriptionsByParent(11L);
    }

    @Test
    void deleteEvenement_adminCannotDeleteOtherClubEvent() {
        Authentication auth = auth("admin@test.com", "ROLE_ADMIN");
        Utilisateur admin = user(1L, "admin@test.com", 4L);
        Evenement evenement = event(90L, 8L);

        when(utilisateurService.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(evenementService.getEvenementEntityById(90L)).thenReturn(Optional.of(evenement));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> controller.deleteEvenement(90L, auth));

        assertEquals(HttpStatus.FORBIDDEN.value(), exception.getStatusCode().value());
        verify(evenementService, never()).deleteEvenement(90L);
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

    private Evenement event(Long id, Long clubId) {
        Club club = new Club();
        club.setId(clubId);
        club.setName("Club " + clubId);

        Evenement evenement = new Evenement();
        evenement.setId(id);
        evenement.setClub(club);
        return evenement;
    }
}
