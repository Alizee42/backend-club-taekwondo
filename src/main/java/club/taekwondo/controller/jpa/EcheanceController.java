package club.taekwondo.controller.jpa;

import club.taekwondo.dto.EcheanceDTO;
import club.taekwondo.service.jpa.EcheanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

@RestController
@RequestMapping("/api/echeances")
public class EcheanceController {

    private static final Logger log = LoggerFactory.getLogger(EcheanceController.class);
    /** Échéances filtrées par club */
    @GetMapping("/club/{clubId}")
    public ResponseEntity<List<EcheanceDTO>> getEcheancesByClub(@PathVariable Long clubId) {
        List<EcheanceDTO> echeances = echeanceService.getEcheancesByClubId(clubId);
        return echeances.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(echeances);
    }

    @Autowired
    private EcheanceService echeanceService;

    // ✅ Liste des échéances (DTO)
    @GetMapping
    public ResponseEntity<List<EcheanceDTO>> getAllEcheances() {
        log.debug("Requête reçue : GET /api/echeances");
        List<EcheanceDTO> echeances = echeanceService.getAllEcheanceDTOs();
        log.debug("Échéances récupérées : {}", echeances.size());
        return ResponseEntity.ok(echeances);
    }

    /**
     * ✅ Marquer une échéance comme payée (compatible legacy)
     * - Si aucun body n'est fourni → on marque payé (date du jour), sans mode/référence
     * - Si body fourni : { "modePaiement": "cb|virement|espèces", "reference": "...", "datePaiementReel": "YYYY-MM-DD" }
     */
    @PostMapping("/{id}/payer")
    public ResponseEntity<?> payerEcheance(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body
    ) {
        log.debug("Requête reçue : POST /api/echeances/{}/payer", id);

        try {
            String mode = null;
            String reference = null;
            LocalDate date = null;

            if (body != null) {
                mode = body.getOrDefault("modePaiement", null);
                reference = body.get("reference");
                String rawDate = body.get("datePaiementReel");
                if (rawDate != null && !rawDate.isBlank()) {
                    try {
                        date = LocalDate.parse(rawDate);
                    } catch (DateTimeParseException e) {
                        return ResponseEntity.badRequest().body(Map.of(
                                "error", "Format de datePaiementReel invalide. Attendu: YYYY-MM-DD"
                        ));
                    }
                }
            }

            // 👉 Appel du service nouvelle signature (gère aussi le cas "legacy")
            EcheanceDTO dto = echeanceService.payerEcheance(id, mode, reference, date);

            log.info("Échéance payée avec succès pour l'ID : {}", id);
            return ResponseEntity.ok(dto);

        } catch (IllegalStateException ise) {
            // déjà payée, etc.
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", ise.getMessage()
            ));
        } catch (NoSuchElementException | IllegalArgumentException notFound) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", notFound.getMessage()
            ));
        } catch (Exception e) {
            log.error("Erreur lors du paiement de l'échéance : {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Erreur interne lors du paiement de l'échéance"
            ));
        }
    }

    // 🔹 Suppression d'une échéance
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEcheance(@PathVariable Long id) {
        try {
            log.debug("Suppression de l'échéance avec ID : {}", id);
            echeanceService.delete(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            log.warn("Erreur lors de la suppression de l'échéance : {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Erreur inattendue lors de la suppression de l'échéance : {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

