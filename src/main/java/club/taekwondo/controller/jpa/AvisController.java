package club.taekwondo.controller.jpa;

import club.taekwondo.entity.jpa.Avis;
import club.taekwondo.service.jpa.AvisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/avis")
public class AvisController {

    @Autowired
    private AvisService avisService;

    // Récupérer tous les avis
    @GetMapping
    public ResponseEntity<List<Avis>> getAllAvis() {
        List<Avis> avisList = avisService.getAllAvis();
        return ResponseEntity.ok(avisList);
    }

    // Ajouter un nouvel avis
    @PostMapping
    public ResponseEntity<Avis> createAvis(@RequestBody Avis avis) {
        avis.setDatePub(LocalDate.now());
        avis.setApprouve(false); // Par défaut, l'avis n'est pas approuvé
        Avis savedAvis = avisService.createAvis(avis);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedAvis);
    }

    // Mettre à jour un avis existant
    @PutMapping("/{id}")
    public ResponseEntity<Avis> updateAvis(@PathVariable Integer id, @RequestBody Avis avisDetails) {
        try {
            Avis updatedAvis = avisService.updateAvis(id, avisDetails);
            return ResponseEntity.ok(updatedAvis);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    // Approuver un avis
    @PutMapping("/{id}/approuver")
    public ResponseEntity<Avis> approuverAvis(@PathVariable Integer id) {
        try {
            Avis approuveAvis = avisService.approuverAvis(id);
            return ResponseEntity.ok(approuveAvis);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    // Supprimer un avis
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAvis(@PathVariable Integer id) {
        avisService.deleteAvis(id);
        return ResponseEntity.noContent().build();
    }
}