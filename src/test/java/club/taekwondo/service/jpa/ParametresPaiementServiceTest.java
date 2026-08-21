package club.taekwondo.service.jpa;

import club.taekwondo.dto.ParametresPaiementDTO;
import club.taekwondo.entity.jpa.Club;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

class ParametresPaiementServiceTest extends AbstractServiceIntegrationTest {

    @Autowired
    private ParametresPaiementService parametresPaiementService;

    private Club club;

    @BeforeEach
    void setupParametresPaiement() {
        club = new Club();
        club.setName("Club Parametres Paiement Test");
        club = clubRepository.save(club);
    }

    @Test
    void getParametresPaiementByClub_aucuneConfigExistante_retourneLesValeursParDefaut() {
        ParametresPaiementDTO dto = parametresPaiementService.getParametresPaiementByClub(club.getId());

        assertEquals(100, dto.getMontantCotisation());
        assertTrue(dto.isVirement());
        assertTrue(dto.isEspeces());
        assertTrue(dto.isStripe());
        assertEquals("stripe", dto.getModePaiementParDefaut());
        assertEquals(4, dto.getEcheancesAutorisees());
        assertEquals("MENSUEL", dto.getIntervalleEcheance());
    }

    @Test
    void updateParametresPaiement_creeLaConfigAvecIdCalqueSurLeClubId() {
        ParametresPaiementDTO dto = new ParametresPaiementDTO();
        dto.setMontantCotisation(150.0);
        dto.setVirement(false);
        dto.setEspeces(true);
        dto.setStripe(false);
        dto.setModePaiementParDefaut("especes");
        dto.setEcheancesAutorisees(2);
        dto.setIntervalleEcheance("TRIMESTRIEL");

        parametresPaiementService.updateParametresPaiement(club.getId(), dto);

        ParametresPaiementDTO reloaded = parametresPaiementService.getParametresPaiementByClub(club.getId());
        assertEquals(150.0, reloaded.getMontantCotisation());
        assertFalse(reloaded.isVirement());
        assertTrue(reloaded.isEspeces());
        assertFalse(reloaded.isStripe());
        assertEquals("especes", reloaded.getModePaiementParDefaut());
        assertEquals(2, reloaded.getEcheancesAutorisees());
        assertEquals("TRIMESTRIEL", reloaded.getIntervalleEcheance());
    }

    @Test
    void updateParametresPaiement_appelsSuccessifs_metAJourLaMemeLigne() {
        ParametresPaiementDTO dto1 = new ParametresPaiementDTO();
        dto1.setMontantCotisation(100.0);
        dto1.setModePaiementParDefaut("stripe");
        dto1.setIntervalleEcheance("MENSUEL");
        parametresPaiementService.updateParametresPaiement(club.getId(), dto1);

        ParametresPaiementDTO dto2 = new ParametresPaiementDTO();
        dto2.setMontantCotisation(200.0);
        dto2.setModePaiementParDefaut("virement");
        dto2.setIntervalleEcheance("ANNUEL");
        parametresPaiementService.updateParametresPaiement(club.getId(), dto2);

        ParametresPaiementDTO reloaded = parametresPaiementService.getParametresPaiementByClub(club.getId());
        assertEquals(200.0, reloaded.getMontantCotisation());
        assertEquals("virement", reloaded.getModePaiementParDefaut());
        assertEquals("ANNUEL", reloaded.getIntervalleEcheance());
    }

    @Test
    void updateParametresPaiement_deuxClubsDifferents_configsIndependantes() {
        Club autreClub = new Club();
        autreClub.setName("Autre Club Parametres");
        autreClub = clubRepository.save(autreClub);

        ParametresPaiementDTO dtoClub1 = new ParametresPaiementDTO();
        dtoClub1.setMontantCotisation(100.0);
        dtoClub1.setModePaiementParDefaut("stripe");
        dtoClub1.setIntervalleEcheance("MENSUEL");
        parametresPaiementService.updateParametresPaiement(club.getId(), dtoClub1);

        ParametresPaiementDTO dtoClub2 = new ParametresPaiementDTO();
        dtoClub2.setMontantCotisation(300.0);
        dtoClub2.setModePaiementParDefaut("especes");
        dtoClub2.setIntervalleEcheance("ANNUEL");
        parametresPaiementService.updateParametresPaiement(autreClub.getId(), dtoClub2);

        assertEquals(100.0, parametresPaiementService.getParametresPaiementByClub(club.getId()).getMontantCotisation());
        assertEquals(300.0, parametresPaiementService.getParametresPaiementByClub(autreClub.getId()).getMontantCotisation());
    }
}
