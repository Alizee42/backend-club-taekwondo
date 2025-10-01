package club.taekwondo.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * ✅ Gestion dédiée des accès interdits (sinon captés par RuntimeException et renvoyés en 400)
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "Access Denied"));
    }

    /**
     * ✅ Handler générique pour RuntimeException (hors AccessDeniedException)
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        if (ex instanceof AccessDeniedException) {
            // Laisser le handler spécifique ci-dessus gérer
            return handleAccessDenied((AccessDeniedException) ex);
        }
        String message = ex.getMessage();
        HttpStatus status = HttpStatus.BAD_REQUEST;
        if (message != null && message.contains("Membre non trouvé")) {
            status = HttpStatus.NOT_FOUND;
        }
        if (message != null && message.contains("Utilisateur non trouvé")) {
            status = HttpStatus.BAD_REQUEST;
        }
        if (message != null && message.contains("mot de passe incorrect")) {
            status = HttpStatus.UNAUTHORIZED;
        }
        return ResponseEntity.status(status).body(Map.of("message", message));
    }
}