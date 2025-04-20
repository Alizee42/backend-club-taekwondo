package club.taekwondo.controller.jpa;

import club.taekwondo.entity.jpa.Cours;
import club.taekwondo.service.jpa.CoursService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/cours")
public class CoursController {

    @Autowired
    private CoursService coursService;

    // Endpoint pour récupérer tous les cours
    @GetMapping
    public List<Cours> getAllCours() {
        return coursService.getAllCours();
    }

    // Endpoint pour récupérer un cours par son ID
    @GetMapping("/{id}")
    public ResponseEntity<Cours> getCoursById(@PathVariable Long id) {
        Optional<Cours> cours = coursService.getCoursById(id);
        return cours.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Endpoint pour créer un nouveau cours
    @PostMapping
    public ResponseEntity<Cours> createCours(@RequestBody Cours cours) {
        Cours newCours = coursService.createCours(cours);
        return new ResponseEntity<>(newCours, HttpStatus.CREATED);
    }

    // Endpoint pour mettre à jour un cours existant
    @PutMapping("/{id}")
    public ResponseEntity<Cours> updateCours(@PathVariable Long id, @RequestBody Cours cours) {
        Cours updatedCours = coursService.updateCours(id, cours);
        return updatedCours != null ? ResponseEntity.ok(updatedCours) : ResponseEntity.notFound().build();
    }

    // Endpoint pour supprimer un cours
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCours(@PathVariable Long id) {
        coursService.deleteCours(id);
        return ResponseEntity.noContent().build();
    }
}
