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
}
