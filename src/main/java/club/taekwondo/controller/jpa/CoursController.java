package club.taekwondo.controller.jpa;

import club.taekwondo.dto.CoursDTO;
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

    // 🔹 Récupérer tous les cours
    @GetMapping
    public List<CoursDTO> getAllCours() {
        return coursService.getAllCours();
    }

    // 🔹 Récupérer un cours par son ID
    @GetMapping("/{id}")
    public ResponseEntity<CoursDTO> getCoursById(@PathVariable Long id) {
        Optional<CoursDTO> cours = coursService.getCoursById(id);
        return cours.map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // 🔹 Créer un nouveau cours
    @PostMapping
    public ResponseEntity<CoursDTO> createCours(@RequestBody CoursDTO coursDTO) {
        CoursDTO newCours = coursService.createCours(coursDTO);
        return new ResponseEntity<>(newCours, HttpStatus.CREATED);
    }

    // 🔹 Mettre à jour un cours existant
    @PutMapping("/{id}")
    public ResponseEntity<CoursDTO> updateCours(@PathVariable Long id, @RequestBody CoursDTO coursDTO) {
        CoursDTO updatedCours = coursService.updateCours(id, coursDTO);
        return updatedCours != null ? ResponseEntity.ok(updatedCours)
                                    : ResponseEntity.notFound().build();
    }

    // 🔹 Supprimer un cours
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCours(@PathVariable Long id) {
        coursService.deleteCours(id);
        return ResponseEntity.noContent().build();
    }
}
