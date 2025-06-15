package club.taekwondo.controller.jpa;

import club.taekwondo.dto.InscriptionEvenementDTO;
import club.taekwondo.service.jpa.InscriptionEvenementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inscriptions")
public class InscriptionEvenementController {

    @Autowired
    private InscriptionEvenementService inscriptionService;

    // 🔹 Récupérer toutes les inscriptions
    @GetMapping
    public ResponseEntity<List<InscriptionEvenementDTO>> getAllInscriptions() {
        return ResponseEntity.ok(inscriptionService.getAllInscriptions());
    }

    // 🔹 Récupérer une inscription par son ID
    @GetMapping("/{id}")
    public ResponseEntity<InscriptionEvenementDTO> getInscriptionById(@PathVariable Long id) {
        return inscriptionService.getInscriptionById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 🔹 Créer une nouvelle inscription
    @PostMapping
    public ResponseEntity<InscriptionEvenementDTO> inscrireMembre(@RequestBody InscriptionEvenementDTO dto) {
        try {
            InscriptionEvenementDTO created = inscriptionService.inscrireMembre(dto);
            return ResponseEntity.status(201).body(created);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // 🔹 Mettre à jour une inscription
    @PutMapping("/{id}")
    public ResponseEntity<InscriptionEvenementDTO> updateInscription(@PathVariable Long id, @RequestBody InscriptionEvenementDTO dto) {
        try {
            InscriptionEvenementDTO updated = inscriptionService.updateInscription(id, dto);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // 🔹 Supprimer une inscription
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> annulerInscription(@PathVariable Long id) {
        inscriptionService.annulerInscription(id);
        return ResponseEntity.noContent().build();
    }
}


