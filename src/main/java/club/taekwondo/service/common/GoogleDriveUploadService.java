package club.taekwondo.service.common;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Service
public class GoogleDriveUploadService {
    private static final String APPLICATION_NAME = "Club Taekwondo Documents";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    /**
     * Uploads a file to Google Drive and returns the public URL.
     */
    public String uploadFileToDrive(MultipartFile multipartFile) throws IOException, GeneralSecurityException {
        // 1. Save the file temporarily
        Path tempFile = Files.createTempFile("upload", multipartFile.getOriginalFilename());
        try (InputStream in = multipartFile.getInputStream()) {
            Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }

        // 2. Authenticate with Google Drive
        InputStream credentialsStream = getClass().getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (credentialsStream == null) {
            throw new IOException("credentials.json not found in resources");
        }
        GoogleCredential credential = GoogleCredential.fromStream(credentialsStream)
                .createScoped(Collections.singleton("https://www.googleapis.com/auth/drive.file"));

        Drive driveService = new Drive.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JSON_FACTORY,
                credential)
                .setApplicationName(APPLICATION_NAME)
                .build();

        // 3. Create file metadata
        File fileMetadata = new File();
        fileMetadata.setName(multipartFile.getOriginalFilename());

        // 4. Upload file
        FileContent mediaContent = new FileContent(multipartFile.getContentType(), tempFile.toFile());
        File uploadedFile = driveService.files().create(fileMetadata, mediaContent)
                .setFields("id,webViewLink,webContentLink")
                .execute();

        // 5. Make file public (anyone with the link can view)
        driveService.permissions().create(uploadedFile.getId(),
                new com.google.api.services.drive.model.Permission()
                        .setType("anyone")
                        .setRole("reader")
        ).execute();

        // 6. Delete temp file
        Files.deleteIfExists(tempFile);

        // 7. Return the public link
        return uploadedFile.getWebViewLink();
    }
}
