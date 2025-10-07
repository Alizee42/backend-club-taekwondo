package club.taekwondo.controller.jpa;

import club.taekwondo.dto.ClubDto;
import club.taekwondo.service.jpa.ClubService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    public ClubDto createClub(@RequestBody ClubDto dto) {
        return clubService.createClub(dto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClubDto> updateClub(@PathVariable Long id, @RequestBody ClubDto dto) {
        ClubDto updated = clubService.updateClub(id, dto);
        if (updated == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClub(@PathVariable Long id) {
        clubService.deleteClub(id);
        return ResponseEntity.noContent().build();
    }
}
