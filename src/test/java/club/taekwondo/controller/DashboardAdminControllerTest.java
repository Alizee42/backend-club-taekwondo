package club.taekwondo.controller;

import club.taekwondo.entity.jpa.Club;
import club.taekwondo.entity.jpa.Evenement;
import club.taekwondo.entity.jpa.Membre;
import club.taekwondo.entity.jpa.Paiement;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.repository.jpa.EvenementRepository;
import club.taekwondo.repository.jpa.MembreRepository;
import club.taekwondo.repository.jpa.PaiementRepository;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardAdminControllerTest {

    @Mock
    private MembreRepository membreRepository;

    @Mock
    private PaiementRepository paiementRepository;

    @Mock
    private EvenementRepository evenementRepository;

    @Mock
    private UtilisateurService utilisateurService;

    private DashboardAdminController controller;

    @BeforeEach
    void setUp() {
        controller = new DashboardAdminController(
                membreRepository,
                paiementRepository,
                evenementRepository,
                utilisateurService
        );
    }

    @Test
    void getAdminStats_returnsClubScopedStats() {
        Utilisateur admin = user(1L, "admin@test.com", 7L);
        Authentication auth = auth("admin@test.com");

        when(utilisateurService.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(membreRepository.findByClub_Id(7L)).thenReturn(List.of(member(1L), member(2L)));
        when(paiementRepository.findByClubIdAny(7L)).thenReturn(List.of(
                paiement("payé", 120.0),
                paiement("en attente", 55.0),
                paiement("annulé", 30.0),
                paiement("payé", null)
        ));
        when(evenementRepository.findByClub_Id(7L)).thenReturn(List.of(
                event(LocalDateTime.now().plusDays(2)),
                event(LocalDateTime.now().minusDays(1))
        ));

        ResponseEntity<Map<String, Object>> response = controller.getAdminStats(auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2L, response.getBody().get("nbMembres"));
        assertEquals(120.0d, response.getBody().get("totalPaiements"));
        assertEquals(1L, response.getBody().get("paiementsAttente"));
        assertEquals(1L, response.getBody().get("evenementsAVenir"));
    }

    @Test
    void getAdminStats_adminWithoutClubGetsZeroStats() {
        Utilisateur admin = user(2L, "admin-sans-club@test.com", null);
        Authentication auth = auth("admin-sans-club@test.com");

        when(utilisateurService.findByEmail("admin-sans-club@test.com")).thenReturn(Optional.of(admin));

        ResponseEntity<Map<String, Object>> response = controller.getAdminStats(auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0L, response.getBody().get("nbMembres"));
        assertEquals(0.0d, response.getBody().get("totalPaiements"));
        assertEquals(0L, response.getBody().get("paiementsAttente"));
        assertEquals(0L, response.getBody().get("evenementsAVenir"));
        verifyNoInteractions(membreRepository, paiementRepository, evenementRepository);
    }

    private Authentication auth(String email) {
        TestingAuthenticationToken token = new TestingAuthenticationToken(email, null, "ROLE_ADMIN");
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

    private Membre member(Long id) {
        Membre membre = new Membre();
        membre.setId(id);
        membre.setNom("Nom");
        membre.setPrenom("Prenom");
        return membre;
    }

    private Paiement paiement(String statut, Double montant) {
        Paiement paiement = new Paiement();
        paiement.setStatut(statut);
        paiement.setMontantTotal(montant);
        return paiement;
    }

    private Evenement event(LocalDateTime dateDebut) {
        Evenement evenement = new Evenement();
        evenement.setDateDebut(dateDebut);
        return evenement;
    }
}
