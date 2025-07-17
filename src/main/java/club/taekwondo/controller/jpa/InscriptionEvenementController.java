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

    // 🔹 Récupérer les inscriptions par événement et statut
    @GetMapping("/evenement/{evenementId}")
    public ResponseEntity<List<InscriptionEvenementDTO>> getInscriptionsByEvenement(
            @PathVariable Long evenementId,
            @RequestParam(required = false) String statut) {
        return ResponseEntity.ok(inscriptionService.getInscriptionsByEvenementAndStatut(evenementId, statut));
    }

    // 🔹 Récupérer une inscription par ID
    @GetMapping("/{id}")
    public ResponseEntity<InscriptionEvenementDTO> getInscriptionById(@PathVariable Long id) {
        return inscriptionService.getInscriptionById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 🔹 Créer une nouvelle inscription
    @PostMapping
    public ResponseEntity<InscriptionEvenementDTO> inscrireMembre(@RequestBody InscriptionEvenementDTO dto) {
        return ResponseEntity.status(201).body(inscriptionService.inscrireMembre(dto));
    }

    // 🔹 Mettre à jour une inscription
    @PutMapping("/{id}")
    public ResponseEntity<InscriptionEvenementDTO> updateInscription(@PathVariable Long id, @RequestBody InscriptionEvenementDTO dto) {
        return ResponseEntity.ok(inscriptionService.updateInscription(id, dto));
    }

    // 🔹 Mettre à jour uniquement le statut d'une inscription
    @PatchMapping("/{id}/statut")
    public ResponseEntity<Void> updateStatutInscription(@PathVariable Long id, @RequestParam String statut) {
        try {
            inscriptionService.updateStatutInscription(id, statut);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build(); // Renvoie une erreur 400 en cas de problème
        }
    }

    // 🔹 Supprimer une inscription
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> annulerInscription(@PathVariable Long id) {
        inscriptionService.annulerInscription(id);
        return ResponseEntity.noContent().build();
    }
}
