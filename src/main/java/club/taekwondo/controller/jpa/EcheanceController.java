package club.taekwondo.controller.jpa;

import club.taekwondo.dto.EcheanceDTO;
import club.taekwondo.service.jpa.EcheanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/echeances")
public class EcheanceController {

    @Autowired
    private EcheanceService echeanceService;

    // ✅ Utilise List<EcheanceDTO>
    @GetMapping
    public ResponseEntity<List<EcheanceDTO>> getAllEcheances() {
        System.out.println("Requête reçue : GET /api/echeances");
        List<EcheanceDTO> echeances = echeanceService.getAllEcheanceDTOs();
        System.out.println("Échéances récupérées : " + echeances);
        return ResponseEntity.ok(echeances);
    }

    // ✅ Inchangé : reste logique métier
    @PostMapping("/{id}/payer")
    public ResponseEntity<Map<String, String>> payerEcheance(@PathVariable Long id) {
        System.out.println("Requête reçue : POST /api/echeances/" + id + "/payer");
        echeanceService.payerEcheance(id);
        System.out.println("Échéance payée avec succès pour l'ID : " + id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Échéance payée avec succès.");
        return ResponseEntity.ok(response);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEcheance(@PathVariable Long id) {
        try {
            System.out.println("Suppression de l'échéance avec ID : " + id);
            echeanceService.delete(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            System.err.println("Erreur lors de la suppression de l'échéance : " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            System.err.println("Erreur inattendue lors de la suppression de l'échéance : " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
