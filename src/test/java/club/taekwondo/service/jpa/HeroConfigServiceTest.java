package club.taekwondo.service.jpa;

import club.taekwondo.dto.HeroConfigDto;
import club.taekwondo.repository.jpa.HeroConfigRepository;
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

class HeroConfigServiceTest extends AbstractServiceIntegrationTest {

    @MockBean
    private FileUploadService fileUploadService;

    @Autowired
    private HeroConfigService heroConfigService;

    @Autowired
    private HeroConfigRepository heroConfigRepository;

    @BeforeEach
    void setupHeroConfig() {
        heroConfigRepository.deleteAll();
    }

    @Test
    void get_aucuneConfigExistante_retourneUnDtoVide() {
        HeroConfigDto dto = heroConfigService.get();

        assertNull(dto.getVideoPath());
        assertNull(dto.getEyebrowText());
        assertNotNull(dto.getSlogans());
        assertNotNull(dto.getStats());
    }

    @Test
    void update_creeLaConfigAvecIdFixe() {
        HeroConfigDto dto = new HeroConfigDto();
        dto.setEyebrowText("Bienvenue");
        dto.setIdentityStrong("Champions");

        HeroConfigDto updated = heroConfigService.update(dto);

        assertEquals("Bienvenue", updated.getEyebrowText());
        assertEquals("Champions", updated.getIdentityStrong());
    }

    @Test
    void update_champNonFourni_neReinitialisePasLaValeurExistante() {
        HeroConfigDto premier = new HeroConfigDto();
        premier.setEyebrowText("Texte 1");
        premier.setIdentityStrong("Identite 1");
        heroConfigService.update(premier);

        HeroConfigDto second = new HeroConfigDto();
        second.setIdentityStrong("Identite 2");
        HeroConfigDto updated = heroConfigService.update(second);

        assertEquals("Texte 1", updated.getEyebrowText());
        assertEquals("Identite 2", updated.getIdentityStrong());
    }

    @Test
    void update_slogansFournis_remplaceCompletementLaListe() {
        HeroConfigDto premier = new HeroConfigDto();
        premier.setSlogans(List.of("Slogan A", "Slogan B"));
        heroConfigService.update(premier);

        HeroConfigDto second = new HeroConfigDto();
        second.setSlogans(List.of("Slogan C"));
        HeroConfigDto updated = heroConfigService.update(second);

        assertEquals(List.of("Slogan C"), updated.getSlogans());
    }

    @Test
    void update_statsFournies_remplaceCompletementLaListe() {
        HeroConfigDto.HeroStatDto stat1 = new HeroConfigDto.HeroStatDto();
        stat1.setValue("100");
        stat1.setLabel("Membres");
        stat1.setIcon("users");

        HeroConfigDto dto = new HeroConfigDto();
        dto.setStats(List.of(stat1));

        HeroConfigDto updated = heroConfigService.update(dto);

        assertEquals(1, updated.getStats().size());
        assertEquals("100", updated.getStats().get(0).getValue());
        assertEquals("Membres", updated.getStats().get(0).getLabel());
    }

    @Test
    void update_reappelSuccessif_reutiliseLaMemeConfig() {
        HeroConfigDto dto1 = new HeroConfigDto();
        dto1.setEyebrowText("V1");
        heroConfigService.update(dto1);

        HeroConfigDto dto2 = new HeroConfigDto();
        dto2.setEyebrowText("V2");
        heroConfigService.update(dto2);

        HeroConfigDto current = heroConfigService.get();
        assertEquals("V2", current.getEyebrowText());
        assertEquals(1, heroConfigRepository.count());
    }

    @Test
    void uploadVideo_persisteLeCheminRetourneParLeFileUploadService() throws Exception {
        MockMultipartFile file = new MockMultipartFile("video", "hero.mp4", "video/mp4", new byte[]{1, 2, 3});
        when(fileUploadService.uploadFile(any(), anyString())).thenReturn("hero/hero.mp4");

        HeroConfigDto updated = heroConfigService.uploadVideo(file);

        assertEquals("hero/hero.mp4", updated.getVideoPath());
    }
}
