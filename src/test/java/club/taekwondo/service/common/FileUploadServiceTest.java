package club.taekwondo.service.common;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class FileUploadServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void uploadFile_succes_creeLeFichierEtRetourneCheminRelatif() throws Exception {
        FileUploadService service = new FileUploadService(tempDir.toString());
        MultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", "contenu".getBytes());

        String result = service.uploadFile(file, "avis");

        assertTrue(result.startsWith("avis/"));
        assertTrue(result.endsWith("photo.jpg"));
        assertTrue(Files.exists(tempDir.resolve(result)));
    }

    @Test
    void uploadFile_fichierNull_leveIllegalArgument() {
        FileUploadService service = new FileUploadService(tempDir.toString());

        assertThrows(IllegalArgumentException.class, () -> service.uploadFile(null, "avis"));
    }

    @Test
    void uploadFile_fichierVide_leveIllegalArgument() {
        FileUploadService service = new FileUploadService(tempDir.toString());
        MultipartFile empty = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[0]);

        assertThrows(IllegalArgumentException.class, () -> service.uploadFile(empty, "avis"));
    }

    @Test
    void uploadFile_sousDossierTraversal_leveIllegalArgument() {
        FileUploadService service = new FileUploadService(tempDir.toString());
        MultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", "contenu".getBytes());

        assertThrows(IllegalArgumentException.class, () -> service.uploadFile(file, "../../etc"));
    }

    @Test
    void uploadFile_nomOriginalNull_leveIllegalArgument() {
        FileUploadService service = new FileUploadService(tempDir.toString());
        MultipartFile file = new MockMultipartFile("file", null, "image/jpeg", "contenu".getBytes());

        assertThrows(IllegalArgumentException.class, () -> service.uploadFile(file, "avis"));
    }

    @Test
    void uploadFile_creeLeSousDossierSiAbsent() throws Exception {
        FileUploadService service = new FileUploadService(tempDir.toString());
        MultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", "contenu".getBytes());

        service.uploadFile(file, "documents/nested");

        assertTrue(Files.isDirectory(tempDir.resolve("documents").resolve("nested")));
    }

    @Test
    void uploadFile_nomDuplique_neEcrasePasLePremierFichier() throws Exception {
        FileUploadService service = new FileUploadService(tempDir.toString());
        MultipartFile file1 = new MockMultipartFile("file", "meme-nom.jpg", "image/jpeg", "contenu1".getBytes());
        MultipartFile file2 = new MockMultipartFile("file", "meme-nom.jpg", "image/jpeg", "contenu2".getBytes());

        String result1 = service.uploadFile(file1, "avis");
        String result2 = service.uploadFile(file2, "avis");

        assertTrue(Files.exists(tempDir.resolve(result1)));
        assertTrue(Files.exists(tempDir.resolve(result2)));
        assertEquals("contenu1", Files.readString(tempDir.resolve(result1)));
        assertEquals("contenu2", Files.readString(tempDir.resolve(result2)));
    }

    @Test
    void uploadFile_sansExtension_leveIllegalArgument() {
        FileUploadService service = new FileUploadService(tempDir.toString());
        MultipartFile file = new MockMultipartFile("file", "fichiersansextension", "text/plain", "contenu".getBytes());

        assertThrows(IllegalArgumentException.class, () -> service.uploadFile(file, "avis"));
    }

    @Test
    void uploadFile_extensionNonAutorisee_leveIllegalArgument() {
        FileUploadService service = new FileUploadService(tempDir.toString());
        MultipartFile file = new MockMultipartFile("file", "page.html", "text/html", "<script>alert(1)</script>".getBytes());

        assertThrows(IllegalArgumentException.class, () -> service.uploadFile(file, "avis"));
    }

    @Test
    void uploadFile_extensionAutoriseeMaisContentTypeIncoherent_leveIllegalArgument() {
        FileUploadService service = new FileUploadService(tempDir.toString());
        MultipartFile file = new MockMultipartFile("file", "faux.jpg", "text/html", "<script>alert(1)</script>".getBytes());

        assertThrows(IllegalArgumentException.class, () -> service.uploadFile(file, "avis"));
    }
}
