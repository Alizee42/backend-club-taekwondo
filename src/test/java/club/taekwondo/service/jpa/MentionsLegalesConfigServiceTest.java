package club.taekwondo.service.jpa;

import club.taekwondo.dto.MentionsLegalesConfigDto;
import club.taekwondo.entity.jpa.Club;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

class MentionsLegalesConfigServiceTest extends AbstractServiceIntegrationTest {

    @Autowired
    private MentionsLegalesConfigService mentionsLegalesConfigService;

    private Club club;

    @BeforeEach
    void setupMentionsLegales() {
        club = new Club();
        club.setName("Club Mentions Legales Test");
        club = clubRepository.save(club);
    }

    @Test
    void get_aucuneConfigExistante_retourneUnDtoVideAvecClubIdNull() {
        MentionsLegalesConfigDto dto = mentionsLegalesConfigService.get(club.getId());

        assertNull(dto.getClubId());
        assertNull(dto.getNomAssociation());
    }

    @Test
    void update_creeLaConfigSiInexistanteEtRattacheLeClub() {
        MentionsLegalesConfigDto dto = new MentionsLegalesConfigDto();
        dto.setNomAssociation("Club de Taekwondo");
        dto.setNumeroSiren("123456789");

        MentionsLegalesConfigDto updated = mentionsLegalesConfigService.update(club.getId(), dto);

        assertEquals(club.getId(), updated.getClubId());
        assertEquals("Club de Taekwondo", updated.getNomAssociation());
        assertEquals("123456789", updated.getNumeroSiren());
    }

    @Test
    void update_champNonFourni_neReinitialisePasLaValeurExistante() {
        MentionsLegalesConfigDto premier = new MentionsLegalesConfigDto();
        premier.setNomAssociation("Nom initial");
        premier.setEmail("contact@club.fr");
        mentionsLegalesConfigService.update(club.getId(), premier);

        MentionsLegalesConfigDto second = new MentionsLegalesConfigDto();
        second.setEmail("nouveau@club.fr");
        MentionsLegalesConfigDto updated = mentionsLegalesConfigService.update(club.getId(), second);

        assertEquals("Nom initial", updated.getNomAssociation());
        assertEquals("nouveau@club.fr", updated.getEmail());
    }

    @Test
    void update_reappelSurMemeClub_reutiliseLaConfigExistante() {
        MentionsLegalesConfigDto dto1 = new MentionsLegalesConfigDto();
        dto1.setNomAssociation("V1");
        mentionsLegalesConfigService.update(club.getId(), dto1);

        MentionsLegalesConfigDto dto2 = new MentionsLegalesConfigDto();
        dto2.setNomAssociation("V2");
        mentionsLegalesConfigService.update(club.getId(), dto2);

        MentionsLegalesConfigDto current = mentionsLegalesConfigService.get(club.getId());
        assertEquals("V2", current.getNomAssociation());
    }

    @Test
    void update_clubInexistant_leveExceptionCarClubIdEstObligatoireEnBase() {
        // club_id est NOT NULL UNIQUE en base : un clubId qui ne correspond a aucun
        // club existant laisse l'entite sans club rattache et la persistance echoue.
        MentionsLegalesConfigDto dto = new MentionsLegalesConfigDto();
        dto.setNomAssociation("Sans club");

        assertThrows(RuntimeException.class, () -> mentionsLegalesConfigService.update(999999L, dto));
    }
}
