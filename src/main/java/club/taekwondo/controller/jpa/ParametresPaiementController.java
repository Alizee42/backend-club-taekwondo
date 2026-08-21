package club.taekwondo.controller.jpa;

import club.taekwondo.dto.ParametresPaiementDTO;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.repository.jpa.UtilisateurRepository;
import club.taekwondo.service.jpa.ParametresPaiementService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/parametres-paiement")
public class ParametresPaiementController {

    private static final Logger log = LoggerFactory.getLogger(ParametresPaiementController.class);

    private final ParametresPaiementService parametresPaiementService;
    private final UtilisateurRepository utilisateurRepository;

    public ParametresPaiementController(ParametresPaiementService parametresPaiementService,
                                         UtilisateurRepository utilisateurRepository) {
        this.parametresPaiementService = parametresPaiementService;
        this.utilisateurRepository = utilisateurRepository;
    }

    // Un ADMIN ne doit gerer que la cotisation de son propre club ; un SUPER_ADMIN
    // n'a pas cette restriction. Renvoie une reponse 401/403 a retourner tel quel
    // si l'acces doit etre refuse, sinon null.
    private ResponseEntity<?> refuserSiPasProprietaireDuClub(Long clubId, Authentication authentication) {
        if (authentication == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        boolean isSuperAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a.getAuthority()));
        if (isSuperAdmin) return null;
        Utilisateur caller = utilisateurRepository.findByEmail(authentication.getName()).orElse(null);
        if (caller == null || caller.getClub() == null || !caller.getClub().getId().equals(clubId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return null;
    }

    /** 🔓 Lecture publique (pas besoin d’authentification) */
    @GetMapping("/public/club/{clubId}")
    public ResponseEntity<ParametresPaiementDTO> getParametresPaiementPublic(@PathVariable Long clubId) {
        log.debug("[CTRL] GET /api/parametres-paiement/public/club/{}", clubId);
        ParametresPaiementDTO dto = parametresPaiementService.getParametresPaiementByClub(clubId);
        return ResponseEntity.ok(dto);
    }

    /** 🔒 Lecture réservée ADMIN par club */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/club/{clubId}")
    public ResponseEntity<ParametresPaiementDTO> getParametresPaiementByClub(@PathVariable Long clubId, Authentication auth) {
        log.debug("[CTRL] GET /api/parametres-paiement/club/{} by user={} roles={}",
                clubId,
                auth != null ? auth.getName() : "anonymous",
                auth != null ? auth.getAuthorities() : "[]");
        ResponseEntity<?> refus = refuserSiPasProprietaireDuClub(clubId, auth);
        if (refus != null) return ResponseEntity.status(refus.getStatusCode()).build();
        ParametresPaiementDTO dto = parametresPaiementService.getParametresPaiementByClub(clubId);
        return ResponseEntity.ok(dto);
    }

    /** 🔒 Mise à jour réservée ADMIN par club */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @PostMapping("/club/{clubId}")
    public ResponseEntity<ParametresPaiementDTO> updateParametresPaiementByClub(@PathVariable Long clubId, Authentication auth, @RequestBody ParametresPaiementDTO parametres) {
        log.debug("[CTRL] POST /api/parametres-paiement/club/{} by user={} roles={} payload={}",
                clubId,
                auth != null ? auth.getName() : "anonymous",
                auth != null ? auth.getAuthorities() : "[]",
                parametres);
        ResponseEntity<?> refus = refuserSiPasProprietaireDuClub(clubId, auth);
        if (refus != null) return ResponseEntity.status(refus.getStatusCode()).build();
        parametresPaiementService.updateParametresPaiement(clubId, parametres);
        return ResponseEntity.ok(parametres);
    }
}
