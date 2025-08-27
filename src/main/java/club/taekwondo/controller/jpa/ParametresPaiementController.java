package club.taekwondo.controller.jpa;

import club.taekwondo.dto.ParametresPaiementDTO;
import club.taekwondo.service.jpa.ParametresPaiementService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/parametres-paiement")
@CrossOrigin(origins = "*")
public class ParametresPaiementController {

    private static final Logger log = LoggerFactory.getLogger(ParametresPaiementController.class);

    private final ParametresPaiementService parametresPaiementService;

    public ParametresPaiementController(ParametresPaiementService parametresPaiementService) {
        this.parametresPaiementService = parametresPaiementService;
    }

    /** 🔓 Lecture publique pour la page membre (pas d'auth requise) */
    @PreAuthorize("permitAll()")
    @GetMapping("/public")
    public ResponseEntity<ParametresPaiementDTO> getParametresPaiementPublic() {
        log.debug("[CTRL] GET /api/parametres-paiement/public");
        ParametresPaiementDTO dto = parametresPaiementService.getParametresPaiement();
        return ResponseEntity.ok(dto);
    }

    /** 🔒 Lecture réservée ADMIN (inchangé) */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<ParametresPaiementDTO> getParametresPaiement(Authentication auth) {
        log.debug("[CTRL] GET /api/parametres-paiement by user={} roles={}",
                auth != null ? auth.getName() : "anonymous",
                auth != null ? auth.getAuthorities() : "[]");

        ParametresPaiementDTO dto = parametresPaiementService.getParametresPaiement();
        return ResponseEntity.ok(dto);
    }

    /** 🔒 Mise à jour réservée ADMIN (inchangé) */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ParametresPaiementDTO> updateParametresPaiement(
            Authentication auth,
            @RequestBody ParametresPaiementDTO parametres) {

        log.debug("[CTRL] POST /api/parametres-paiement by user={} roles={} payload={}",
                auth != null ? auth.getName() : "anonymous",
                auth != null ? auth.getAuthorities() : "[]",
                parametres);

        parametresPaiementService.updateParametresPaiement(parametres);
        return ResponseEntity.ok(parametres);
    }
}
