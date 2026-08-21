package club.taekwondo.controller.jpa;

import club.taekwondo.dto.RequiredDocumentDTO;
import club.taekwondo.service.jpa.RequiredDocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequiredDocumentControllerTest {

    @Mock
    private RequiredDocumentService service;

    private RequiredDocumentController controller;

    @BeforeEach
    void setUp() {
        controller = new RequiredDocumentController();
        ReflectionTestUtils.setField(controller, "service", service);
    }

    @Test
    void getByClub_vide_retourneNoContent() {
        when(service.getByClub(1L)).thenReturn(List.of());

        ResponseEntity<?> response = controller.getByClub(1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void getByClub_nonVide_retourneOk() {
        when(service.getByClub(1L)).thenReturn(List.of(new RequiredDocumentDTO()));

        ResponseEntity<?> response = controller.getByClub(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getByClub_erreur_retourneBadRequestAvecMessage() {
        when(service.getByClub(1L)).thenThrow(new IllegalArgumentException("club inconnu"));

        ResponseEntity<?> response = controller.getByClub(1L);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("club inconnu", response.getBody());
    }

    @Test
    void create_succes_retourneCreated() {
        RequiredDocumentDTO dto = new RequiredDocumentDTO();
        when(service.create(dto)).thenReturn(dto);

        ResponseEntity<?> response = controller.create(dto);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    void create_erreur_retourneBadRequest() {
        RequiredDocumentDTO dto = new RequiredDocumentDTO();
        when(service.create(dto)).thenThrow(new IllegalArgumentException("code déjà utilisé"));

        ResponseEntity<?> response = controller.create(dto);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("code déjà utilisé", response.getBody());
    }

    @Test
    void update_succes_retourneOk() {
        RequiredDocumentDTO dto = new RequiredDocumentDTO();
        when(service.update(anyLong(), any(RequiredDocumentDTO.class))).thenReturn(dto);

        ResponseEntity<?> response = controller.update(1L, dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void update_erreur_retourneBadRequest() {
        RequiredDocumentDTO dto = new RequiredDocumentDTO();
        when(service.update(anyLong(), any(RequiredDocumentDTO.class)))
                .thenThrow(new IllegalArgumentException("introuvable"));

        ResponseEntity<?> response = controller.update(1L, dto);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void delete_succes_retourneNoContent() {
        ResponseEntity<?> response = controller.delete(1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(service).delete(1L);
    }

    @Test
    void delete_erreur_retourneBadRequest() {
        doThrow(new IllegalArgumentException("introuvable")).when(service).delete(1L);

        ResponseEntity<?> response = controller.delete(1L);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}
