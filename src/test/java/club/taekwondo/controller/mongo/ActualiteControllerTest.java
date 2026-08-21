package club.taekwondo.controller.mongo;

import club.taekwondo.dto.ActualiteDTO;
import club.taekwondo.service.common.FileUploadService;
import club.taekwondo.service.jpa.ActualiteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActualiteControllerTest {

    @Mock
    private ActualiteService actualiteService;

    @Mock
    private FileUploadService fileUploadService;

    private ActualiteController controller;

    @BeforeEach
    void setUp() {
        controller = new ActualiteController(actualiteService, fileUploadService);
    }

    @Test
    void getByClub_delegueAuService() {
        when(actualiteService.getByClubId("10")).thenReturn(List.of(new ActualiteDTO()));

        ResponseEntity<List<ActualiteDTO>> response = controller.getByClub("10");

        assertEquals(1, response.getBody().size());
    }

    @Test
    void getByClub_resultatNull_retourneListeVide() {
        when(actualiteService.getByClubId("10")).thenReturn(null);

        ResponseEntity<List<ActualiteDTO>> response = controller.getByClub("10");

        assertEquals(List.of(), response.getBody());
    }

    @Test
    void getAll_succes_retourneLaListe() {
        when(actualiteService.getAll()).thenReturn(List.of(new ActualiteDTO(), new ActualiteDTO()));

        ResponseEntity<List<ActualiteDTO>> response = controller.getAll();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(2, response.getBody().size());
    }

    @Test
    void getAll_erreurService_retourne500AvecListeVide() {
        when(actualiteService.getAll()).thenThrow(new RuntimeException("erreur DB"));

        ResponseEntity<List<ActualiteDTO>> response = controller.getAll();

        assertEquals(500, response.getStatusCode().value());
        assertEquals(List.of(), response.getBody());
    }

    @Test
    void getFeatured_delegueAuService() {
        when(actualiteService.getFeatured()).thenReturn(List.of(new ActualiteDTO()));

        ResponseEntity<List<ActualiteDTO>> response = controller.getFeatured();

        assertEquals(1, response.getBody().size());
    }

    @Test
    void getById_trouve_retourneOk() {
        when(actualiteService.getById("1")).thenReturn(Optional.of(new ActualiteDTO()));

        ResponseEntity<ActualiteDTO> response = controller.getById("1");

        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void getById_absent_retourneNotFound() {
        when(actualiteService.getById("1")).thenReturn(Optional.empty());

        ResponseEntity<ActualiteDTO> response = controller.getById("1");

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void create_fixeLaDatePublicationEtRetourneCreated() {
        ActualiteDTO dto = new ActualiteDTO();
        when(actualiteService.create(any(ActualiteDTO.class))).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<ActualiteDTO> response = controller.create(dto);

        assertEquals(201, response.getStatusCode().value());
        assertEquals(dto.getDatePublication() != null, true);
    }

    @Test
    void createWithImage_avecImage_uploadeEtRetourneCreated() throws IOException {
        when(fileUploadService.uploadFile(any(MultipartFile.class), eq("actualites"))).thenReturn("actualites/img.jpg");
        when(actualiteService.create(any(ActualiteDTO.class))).thenAnswer(inv -> inv.getArgument(0));
        MockMultipartFile image = new MockMultipartFile("image", "img.jpg", "image/jpeg", new byte[]{1});

        ResponseEntity<?> response = controller.createWithImage(
                "Titre", "Contenu", "Extrait", "info", true, "10", "complement", image);

        assertEquals(201, response.getStatusCode().value());
    }

    @Test
    void createWithImage_sansImage_creeSansUpload() throws IOException {
        when(actualiteService.create(any(ActualiteDTO.class))).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<?> response = controller.createWithImage(
                "Titre", "Contenu", null, "info", false, "10", null, null);

        assertEquals(201, response.getStatusCode().value());
    }

    @Test
    void createWithImage_erreurUpload_retourneInternalServerError() throws IOException {
        when(fileUploadService.uploadFile(any(MultipartFile.class), eq("actualites")))
                .thenThrow(new IOException("disque plein"));
        MockMultipartFile image = new MockMultipartFile("image", "img.jpg", "image/jpeg", new byte[]{1});

        ResponseEntity<?> response = controller.createWithImage(
                "Titre", "Contenu", null, "info", false, "10", null, image);

        assertEquals(500, response.getStatusCode().value());
    }

    @Test
    void update_trouve_retourneOk() {
        ActualiteDTO dto = new ActualiteDTO();
        when(actualiteService.update(eq("1"), any(ActualiteDTO.class))).thenReturn(dto);

        ResponseEntity<ActualiteDTO> response = controller.update("1", new ActualiteDTO());

        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void update_absent_retourneNotFound() {
        when(actualiteService.update(eq("1"), any(ActualiteDTO.class))).thenReturn(null);

        ResponseEntity<ActualiteDTO> response = controller.update("1", new ActualiteDTO());

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void updateWithImage_avecImage_uploadeEtRetourneOk() throws IOException {
        when(fileUploadService.uploadFile(any(MultipartFile.class), eq("actualites"))).thenReturn("actualites/img.jpg");
        when(actualiteService.update(eq("1"), any(ActualiteDTO.class))).thenReturn(new ActualiteDTO());
        MockMultipartFile image = new MockMultipartFile("image", "img.jpg", "image/jpeg", new byte[]{1});

        ResponseEntity<?> response = controller.updateWithImage(
                "1", "Titre", "Contenu", null, "info", true, "10", null, image);

        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void updateWithImage_absent_retourneNotFound() throws IOException {
        when(actualiteService.update(eq("1"), any(ActualiteDTO.class))).thenReturn(null);

        ResponseEntity<?> response = controller.updateWithImage(
                "1", "Titre", "Contenu", null, "info", false, "10", null, null);

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void updateWithImage_erreurUpload_retourneInternalServerError() throws IOException {
        when(fileUploadService.uploadFile(any(MultipartFile.class), eq("actualites")))
                .thenThrow(new IOException("disque plein"));
        MockMultipartFile image = new MockMultipartFile("image", "img.jpg", "image/jpeg", new byte[]{1});

        ResponseEntity<?> response = controller.updateWithImage(
                "1", "Titre", "Contenu", null, "info", false, "10", null, image);

        assertEquals(500, response.getStatusCode().value());
    }

    @Test
    void setFeatured_succes_retourneOk() {
        ResponseEntity<Void> response = controller.setFeatured("1");

        assertEquals(200, response.getStatusCode().value());
        verify(actualiteService).setFeatured("1");
    }

    @Test
    void setFeatured_erreur_retourneNotFound() {
        doThrow(new RuntimeException("introuvable")).when(actualiteService).setFeatured("1");

        ResponseEntity<Void> response = controller.setFeatured("1");

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void delete_appelleLeServiceEtRetourneNoContent() {
        ResponseEntity<Void> response = controller.delete("1");

        assertEquals(204, response.getStatusCode().value());
        verify(actualiteService).delete("1");
    }
}
