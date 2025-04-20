package club.taekwondo.controller.jpa;

import club.taekwondo.entity.jpa.Paiement;
import club.taekwondo.service.jpa.PaiementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/paiements")
public class PaiementController {

    private final PaiementService paiementService;

    public PaiementController(PaiementService paiementService) {
        this.paiementService = paiementService;
    }

    @GetMapping
    public List<Paiement> getAll() {
        return paiementService.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Paiement> getById(@PathVariable Long id) {
        return paiementService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/membre/{membreId}")
    public List<Paiement> getByMembreId(@PathVariable Long membreId) {
        return paiementService.getByMembreId(membreId);
    }

    @PostMapping
    public Paiement create(@RequestBody Paiement paiement) {
        return paiementService.save(paiement);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Paiement> update(@PathVariable Long id, @RequestBody Paiement paiement) {
        if (!paiementService.getById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        paiement.setId(id);
        return ResponseEntity.ok(paiementService.save(paiement));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!paiementService.getById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        paiementService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
