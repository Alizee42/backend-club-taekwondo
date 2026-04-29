package club.taekwondo.controller.jpa;

import club.taekwondo.dto.ClubDto;
import club.taekwondo.service.jpa.ClubService;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clubs")
public class ClubController {
    @Autowired
    private ClubService clubService;

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
}
