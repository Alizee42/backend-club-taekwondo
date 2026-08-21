package club.taekwondo.service.common;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.Permission;
import com.google.auth.oauth2.ServiceAccountCredentials;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Teste le chemin de succes de GoogleDriveUploadService en mockant la chaine
 * fluide du SDK Google (ServiceAccountCredentials statique, construction de
 * Drive.Builder, et les appels files().create()/permissions().create()).
 */
@ExtendWith(MockitoExtension.class)
class GoogleDriveUploadServiceSuccessTest {

    @Mock
    private ResourceLoader resourceLoader;

    private Resource fakeCredentialsResource() {
        String json = "{\"type\":\"service_account\"}";
        return new ByteArrayResource(json.getBytes(StandardCharsets.UTF_8)) {
            @Override
            public InputStream getInputStream() {
                return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
            }
        };
    }

    @Test
    void uploadFileToDrive_succes_retourneWebViewLink() throws Exception {
        when(resourceLoader.getResource(anyString())).thenReturn(fakeCredentialsResource());

        GoogleDriveUploadService service = new GoogleDriveUploadService(resourceLoader, "classpath:credentials.json", "");
        MultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", "contenu".getBytes());

        ServiceAccountCredentials credentials = mock(ServiceAccountCredentials.class);
        when(credentials.createScoped(any(java.util.Collection.class))).thenReturn(credentials);

        Drive.Files.Create createRequest = mock(Drive.Files.Create.class, Mockito.RETURNS_DEEP_STUBS);
        File uploadedFile = new File();
        uploadedFile.setId("drive-file-id");
        uploadedFile.setWebViewLink("https://drive.google.com/view/xyz");
        when(createRequest.setFields(anyString())).thenReturn(createRequest);
        when(createRequest.execute()).thenReturn(uploadedFile);

        Drive.Permissions.Create permissionCreate = mock(Drive.Permissions.Create.class);
        when(permissionCreate.execute()).thenReturn(new Permission());

        Drive.Files filesResource = mock(Drive.Files.class);
        when(filesResource.create(any(File.class), any())).thenReturn(createRequest);

        Drive.Permissions permissionsResource = mock(Drive.Permissions.class);
        when(permissionsResource.create(anyString(), any(Permission.class))).thenReturn(permissionCreate);

        Drive drive = mock(Drive.class);
        when(drive.files()).thenReturn(filesResource);
        when(drive.permissions()).thenReturn(permissionsResource);

        try (MockedStatic<ServiceAccountCredentials> credsStatic = Mockito.mockStatic(ServiceAccountCredentials.class);
             MockedConstruction<Drive.Builder> builderConstruction = Mockito.mockConstruction(Drive.Builder.class,
                     (mockBuilder, context) -> {
                         when(mockBuilder.setApplicationName(anyString())).thenReturn(mockBuilder);
                         when(mockBuilder.build()).thenReturn(drive);
                     })) {

            credsStatic.when(() -> ServiceAccountCredentials.fromStream(any(InputStream.class)))
                    .thenReturn(credentials);

            String result = service.uploadFileToDrive(file);

            assertEquals("https://drive.google.com/view/xyz", result);
        }
    }

    @Test
    void uploadFileToDrive_sansWebViewLink_replieSurWebContentLink() throws Exception {
        when(resourceLoader.getResource(anyString())).thenReturn(fakeCredentialsResource());

        GoogleDriveUploadService service = new GoogleDriveUploadService(resourceLoader, "classpath:credentials.json", "");
        MultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", "contenu".getBytes());

        ServiceAccountCredentials credentials = mock(ServiceAccountCredentials.class);
        when(credentials.createScoped(any(java.util.Collection.class))).thenReturn(credentials);

        Drive.Files.Create createRequest = mock(Drive.Files.Create.class);
        File uploadedFile = new File();
        uploadedFile.setId("drive-file-id");
        uploadedFile.setWebViewLink(null);
        uploadedFile.setWebContentLink("https://drive.google.com/content/xyz");
        when(createRequest.setFields(anyString())).thenReturn(createRequest);
        when(createRequest.execute()).thenReturn(uploadedFile);

        Drive.Permissions.Create permissionCreate = mock(Drive.Permissions.Create.class);
        when(permissionCreate.execute()).thenReturn(new Permission());

        Drive.Files filesResource = mock(Drive.Files.class);
        when(filesResource.create(any(File.class), any())).thenReturn(createRequest);

        Drive.Permissions permissionsResource = mock(Drive.Permissions.class);
        when(permissionsResource.create(anyString(), any(Permission.class))).thenReturn(permissionCreate);

        Drive drive = mock(Drive.class);
        when(drive.files()).thenReturn(filesResource);
        when(drive.permissions()).thenReturn(permissionsResource);

        try (MockedStatic<ServiceAccountCredentials> credsStatic = Mockito.mockStatic(ServiceAccountCredentials.class);
             MockedConstruction<Drive.Builder> builderConstruction = Mockito.mockConstruction(Drive.Builder.class,
                     (mockBuilder, context) -> {
                         when(mockBuilder.setApplicationName(anyString())).thenReturn(mockBuilder);
                         when(mockBuilder.build()).thenReturn(drive);
                     })) {

            credsStatic.when(() -> ServiceAccountCredentials.fromStream(any(InputStream.class)))
                    .thenReturn(credentials);

            String result = service.uploadFileToDrive(file);

            assertEquals("https://drive.google.com/content/xyz", result);
        }
    }

