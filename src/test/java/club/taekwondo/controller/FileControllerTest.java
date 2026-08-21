package club.taekwondo.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class FileControllerTest {

    @TempDir
    Path tempDir;

    @Test
    void getFile_fichierExistant_retourneOk() throws Exception {
        FileController controller = new FileController(tempDir.toString());
        Path avisDir = tempDir.resolve("avis");
        Files.createDirectories(avisDir);
        Files.writeString(avisDir.resolve("image.jpg"), "contenu");

        ResponseEntity<Resource> response = controller.getFile("avis", "image.jpg");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getFile_fichierInexistant_retourneNotFound() {
        FileController controller = new FileController(tempDir.toString());

        ResponseEntity<Resource> response = controller.getFile("avis", "inexistant.jpg");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void getFile_traversalViaDossier_retourneNotFound() {
        FileController controller = new FileController(tempDir.toString());

        ResponseEntity<Resource> response = controller.getFile("..", "etc%2Fpasswd");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
