package club.taekwondo.controller.jpa;

import club.taekwondo.dto.InscriptionEvenementDTO;
import club.taekwondo.dto.InscriptionRequestDTO;
import club.taekwondo.service.jpa.InscriptionEvenementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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

    // 🔹 Récupérer les inscriptions par événement (option : filtre par statut)
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

    // 🔧 DEBUG : Endpoint pour tester la récupération des données
    @GetMapping("/debug/evenement/{evenementId}")
    public ResponseEntity<Map<String, Object>> debugInscriptions(@PathVariable Long evenementId) {
        try {
            List<InscriptionEvenementDTO> inscriptions = inscriptionService.getInscriptionsByEvenementAndStatut(evenementId, null);
            Map<String, Object> debug = Map.of(
                "totalInscriptions", inscriptions.size(),
                "inscriptions", inscriptions,
                "message", "Debug des inscriptions pour l'événement " + evenementId
            );
            return ResponseEntity.ok(debug);
        } catch (Exception e) {
            Map<String, Object> error = Map.of(
                "error", e.getMessage(),
                "stackTrace", e.getStackTrace()
            );
            return ResponseEntity.badRequest().body(error);
        }
    }

    // 🔹 Créer une nouvelle inscription (plusieurs enfants)
    @PostMapping
    public ResponseEntity<?> inscrireMembres(@RequestBody InscriptionRequestDTO request) {
        try {
            List<InscriptionEvenementDTO> inscriptions = inscriptionService.inscrireMembres(
                    request.getEvenementId(),
                    request.getEnfantsIds(),
                    request.getCommentaire()
            );
            return ResponseEntity.status(201).body(inscriptions);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 🔹 Mettre à jour une inscription complète
    @PutMapping("/{id}")
    public ResponseEntity<?> updateInscription(@PathVariable Long id, @RequestBody InscriptionEvenementDTO dto) {
        try {
            return ResponseEntity.ok(inscriptionService.updateInscription(id, dto));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 🔹 Mettre à jour uniquement le statut
    @PatchMapping("/{id}/statut")
    public ResponseEntity<?> updateStatutInscription(@PathVariable Long id, @RequestParam String statut) {
        try {
            inscriptionService.updateStatutInscription(id, statut);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 🔹 Annuler une inscription (soft delete)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> annulerInscription(@PathVariable Long id) {
        try {
            inscriptionService.annulerInscription(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}