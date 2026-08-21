package club.taekwondo.controller.jpa;

import club.taekwondo.dto.HeroConfigDto;
import club.taekwondo.service.jpa.HeroConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HeroConfigControllerTest {

    @Mock
    private HeroConfigService heroConfigService;

    private HeroConfigController controller;

    @BeforeEach
    void setUp() {
        controller = new HeroConfigController();
        ReflectionTestUtils.setField(controller, "heroConfigService", heroConfigService);
    }

    @Test
    void get_retourneLaConfig() {
        when(heroConfigService.get()).thenReturn(new HeroConfigDto());

        ResponseEntity<HeroConfigDto> response = controller.get();

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void update_retourneLaConfigMiseAJour() {
        HeroConfigDto dto = new HeroConfigDto();
        when(heroConfigService.update(dto)).thenReturn(dto);

        ResponseEntity<HeroConfigDto> response = controller.update(dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void uploadVideo_succes_retourneOk() throws IOException {
        when(heroConfigService.uploadVideo(any(MultipartFile.class))).thenReturn(new HeroConfigDto());
        MockMultipartFile file = new MockMultipartFile("video", "hero.mp4", "video/mp4", new byte[]{1});

        ResponseEntity<?> response = controller.uploadVideo(file);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void uploadVideo_erreurService_retourneInternalServerError() throws IOException {
        when(heroConfigService.uploadVideo(any(MultipartFile.class))).thenThrow(new IOException("Disque plein"));
        MockMultipartFile file = new MockMultipartFile("video", "hero.mp4", "video/mp4", new byte[]{1});

        ResponseEntity<?> response = controller.uploadVideo(file);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }
}