    @Test
    void uploadFileToDrive_avecParentFolderId_lAssocieAuxMetadonnees() throws Exception {
        when(resourceLoader.getResource(anyString())).thenReturn(fakeCredentialsResource());

        GoogleDriveUploadService service = new GoogleDriveUploadService(resourceLoader, "classpath:credentials.json", "folder-123");
        MultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", "contenu".getBytes());

        ServiceAccountCredentials credentials = mock(ServiceAccountCredentials.class);
        when(credentials.createScoped(any(java.util.Collection.class))).thenReturn(credentials);

        Drive.Files.Create createRequest = mock(Drive.Files.Create.class);
        File uploadedFile = new File();
        uploadedFile.setId("drive-file-id");
        uploadedFile.setWebViewLink("https://drive.google.com/view/parented");
        when(createRequest.setFields(anyString())).thenReturn(createRequest);
        when(createRequest.execute()).thenReturn(uploadedFile);

        Drive.Permissions.Create permissionCreate = mock(Drive.Permissions.Create.class);
        when(permissionCreate.execute()).thenReturn(new Permission());

        Drive.Files filesResource = mock(Drive.Files.class);
        org.mockito.ArgumentCaptor<File> metadataCaptor = org.mockito.ArgumentCaptor.forClass(File.class);
        when(filesResource.create(metadataCaptor.capture(), any())).thenReturn(createRequest);

        Drive.Permissions permissionsResource = mock(Drive.Permissions.class);
        when(permissionsResource.create(anyString(), any(Permission.class))).thenReturn(permissionCreate);

        Drive drive = mock(Drive.class);
        when(drive.files()).thenReturn(filesResource);
        when(drive.permissions()).thenReturn(permissionsResource);

        try (MockedStatic<ServiceAccountCredentials> credsStatic = Mockito.mockStatic(ServiceAccountCredentials.class);
             MockedConstruction<Drive.Builder> builderConstruction = Mockito.mockConstruction(Drive.Builder.class,
                     (mockBuilder, context) -> {
                         when(mockBuilder.setApplicationName(anyString())).thenReturn(mockBuilder);
                         when(mockBuilder.build()).thenReturn(drive);
                     })) {

            credsStatic.when(() -> ServiceAccountCredentials.fromStream(any(InputStream.class)))
                    .thenReturn(credentials);

            service.uploadFileToDrive(file);

            assertEquals(java.util.List.of("folder-123"), metadataCaptor.getValue().getParents());
        }
    }

    @Test
    void uploadFileToDrive_erreurPendantLUpload_relanceIOExceptionAvecMessageExplicite() throws Exception {
        when(resourceLoader.getResource(anyString())).thenReturn(fakeCredentialsResource());

        GoogleDriveUploadService service = new GoogleDriveUploadService(resourceLoader, "classpath:credentials.json", "");
        MultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", "contenu".getBytes());

        try (MockedStatic<ServiceAccountCredentials> credsStatic = Mockito.mockStatic(ServiceAccountCredentials.class)) {
            credsStatic.when(() -> ServiceAccountCredentials.fromStream(any(InputStream.class)))
                    .thenThrow(new RuntimeException("credentials JSON invalides"));

            java.io.IOException ex = assertThrows(java.io.IOException.class,
                    () -> service.uploadFileToDrive(file));
            org.junit.jupiter.api.Assertions.assertTrue(ex.getMessage().contains("credentials JSON invalides"));
        }
    }
}
