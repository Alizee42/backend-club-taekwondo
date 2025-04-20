package club.taekwondo.controller.jpa;

import club.taekwondo.entity.jpa.Professeur;
import club.taekwondo.service.jpa.ProfesseurService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/professeurs")
public class ProfesseurController {

    private final ProfesseurService professeurService;

    public ProfesseurController(ProfesseurService professeurService) {
        this.professeurService = professeurService;
    }

    @GetMapping
    public List<Professeur> getAllProfesseurs() {
        return professeurService.getAllProfesseurs();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Professeur> getProfesseurById(@PathVariable Long id) {
        return professeurService.getProfesseurById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Professeur createProfesseur(@RequestBody Professeur professeur) {
        return professeurService.createProfesseur(professeur);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Professeur> updateProfesseur(@PathVariable Long id, @RequestBody Professeur updatedProf) {
        try {
            return ResponseEntity.ok(professeurService.updateProfesseur(id, updatedProf));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProfesseur(@PathVariable Long id) {
        professeurService.deleteProfesseur(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/specialite/{specialite}")
    public List<Professeur> getBySpecialite(@PathVariable String specialite) {
        return professeurService.getProfesseursBySpecialite(specialite);
    }
}

