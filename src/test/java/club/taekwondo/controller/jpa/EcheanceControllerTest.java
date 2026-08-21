package club.taekwondo.controller.jpa;

import club.taekwondo.dto.EcheanceDTO;
import club.taekwondo.service.jpa.EcheanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EcheanceControllerTest {

    @Mock
    private EcheanceService echeanceService;

    private EcheanceController controller;

    @BeforeEach
    void setUp() {
        controller = new EcheanceController();
        ReflectionTestUtils.setField(controller, "echeanceService", echeanceService);
    }

    @Test
    void getEcheancesByClub_vide_retourneNoContent() {
        when(echeanceService.getEcheancesByClubId(1L)).thenReturn(List.of());

        ResponseEntity<List<EcheanceDTO>> response = controller.getEcheancesByClub(1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void getEcheancesByClub_nonVide_retourneOk() {
        when(echeanceService.getEcheancesByClubId(1L)).thenReturn(List.of(new EcheanceDTO()));

        ResponseEntity<List<EcheanceDTO>> response = controller.getEcheancesByClub(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getAllEcheances_delegueAuService() {
        when(echeanceService.getAllEcheanceDTOs()).thenReturn(List.of(new EcheanceDTO(), new EcheanceDTO()));

        ResponseEntity<List<EcheanceDTO>> response = controller.getAllEcheances();

        assertEquals(2, response.getBody().size());
    }

    @Test
    void payerEcheance_sansBody_utiliseValeursNulles() {
        when(echeanceService.payerEcheance(eq(1L), isNull(), isNull(), isNull())).thenReturn(new EcheanceDTO());

        ResponseEntity<?> response = controller.payerEcheance(1L, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void payerEcheance_avecBodyComplet_parseLaDate() {
        when(echeanceService.payerEcheance(eq(1L), eq("cb"), eq("REF123"), eq(LocalDate.of(2026, 1, 15))))
                .thenReturn(new EcheanceDTO());
        Map<String, String> body = Map.of(
                "modePaiement", "cb",
                "reference", "REF123",
                "datePaiementReel", "2026-01-15"
        );

        ResponseEntity<?> response = controller.payerEcheance(1L, body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void payerEcheance_dateInvalide_retourneBadRequest() {
        Map<String, String> body = Map.of("datePaiementReel", "pas-une-date");

        ResponseEntity<?> response = controller.payerEcheance(1L, body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void payerEcheance_dejaPayee_retourneConflict() {
        when(echeanceService.payerEcheance(eq(1L), isNull(), isNull(), isNull()))
                .thenThrow(new IllegalStateException("déjà payée"));

        ResponseEntity<?> response = controller.payerEcheance(1L, null);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void payerEcheance_introuvable_retourneNotFound() {
        when(echeanceService.payerEcheance(eq(1L), isNull(), isNull(), isNull()))
                .thenThrow(new NoSuchElementException("absente"));

        ResponseEntity<?> response = controller.payerEcheance(1L, null);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void payerEcheance_argumentInvalide_retourneNotFound() {
        when(echeanceService.payerEcheance(eq(1L), isNull(), isNull(), isNull()))
                .thenThrow(new IllegalArgumentException("invalide"));

        ResponseEntity<?> response = controller.payerEcheance(1L, null);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void payerEcheance_erreurInattendue_retourneInternalServerError() {
        when(echeanceService.payerEcheance(eq(1L), isNull(), isNull(), isNull()))
                .thenThrow(new RuntimeException("boom"));

        ResponseEntity<?> response = controller.payerEcheance(1L, null);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void deleteEcheance_succes_retourneOk() {
        ResponseEntity<Void> response = controller.deleteEcheance(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(echeanceService).delete(1L);
    }

    @Test
    void deleteEcheance_runtimeException_retourneNotFound() {
        doThrow(new RuntimeException("absente")).when(echeanceService).delete(1L);

        ResponseEntity<Void> response = controller.deleteEcheance(1L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

}
