package club.taekwondo.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class UploadsControllerTest {

    @TempDir
    Path tempDir;

    private UploadsController controller;

    @BeforeEach
    void setUp() {
        controller = new UploadsController();
    }

    @Test
    void getDocument_uploadDirNonConfigure_retourneNotFound() {
        ResponseEntity<Resource> response = controller.getDocument("fichier.pdf");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void getDocument_fichierInexistant_retourneNotFound() {
        ReflectionTestUtils.setField(controller, "uploadDir", tempDir.toString());

        ResponseEntity<Resource> response = controller.getDocument("inexistant.pdf");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void getDocument_fichierExistant_retourneOk() throws Exception {
        ReflectionTestUtils.setField(controller, "uploadDir", tempDir.toString());
        Path documentsDir = tempDir.resolve("documents");
        Files.createDirectories(documentsDir);
        Files.writeString(documentsDir.resolve("licence.pdf"), "contenu");

        ResponseEntity<Resource> response = controller.getDocument("licence.pdf");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getDocument_traversal_retourneNotFound() {
        ReflectionTestUtils.setField(controller, "uploadDir", tempDir.toString());

        ResponseEntity<Resource> response = controller.getDocument("..%2F..%2Fetc%2Fpasswd");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
