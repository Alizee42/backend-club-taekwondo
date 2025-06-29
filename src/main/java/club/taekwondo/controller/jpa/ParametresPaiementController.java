package club.taekwondo.controller.jpa;

import club.taekwondo.dto.ParametresPaiementDTO;
import club.taekwondo.service.jpa.ParametresPaiementService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/parametres-paiement")
public class ParametresPaiementController {

    @Autowired
    private ParametresPaiementService parametresPaiementService;

    // Endpoint pour récupérer les paramètres
    @GetMapping
    public ParametresPaiementDTO getParametresPaiement() {
        return parametresPaiementService.getParametresPaiement();
    }

    // Endpoint pour mettre à jour les paramètres
    @PostMapping
    public ParametresPaiementDTO updateParametresPaiement(@RequestBody ParametresPaiementDTO parametres) {
        parametresPaiementService.updateParametresPaiement(parametres);
        return parametres;
    }
}