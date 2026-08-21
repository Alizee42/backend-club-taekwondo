package club.taekwondo.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleAccessDenied_retourneForbidden() {
        ResponseEntity<Map<String, String>> response = handler.handleAccessDenied(new AccessDeniedException("nope"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Access Denied", response.getBody().get("message"));
    }

    @Test
    void handleRuntimeException_membreNonTrouve_retourneNotFound() {
        ResponseEntity<Map<String, String>> response = handler.handleRuntimeException(new RuntimeException("Membre non trouvé avec id 5"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void handleRuntimeException_utilisateurNonTrouve_retourneBadRequest() {
        ResponseEntity<Map<String, String>> response = handler.handleRuntimeException(new RuntimeException("Utilisateur non trouvé"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void handleRuntimeException_motDePasseIncorrect_retourneUnauthorized() {
        ResponseEntity<Map<String, String>> response = handler.handleRuntimeException(new RuntimeException("mot de passe incorrect"));

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void handleRuntimeException_messageInconnu_retourneBadRequestParDefaut() {
        ResponseEntity<Map<String, String>> response = handler.handleRuntimeException(new RuntimeException("erreur quelconque"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void handleRuntimeException_messageNull_leveNullPointerException() {
        // Bug connu : Map.of("message", null) rejette les valeurs nulles, donc une
        // RuntimeException sans message fait planter le handler lui-meme (NPE non capturee).
        org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class,
                () -> handler.handleRuntimeException(new RuntimeException()));
    }

    @Test
    void handleRuntimeException_avecAccessDeniedException_delegueAuHandlerSpecifique() {
        ResponseEntity<Map<String, String>> response = handler.handleRuntimeException(new AccessDeniedException("nope"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Access Denied", response.getBody().get("message"));
    }
}
