package club.taekwondo.service.common;

import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.Permission;
import com.google.api.services.drive.model.File;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.ServiceAccountCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

@Service
public class GoogleDriveUploadService {
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String APPLICATION_NAME = "Club Taekwondo Documents";

    private final ResourceLoader resourceLoader;
    private final String credentialsPath;
    private final String parentFolderId; // optional parent folder id on Drive

    public GoogleDriveUploadService(ResourceLoader resourceLoader,
                                    @Value("${google.credentials.path:classpath:credentials.json}") String credentialsPath,
                                    @Value("${google.drive.parentFolderId:}") String parentFolderId) {
        this.resourceLoader = resourceLoader;
        this.credentialsPath = credentialsPath;
        this.parentFolderId = parentFolderId != null && !parentFolderId.isBlank() ? parentFolderId : null;
    }

    /**
     * Upload file to Google Drive using service account credentials and return a webView link.
     */
    public String uploadFileToDrive(MultipartFile multipartFile) throws IOException {
        // 1) Save temp file
        Path tempFile = Files.createTempFile("upload", multipartFile.getOriginalFilename());
        try (InputStream in = multipartFile.getInputStream()) {
            Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }

        // 2) Load credentials
        try (InputStream credStream = resourceLoader.getResource(credentialsPath).getInputStream()) {
            try {
                com.google.auth.Credentials credentials = ServiceAccountCredentials.fromStream(credStream)
                        .createScoped(List.of("https://www.googleapis.com/auth/drive.file", "https://www.googleapis.com/auth/drive"));

                // 3) Build Drive service
                Drive drive = new Drive.Builder(new NetHttpTransport(), JSON_FACTORY, new HttpCredentialsAdapter(credentials))
                        .setApplicationName(APPLICATION_NAME)
                        .build();

                // continue with upload using local variables
                // 4) Prepare metadata
                File fileMetadata = new File();
                fileMetadata.setName(multipartFile.getOriginalFilename());
                if (parentFolderId != null) {
                    fileMetadata.setParents(List.of(parentFolderId));
                }

                // 5) Upload
                com.google.api.client.http.FileContent mediaContent = new com.google.api.client.http.FileContent(multipartFile.getContentType(), tempFile.toFile());
                File uploaded = drive.files().create(fileMetadata, mediaContent)
                        .setFields("id,webViewLink,webContentLink,owners")
                        .execute();

                // 6) Make public (optional: keeps previous behaviour)
                Permission p = new Permission();
                p.setType("anyone");
                p.setRole("reader");
                drive.permissions().create(uploaded.getId(), p).execute();

                // 7) cleanup
                Files.deleteIfExists(tempFile);

                // 8) return link (fallback to webContentLink if webViewLink null)
                return uploaded.getWebViewLink() != null ? uploaded.getWebViewLink() : uploaded.getWebContentLink();
            } catch (Exception ex) {
                String message = "Impossible de lire les credentials Google Drive depuis '" + credentialsPath + "'. Vérifiez que le fichier est une clé de compte de service (JSON) contenant \"type\": \"service_account\". Détails: " + ex.getMessage();
                System.out.println("[GOOGLE DRIVE] " + message);
                // cleanup temp file before rethrow
                try { Files.deleteIfExists(tempFile); } catch (Exception ignore) {}
                throw new IOException(message, ex);
            }
        }
    }
}
