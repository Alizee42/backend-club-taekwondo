package club.taekwondo.controller.jpa;

import club.taekwondo.dto.ReinitialisationMotDePasseDTO;
import club.taekwondo.service.jpa.ReinitialisationMotDePasseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/reinitialisation")
public class ReinitialisationMotDePasseController {

    @Autowired
    private ReinitialisationMotDePasseService service;

    @PostMapping("/demander")
    public ResponseEntity<Map<String, String>> demanderReinitialisation(@RequestParam String email) {
        try {
            service.demanderReinitialisation(email);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Demande de réinitialisation envoyée.");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> response = new HashMap<>();
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/verifier")
    public ResponseEntity<?> verifierToken(@RequestParam String token) {
        Optional<ReinitialisationMotDePasseDTO> dto = service.getByToken(token);
        if (dto.isPresent()) {
            return ResponseEntity.ok(dto.get());
        } else {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Lien invalide ou expiré.");
            return ResponseEntity.status(404).body(response);
        }
    }

    @PostMapping("/valider") // Remplacé PUT par POST
    public ResponseEntity<Map<String, String>> validerToken(@RequestParam String token) {
        boolean valide = service.validerToken(token);
        Map<String, String> response = new HashMap<>();
        if (valide) {
            response.put("message", "Token validé.");
            return ResponseEntity.ok(response);
        } else {
            response.put("message", "Token invalide ou expiré.");
            return ResponseEntity.badRequest().body(response);
        }
    }
}

