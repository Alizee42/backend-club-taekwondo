package club.taekwondo.service.jpa;

import club.taekwondo.entity.jpa.Club;
import club.taekwondo.entity.jpa.Horaire;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

class HoraireServiceTest extends AbstractServiceIntegrationTest {

    @Autowired
    private HoraireService horaireService;

    private Club club;

    @BeforeEach
    void setupHoraires() {
        club = new Club();
        club.setName("Club Horaire Test");
        club = clubRepository.save(club);
    }

    private Horaire horaire(String jour, String debut, String fin) {
        Horaire h = new Horaire();
        h.setJour(jour);
        h.setHeureDebut(debut);
        h.setHeureFin(fin);
        h.setClub(club);
        return h;
    }

    @Test
    void addHoraire_persisteEtRetourneAvecId() {
        Horaire saved = horaireService.addHoraire(horaire("Lundi", "18:00", "20:00"));

        assertNotNull(saved.getId());
        assertEquals("Lundi", saved.getJour());
    }

    @Test
    void updateHoraire_modifieLesChamps() {
        Horaire saved = horaireService.addHoraire(horaire("Lundi", "18:00", "20:00"));
        saved.setHeureFin("21:00");

        Horaire updated = horaireService.updateHoraire(saved);

        assertEquals("21:00", updated.getHeureFin());
    }

    @Test
    void deleteHoraire_supprimeLHoraire() {
        Horaire saved = horaireService.addHoraire(horaire("Lundi", "18:00", "20:00"));

        horaireService.deleteHoraire(saved.getId());

        assertTrue(horaireService.getAllHoraires().isEmpty());
    }

    @Test
    void getHorairesByClub_neRetourneQueLesHorairesDuClub() {
        horaireService.addHoraire(horaire("Lundi", "18:00", "20:00"));

        Club autreClub = new Club();
        autreClub.setName("Autre Club Horaire");
        autreClub = clubRepository.save(autreClub);
        Horaire autreHoraire = new Horaire();
        autreHoraire.setJour("Mardi");
        autreHoraire.setHeureDebut("10:00");
        autreHoraire.setHeureFin("12:00");
        autreHoraire.setClub(autreClub);
        horaireService.addHoraire(autreHoraire);

        assertEquals(1, horaireService.getHorairesByClub(club.getId()).size());
        assertEquals(1, horaireService.getHorairesByClub(autreClub.getId()).size());
    }

    @Test
    void getAllHoraires_retourneTousLesHoraires() {
        horaireService.addHoraire(horaire("Lundi", "18:00", "20:00"));
        horaireService.addHoraire(horaire("Mercredi", "17:00", "19:00"));

        assertEquals(2, horaireService.getAllHoraires().size());
    }
}
