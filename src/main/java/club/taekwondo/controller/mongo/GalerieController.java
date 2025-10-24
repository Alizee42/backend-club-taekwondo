
package club.taekwondo.controller.mongo;

import club.taekwondo.dto.GalerieDTO;
import club.taekwondo.service.mongo.GalerieService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/galeries")
public class GalerieController {
    // 🔒 Récupérer toutes les galeries d'un club
    @GetMapping("/club/{clubId}")
    public List<GalerieDTO> getByClubId(@PathVariable Long clubId) {
        return galerieService.getByClubId(clubId);
    }

    private final GalerieService galerieService;

    public GalerieController(GalerieService galerieService) {
        this.galerieService = galerieService;
    }

    @GetMapping
    public List<GalerieDTO> getAll() {
        return galerieService.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<GalerieDTO> getById(@PathVariable String id) {
        return galerieService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public GalerieDTO createMultipart(
            @RequestParam("titre") String titre,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("clubId") Long clubId,
            @RequestParam("image") MultipartFile image,
            @RequestHeader("X-User-Role") String userRole,
            @RequestHeader("X-User-ClubId") Long userClubId) {
        return galerieService.createMultipart(titre, description, clubId, image, userRole, userClubId);
    }


    @PutMapping("/{id}")
    public ResponseEntity<GalerieDTO> update(@PathVariable String id,
                                             @RequestBody GalerieDTO galerieDTO,
                                             @RequestHeader("X-User-Role") String userRole,
                                             @RequestHeader("X-User-ClubId") Long userClubId) {
        GalerieDTO updated = galerieService.update(id, galerieDTO, userRole, userClubId);
        if (updated != null) {
            return ResponseEntity.ok(updated);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        galerieService.delete(id);
        return ResponseEntity.noContent().build();
    }
}