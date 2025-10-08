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
public class ParametresPaiementController {

    private static final Logger log = LoggerFactory.getLogger(ParametresPaiementController.class);

    private final ParametresPaiementService parametresPaiementService;

    public ParametresPaiementController(ParametresPaiementService parametresPaiementService) {
        this.parametresPaiementService = parametresPaiementService;
    }

    /** 🔓 Lecture publique (pas besoin d’authentification) */
    @GetMapping("/public/club/{clubId}")
    public ResponseEntity<ParametresPaiementDTO> getParametresPaiementPublic(@PathVariable Long clubId) {
        log.debug("[CTRL] GET /api/parametres-paiement/public/club/{}", clubId);
        ParametresPaiementDTO dto = parametresPaiementService.getParametresPaiementByClub(clubId);
        return ResponseEntity.ok(dto);
    }

    /** 🔒 Lecture réservée ADMIN par club */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/club/{clubId}")
    public ResponseEntity<ParametresPaiementDTO> getParametresPaiementByClub(@PathVariable Long clubId, Authentication auth) {
        log.debug("[CTRL] GET /api/parametres-paiement/club/{} by user={} roles={}",
                clubId,
                auth != null ? auth.getName() : "anonymous",
                auth != null ? auth.getAuthorities() : "[]");
        ParametresPaiementDTO dto = parametresPaiementService.getParametresPaiementByClub(clubId);
        return ResponseEntity.ok(dto);
    }

    /** 🔒 Mise à jour réservée ADMIN par club */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/club/{clubId}")
    public ResponseEntity<ParametresPaiementDTO> updateParametresPaiementByClub(@PathVariable Long clubId, Authentication auth, @RequestBody ParametresPaiementDTO parametres) {
        log.debug("[CTRL] POST /api/parametres-paiement/club/{} by user={} roles={} payload={}",
                clubId,
                auth != null ? auth.getName() : "anonymous",
                auth != null ? auth.getAuthorities() : "[]",
                parametres);
        parametresPaiementService.updateParametresPaiement(clubId, parametres);
        return ResponseEntity.ok(parametres);
    }
}
