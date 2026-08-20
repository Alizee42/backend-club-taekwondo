package club.taekwondo.controller.jpa;

import club.taekwondo.dto.ClubDto;
import club.taekwondo.service.jpa.ClubService;
import club.taekwondo.service.common.FileUploadService;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/clubs")
public class ClubController {

    private static final Logger log = LoggerFactory.getLogger(ClubController.class);

    @Autowired
    private ClubService clubService;

    @Autowired
    private FileUploadService fileUploadService;

    @GetMapping
    public List<ClubDto> getAllClubs() {
        return clubService.getAllClubs();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClubDto> getClubById(@PathVariable Long id) {
        ClubDto club = clubService.getClubById(id);
        if (club == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(club);
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ClubDto createClub(@Valid @RequestBody ClubDto dto) {
        return clubService.createClub(dto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ClubDto> updateClub(@PathVariable Long id, @Valid @RequestBody ClubDto dto) {
        ClubDto updated = clubService.updateClub(id, dto);
        if (updated == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deleteClub(@PathVariable Long id) {
        clubService.deleteClub(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/upload-logo")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, String>> uploadLogo(@RequestParam("logo") MultipartFile logo) {
        try {
            String path = fileUploadService.uploadFile(logo, "clubs");
            Map<String, String> body = new HashMap<>();
            body.put("path", path);
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            log.warn("[Clubs] Upload logo échoué: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}
