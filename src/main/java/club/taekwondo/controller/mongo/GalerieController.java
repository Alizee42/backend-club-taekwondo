package club.taekwondo.controller.mongo;

import club.taekwondo.entity.mongo.Galerie;
import club.taekwondo.service.mongo.GalerieService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/galeries")
public class GalerieController {

    private final GalerieService galerieService;

    public GalerieController(GalerieService galerieService) {
        this.galerieService = galerieService;
    }

    @GetMapping
    public List<Galerie> getAll() {
        return galerieService.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Galerie> getById(@PathVariable String id) {
        return galerieService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Galerie create(@RequestBody Galerie galerie) {
        return galerieService.create(galerie);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Galerie> update(@PathVariable String id, @RequestBody Galerie galerie) {
        Galerie updated = galerieService.update(id, galerie);
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