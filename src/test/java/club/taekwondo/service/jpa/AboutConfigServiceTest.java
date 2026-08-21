package club.taekwondo.service.jpa;

import club.taekwondo.dto.AboutConfigDto;
import club.taekwondo.entity.jpa.Club;
import club.taekwondo.service.common.FileUploadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class AboutConfigServiceTest extends AbstractServiceIntegrationTest {

    @MockBean
    private FileUploadService fileUploadService;

    @Autowired
    private AboutConfigService aboutConfigService;

    private Club club;

    @BeforeEach
    void setupAboutConfig() {
        club = new Club();
        club.setName("Club About Test");
        club = clubRepository.save(club);
    }

    @Test
    void get_aucuneConfigExistante_retourneUnDtoVideAvecClubIdNull() {
        AboutConfigDto dto = aboutConfigService.get(club.getId());

        assertNull(dto.getClubId());
        assertNull(dto.getHeadingLine1());
        assertNotNull(dto.getChips());
        assertTrue(dto.getChips().isEmpty());
    }

    @Test
    void update_creeLaConfigSiInexistanteEtRattacheLeClub() {
        AboutConfigDto dto = new AboutConfigDto();
        dto.setHeadingLine1("Bienvenue");
        dto.setLeadText("Texte d'intro");

        AboutConfigDto updated = aboutConfigService.update(club.getId(), dto);

        assertEquals(club.getId(), updated.getClubId());
        assertEquals("Bienvenue", updated.getHeadingLine1());
        assertEquals("Texte d'intro", updated.getLeadText());
    }

    @Test
    void update_champNonFourni_neReinitialisePasLaValeurExistante() {
        AboutConfigDto premier = new AboutConfigDto();
        premier.setHeadingLine1("Titre 1");
        premier.setLeadText("Intro 1");
        aboutConfigService.update(club.getId(), premier);

        AboutConfigDto second = new AboutConfigDto();
        second.setLeadText("Intro 2");
        AboutConfigDto updated = aboutConfigService.update(club.getId(), second);

        assertEquals("Titre 1", updated.getHeadingLine1());
        assertEquals("Intro 2", updated.getLeadText());
    }

    @Test
    void update_chipsFournis_remplaceCompletementLaListe() {
        AboutConfigDto premier = new AboutConfigDto();
        premier.setChips(List.of("A", "B"));
        aboutConfigService.update(club.getId(), premier);

        AboutConfigDto second = new AboutConfigDto();
        second.setChips(List.of("C"));
        AboutConfigDto updated = aboutConfigService.update(club.getId(), second);

        assertEquals(List.of("C"), updated.getChips());
    }

    @Test
    void update_valuesFournies_remplaceCompletementLaListe() {
        AboutConfigDto.AboutValueDto v1 = new AboutConfigDto.AboutValueDto();
        v1.setBold("Respect");
        v1.setDescription("Description respect");

        AboutConfigDto premier = new AboutConfigDto();
        premier.setValues(List.of(v1));
        AboutConfigDto updated = aboutConfigService.update(club.getId(), premier);

        assertEquals(1, updated.getValues().size());
        assertEquals("Respect", updated.getValues().get(0).getBold());
    }

    @Test
    void update_reappelSurMemeClub_reutiliseLaConfigExistante() {
        AboutConfigDto dto1 = new AboutConfigDto();
        dto1.setHeadingLine1("V1");
        aboutConfigService.update(club.getId(), dto1);

        AboutConfigDto dto2 = new AboutConfigDto();
        dto2.setHeadingLine1("V2");
        aboutConfigService.update(club.getId(), dto2);

        AboutConfigDto current = aboutConfigService.get(club.getId());
        assertEquals("V2", current.getHeadingLine1());
    }

    @Test
    void uploadImage_persisteLeCheminRetourneParLeFileUploadService() throws Exception {
        MockMultipartFile file = new MockMultipartFile("image", "photo.png", "image/png", new byte[]{1, 2, 3});
        when(fileUploadService.uploadFile(any(), anyString())).thenReturn("about/photo.png");

        AboutConfigDto updated = aboutConfigService.uploadImage(club.getId(), file);

        assertEquals("about/photo.png", updated.getImagePath());
    }
}
