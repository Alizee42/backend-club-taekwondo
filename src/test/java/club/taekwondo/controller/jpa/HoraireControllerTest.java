package club.taekwondo.controller.jpa;

import club.taekwondo.entity.jpa.Club;
import club.taekwondo.entity.jpa.Horaire;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.repository.jpa.UtilisateurRepository;
import club.taekwondo.service.jpa.HoraireService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HoraireControllerTest {

    @Mock
    private HoraireService horaireService;

    @Mock
    private UtilisateurRepository utilisateurRepository;

    private HoraireController controller;

    @BeforeEach
    void setUp() {
        controller = new HoraireController();
        ReflectionTestUtils.setField(controller, "horaireService", horaireService);
        ReflectionTestUtils.setField(controller, "utilisateurRepository", utilisateurRepository);
    }

    private Authentication auth(String email, String role) {
        return new TestingAuthenticationToken(email, null, role);
    }

    private Utilisateur utilisateurDuClub(Long clubId) {
        Club club = new Club();
        club.setId(clubId);
        Utilisateur u = new Utilisateur();
        u.setEmail("admin@test.com");
        u.setClub(club);
        return u;
    }

    private Horaire horaireDuClub(Long clubId) {
        Club club = new Club();
        club.setId(clubId);
        Horaire h = new Horaire();
        h.setClub(club);
        return h;
    }

    @Test
    void getAllHoraires_delegueAuService() {
        when(horaireService.getAllHoraires()).thenReturn(List.of(new Horaire(), new Horaire()));

        List<Horaire> result = controller.getAllHoraires();

        assertEquals(2, result.size());
    }

    @Test
    void getHorairesByClub_delegueAuService() {
        when(horaireService.getHorairesByClub(1L)).thenReturn(List.of(new Horaire()));

        List<Horaire> result = controller.getHorairesByClub(1L);

        assertEquals(1, result.size());
    }

    @Test
    void updateHoraire_adminProprietaireDuClub_fixeLIdDepuisLePathVariable() {
        when(horaireService.getHoraireById(42L)).thenReturn(Optional.of(horaireDuClub(1L)));
        when(utilisateurRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(utilisateurDuClub(1L)));
        when(horaireService.updateHoraire(any(Horaire.class))).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<?> response = controller.updateHoraire(42L, new Horaire(), auth("admin@test.com", "ROLE_ADMIN"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(42L, ((Horaire) response.getBody()).getId());
    }

    @Test
    void updateHoraire_adminDunAutreClub_retourne403EtNeModifieRien() {
        when(horaireService.getHoraireById(42L)).thenReturn(Optional.of(horaireDuClub(1L)));
        when(utilisateurRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(utilisateurDuClub(2L)));

        ResponseEntity<?> response = controller.updateHoraire(42L, new Horaire(), auth("admin@test.com", "ROLE_ADMIN"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(horaireService, never()).updateHoraire(any(Horaire.class));
    }

    @Test
    void updateHoraire_horaireInexistant_retourne404() {
        when(horaireService.getHoraireById(999L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.updateHoraire(999L, new Horaire(), auth("admin@test.com", "ROLE_ADMIN"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void addHoraire_adminProprietaireDuClub_associeLeClubDepuisLePathVariable() {
        when(utilisateurRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(utilisateurDuClub(7L)));
        when(horaireService.addHoraire(any(Horaire.class))).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<?> response = controller.addHoraire(7L, new Horaire(), auth("admin@test.com", "ROLE_ADMIN"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(7L, ((Horaire) response.getBody()).getClub().getId());
    }

    @Test
    void addHoraire_adminDunAutreClub_retourne403() {
        when(utilisateurRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(utilisateurDuClub(2L)));

        ResponseEntity<?> response = controller.addHoraire(7L, new Horaire(), auth("admin@test.com", "ROLE_ADMIN"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(horaireService, never()).addHoraire(any(Horaire.class));
    }

    @Test
    void addHoraire_superAdmin_neVerifiePasLeClub() {
        when(horaireService.addHoraire(any(Horaire.class))).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<?> response = controller.addHoraire(7L, new Horaire(), auth("superadmin@test.com", "ROLE_SUPER_ADMIN"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void deleteHoraire_adminProprietaireDuClub_appelleLeService() {
        when(horaireService.getHoraireById(5L)).thenReturn(Optional.of(horaireDuClub(1L)));
        when(utilisateurRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(utilisateurDuClub(1L)));

        ResponseEntity<?> response = controller.deleteHoraire(5L, auth("admin@test.com", "ROLE_ADMIN"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(horaireService).deleteHoraire(5L);
    }

    @Test
    void deleteHoraire_adminDunAutreClub_retourne403EtNeSupprimeRien() {
        when(horaireService.getHoraireById(5L)).thenReturn(Optional.of(horaireDuClub(1L)));
        when(utilisateurRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(utilisateurDuClub(2L)));

        ResponseEntity<?> response = controller.deleteHoraire(5L, auth("admin@test.com", "ROLE_ADMIN"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(horaireService, never()).deleteHoraire(any());
    }
}
