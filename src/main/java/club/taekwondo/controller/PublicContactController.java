package club.taekwondo.controller;

import club.taekwondo.dto.ContactMessageDTO;
import club.taekwondo.entity.jpa.Club;
import club.taekwondo.repository.jpa.ClubRepository;
import club.taekwondo.service.jpa.EmailService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/public/contact")
public class PublicContactController {

    private final EmailService emailService;
    private final ClubRepository clubRepository;

    public PublicContactController(EmailService emailService, ClubRepository clubRepository) {
        this.emailService = emailService;
        this.clubRepository = clubRepository;
    }

    @PostMapping
    public ResponseEntity<?> envoyer(@Valid @RequestBody ContactMessageDTO dto) {
        Club club = dto.getClubId() != null ? clubRepository.findById(dto.getClubId()).orElse(null) : null;
        emailService.envoyerMessageContact(club, dto.getName(), dto.getEmail(), dto.getObjet(), dto.getMessage());
        Map<String,Object> body = new HashMap<>();
        body.put("status", "OK");
        body.put("timestamp", Instant.now());
        return ResponseEntity.ok(body);
    }
}
