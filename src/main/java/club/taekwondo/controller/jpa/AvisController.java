package club.taekwondo.controller.jpa;

import club.taekwondo.dto.AvisDTO;
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

    // 🔹 Récupérer tous les avis
    @GetMapping
    public ResponseEntity<List<AvisDTO>> getAllAvis() {
        List<AvisDTO> avisList = avisService.getAllAvis();
        return ResponseEntity.ok(avisList);
    }

    // 🔹 Ajouter un nouvel avis
    @PostMapping
    public ResponseEntity<AvisDTO> createAvis(@RequestBody AvisDTO avisDTO) {
        avisDTO.setDatePub(LocalDate.now());
        avisDTO.setApprouve(false); // Par défaut, l'avis n'est pas approuvé
        AvisDTO savedAvis = avisService.createAvis(avisDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedAvis);
    }

    // 🔹 Mettre à jour un avis existant
    @PutMapping("/{id}")
    public ResponseEntity<AvisDTO> updateAvis(@PathVariable Integer id, @RequestBody AvisDTO avisDTO) {
        try {
            AvisDTO updatedAvis = avisService.updateAvis(id, avisDTO);
            return ResponseEntity.ok(updatedAvis);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    // 🔹 Approuver un avis
    @PutMapping("/{id}/approuver")
    public ResponseEntity<AvisDTO> approuverAvis(@PathVariable Integer id) {
        try {
            AvisDTO approuveAvis = avisService.approuverAvis(id);
            return ResponseEntity.ok(approuveAvis);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    // 🔹 Supprimer un avis
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAvis(@PathVariable Integer id) {
        avisService.deleteAvis(id);
        return ResponseEntity.noContent().build();
    }
}
