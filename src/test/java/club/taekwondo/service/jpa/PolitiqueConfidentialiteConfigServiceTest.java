package club.taekwondo.service.jpa;

import club.taekwondo.dto.PolitiqueConfidentialiteConfigDto;
import club.taekwondo.entity.jpa.Club;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

class PolitiqueConfidentialiteConfigServiceTest extends AbstractServiceIntegrationTest {

    @Autowired
    private PolitiqueConfidentialiteConfigService politiqueConfidentialiteConfigService;

    private Club club;

    @BeforeEach
    void setupPolitiqueConfidentialite() {
        club = new Club();
        club.setName("Club Politique Confidentialite Test");
        club = clubRepository.save(club);
    }

    @Test
    void get_aucuneConfigExistante_retourneUnDtoVideAvecClubIdNull() {
        PolitiqueConfidentialiteConfigDto dto = politiqueConfidentialiteConfigService.get(club.getId());

        assertNull(dto.getClubId());
        assertNull(dto.getNomAssociation());
    }

    @Test
    void update_creeLaConfigSiInexistanteEtRattacheLeClub() {
        PolitiqueConfidentialiteConfigDto dto = new PolitiqueConfidentialiteConfigDto();
        dto.setNomAssociation("Club de Taekwondo");
        dto.setEmailRgpd("rgpd@club.fr");

        PolitiqueConfidentialiteConfigDto updated = politiqueConfidentialiteConfigService.update(club.getId(), dto);

        assertEquals(club.getId(), updated.getClubId());
        assertEquals("Club de Taekwondo", updated.getNomAssociation());
        assertEquals("rgpd@club.fr", updated.getEmailRgpd());
    }

    @Test
    void update_champNonFourni_neReinitialisePasLaValeurExistante() {
        PolitiqueConfidentialiteConfigDto premier = new PolitiqueConfidentialiteConfigDto();
        premier.setNomAssociation("Nom initial");
        premier.setEmailContact("contact@club.fr");
        politiqueConfidentialiteConfigService.update(club.getId(), premier);

        PolitiqueConfidentialiteConfigDto second = new PolitiqueConfidentialiteConfigDto();
        second.setEmailContact("nouveau@club.fr");
        PolitiqueConfidentialiteConfigDto updated = politiqueConfidentialiteConfigService.update(club.getId(), second);

        assertEquals("Nom initial", updated.getNomAssociation());
        assertEquals("nouveau@club.fr", updated.getEmailContact());
    }

    @Test
    void update_reappelSurMemeClub_reutiliseLaConfigExistante() {
        PolitiqueConfidentialiteConfigDto dto1 = new PolitiqueConfidentialiteConfigDto();
        dto1.setNomAssociation("V1");
        politiqueConfidentialiteConfigService.update(club.getId(), dto1);

        PolitiqueConfidentialiteConfigDto dto2 = new PolitiqueConfidentialiteConfigDto();
        dto2.setNomAssociation("V2");
        politiqueConfidentialiteConfigService.update(club.getId(), dto2);

        PolitiqueConfidentialiteConfigDto current = politiqueConfidentialiteConfigService.get(club.getId());
        assertEquals("V2", current.getNomAssociation());
    }

    @Test
    void update_clubInexistant_leveExceptionCarClubIdEstObligatoireEnBase() {
        // club_id est NOT NULL UNIQUE en base : un clubId qui ne correspond a aucun
        // club existant laisse l'entite sans club rattache et la persistance echoue.
        PolitiqueConfidentialiteConfigDto dto = new PolitiqueConfidentialiteConfigDto();
        dto.setNomAssociation("Sans club");

        assertThrows(RuntimeException.class, () -> politiqueConfidentialiteConfigService.update(999999L, dto));
    }
}
