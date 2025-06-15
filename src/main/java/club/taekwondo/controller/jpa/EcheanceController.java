package club.taekwondo.controller.jpa;

import club.taekwondo.dto.EcheanceDTO;
import club.taekwondo.service.jpa.EcheanceService;
import org.springframework.beans.factory.annotation.Autowired;
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
        List<EcheanceDTO> echeances = echeanceService.getAllEcheanceDTOs();
        return ResponseEntity.ok(echeances);
    }

    // ✅ Inchangé : reste logique métier
    @PostMapping("/{id}/payer")
    public ResponseEntity<Map<String, String>> payerEcheance(@PathVariable Long id) {
        echeanceService.payerEcheance(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Échéance payée avec succès.");
        return ResponseEntity.ok(response);
    }
}
