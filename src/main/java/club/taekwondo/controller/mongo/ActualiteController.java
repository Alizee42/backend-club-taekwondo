package club.taekwondo.controller.mongo;

import club.taekwondo.entity.mongo.Actualite;
import club.taekwondo.service.mongo.ActualiteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/actualites")
public class ActualiteController {

    private final ActualiteService actualiteService;

    public ActualiteController(ActualiteService actualiteService) {
        this.actualiteService = actualiteService;
    }

    @GetMapping
    public List<Actualite> getAll() {
        return actualiteService.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Actualite> getById(@PathVariable String id) {
        return actualiteService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Actualite create(@RequestBody Actualite actualite) {
        return actualiteService.create(actualite);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Actualite> update(@PathVariable String id, @RequestBody Actualite actualite) {
        Actualite updated = actualiteService.update(id, actualite);
        if (updated != null) {
            return ResponseEntity.ok(updated);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        actualiteService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
