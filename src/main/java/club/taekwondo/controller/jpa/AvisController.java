package club.taekwondo.controller.jpa;

import club.taekwondo.entity.jpa.Avis;
import club.taekwondo.service.jpa.AvisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/avis")
public class AvisController {

    @Autowired
    private AvisService avisService;

    // Endpoint pour récupérer tous les avis
    @GetMapping
    public List<Avis> getAllAvis() {
        return avisService.getAllAvis();
    }

    // Endpoint pour récupérer un avis par son ID
    @GetMapping("/{id}")
    public ResponseEntity<Avis> getAvisById(@PathVariable Long id) {
        Optional<Avis> avis = avisService.getAvisById(id);
        return avis.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Endpoint pour créer un nouvel avis
    @PostMapping
    public ResponseEntity<Avis> createAvis(@RequestBody Avis avis) {
        Avis newAvis = avisService.createAvis(avis);
        return new ResponseEntity<>(newAvis, HttpStatus.CREATED);
    }

    // Endpoint pour mettre à jour un avis existant
    @PutMapping("/{id}")
    public ResponseEntity<Avis> updateAvis(@PathVariable Long id, @RequestBody Avis avis) {
        Avis updatedAvis = avisService.updateAvis(id, avis);
        return updatedAvis != null ? ResponseEntity.ok(updatedAvis) : ResponseEntity.notFound().build();
    }

    // Endpoint pour supprimer un avis
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAvis(@PathVariable Long id) {
        avisService.deleteAvis(id);
        return ResponseEntity.noContent().build();
    }
}
