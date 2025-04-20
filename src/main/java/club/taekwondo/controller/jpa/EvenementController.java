package club.taekwondo.controller.jpa;

import club.taekwondo.entity.jpa.Evenement;
import club.taekwondo.service.jpa.EvenementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/evenements")
public class EvenementController {

    @Autowired
    private EvenementService evenementService;

    // Endpoint pour récupérer tous les événements
    @GetMapping
    public List<Evenement> getAllEvenements() {
        return evenementService.getAllEvenements();
    }

    // Endpoint pour récupérer un événement par son ID
    @GetMapping("/{id}")
    public ResponseEntity<Evenement> getEvenementById(@PathVariable Long id) {
        Optional<Evenement> evenement = evenementService.getEvenementById(id);
        return evenement.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Endpoint pour créer un nouvel événement
    @PostMapping
    public ResponseEntity<Evenement> createEvenement(@RequestBody Evenement evenement) {
        Evenement newEvenement = evenementService.createEvenement(evenement);
        return new ResponseEntity<>(newEvenement, HttpStatus.CREATED);
    }

    // Endpoint pour mettre à jour un événement existant
    @PutMapping("/{id}")
    public ResponseEntity<Evenement> updateEvenement(@PathVariable Long id, @RequestBody Evenement evenement) {
        Evenement updatedEvenement = evenementService.updateEvenement(id, evenement);
        return updatedEvenement != null ? ResponseEntity.ok(updatedEvenement) : ResponseEntity.notFound().build();
    }

    // Endpoint pour supprimer un événement
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvenement(@PathVariable Long id) {
        evenementService.deleteEvenement(id);
        return ResponseEntity.noContent().build();
    }
}
