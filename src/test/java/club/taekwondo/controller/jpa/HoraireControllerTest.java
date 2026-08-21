package club.taekwondo.controller.jpa;

import club.taekwondo.entity.jpa.Horaire;
import club.taekwondo.service.jpa.HoraireService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HoraireControllerTest {

    @Mock
    private HoraireService horaireService;

    private HoraireController controller;

    @BeforeEach
    void setUp() {
        controller = new HoraireController();
        ReflectionTestUtils.setField(controller, "horaireService", horaireService);
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
    void updateHoraire_fixeLIdDepuisLePathVariable() {
        Horaire horaire = new Horaire();
        when(horaireService.updateHoraire(any(Horaire.class))).thenAnswer(inv -> inv.getArgument(0));

        Horaire result = controller.updateHoraire(42L, horaire);

        assertEquals(42L, result.getId());
    }

    @Test
    void addHoraire_associeLeClubDepuisLePathVariable() {
        when(horaireService.addHoraire(any(Horaire.class))).thenAnswer(inv -> inv.getArgument(0));

        Horaire result = controller.addHoraire(7L, new Horaire());

        assertEquals(7L, result.getClub().getId());
    }

    @Test
    void deleteHoraire_appelleLeService() {
        controller.deleteHoraire(5L);

        verify(horaireService).deleteHoraire(5L);
    }
}
