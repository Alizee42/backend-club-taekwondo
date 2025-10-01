package club.taekwondo.controller;

import club.taekwondo.dto.ContactMessageDTO;
import club.taekwondo.service.jpa.EmailService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/public/contact")
@CrossOrigin(origins = "*")
public class PublicContactController {

    private final EmailService emailService;

    public PublicContactController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping
    public ResponseEntity<?> envoyer(@Valid @RequestBody ContactMessageDTO dto) {
        emailService.envoyerMessageContact(dto.getName(), dto.getEmail(), dto.getObjet(), dto.getMessage());
        Map<String,Object> body = new HashMap<>();
        body.put("status", "OK");
        body.put("timestamp", Instant.now());
        return ResponseEntity.ok(body);
    }
}
