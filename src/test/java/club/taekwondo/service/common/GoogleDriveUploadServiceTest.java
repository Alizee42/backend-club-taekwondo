package club.taekwondo.service.common;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleDriveUploadServiceTest {

    @Mock
    private ResourceLoader resourceLoader;

    @Test
    void uploadFileToDrive_credentialsIntrouvables_leveIOExceptionAvecMessageExplicite() {
        // Chemin credentials pointant vers une ressource classpath inexistante :
        // resourceLoader.getResource(...).getInputStream() leve alors une IOException,
        // catchee par le service qui la re-emballe avec un message explicite.
        when(resourceLoader.getResource(anyString()))
                .thenReturn(new org.springframework.core.io.ClassPathResource("credentials-inexistant.json"));

        GoogleDriveUploadService service = new GoogleDriveUploadService(
                resourceLoader, "classpath:credentials-inexistant.json", "");

        MultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", "contenu".getBytes());

        IOException ex = assertThrows(IOException.class, () -> service.uploadFileToDrive(file));
        assertTrue(ex.getMessage().contains("credentials-inexistant.json"));
    }
}
