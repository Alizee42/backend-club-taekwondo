package club.taekwondo.controller.jpa;

import club.taekwondo.dto.PaiementDTO;
import club.taekwondo.dto.PaiementRequestDTO;
import club.taekwondo.entity.jpa.Paiement;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.service.jpa.PaiementAccessService;
import club.taekwondo.service.jpa.PaiementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/paiements")
public class PaiementParentController {

    private static final Logger log = LoggerFactory.getLogger(PaiementParentController.class);

    private final PaiementService paiementService;
    private final PaiementAccessService paiementAccessService;

    public PaiementParentController(PaiementService paiementService,
                                    PaiementAccessService paiementAccessService) {
        this.paiementService = paiementService;
        this.paiementAccessService = paiementAccessService;
    }

    @PreAuthorize("hasAnyRole('PARENT','ADMIN')")
    @GetMapping("/parent/mes-paiements")
    public ResponseEntity<List<PaiementDTO>> getPaiementsPourParentConnecte(Authentication authentication) {
        Utilisateur parent = paiementAccessService.requireAuthenticatedUser(authentication);

        List<Long> enfantsIds = paiementAccessService.getChildMemberIds(parent);
        List<PaiementDTO> byMembre = paiementService.getPaiementsParMembres(enfantsIds);
        List<PaiementDTO> byUser = paiementService.getPaiementsParUtilisateur(parent.getId());

        Map<Long, PaiementDTO> merged = new LinkedHashMap<>();
        byMembre.forEach(paiement -> merged.put(paiement.getId(), paiement));
        byUser.forEach(paiement -> merged.putIfAbsent(paiement.getId(), paiement));

        List<PaiementDTO> out = new ArrayList<>(merged.values());
        log.info("[PARENT] mes-paiements parentId={} -> {} paiements ({} via enfants, {} via user)",
                parent.getId(), out.size(), byMembre.size(), byUser.size());

        return ResponseEntity.ok(out);
    }

    @PreAuthorize("hasAnyRole('PARENT','ADMIN')")
    @PostMapping("/parent/ajouter")
    public ResponseEntity<PaiementDTO> ajouterPaiementParent(Authentication authentication,
                                                             @RequestBody Map<String, Object> body) {
        try {
            Utilisateur parent = paiementAccessService.requireAuthenticatedUser(authentication);

            Long membreId = PaiementRequestValues.longOrNull(body.get("membreId"));
            if (membreId == null || membreId <= 0) {
                return ResponseEntity.badRequest().build();
            }

            if (!paiementAccessService.hasAnyRole(authentication, "ADMIN", "SUPER_ADMIN")) {
                paiementAccessService.assertParentOwnsMember(parent, membreId);
            }

            PaiementRequestDTO req = new PaiementRequestDTO();
            req.setMembreId(membreId);

            String typeHuman = PaiementRequestValues.strOrNull(body.get("type"));
            String typeAlt = PaiementRequestValues.strOrNull(body.get("typePaiement"));
            req.setTypePaiement(PaiementRequestValues.normalizeTypeHuman(typeHuman != null ? typeHuman : typeAlt));

            String modeHuman = PaiementRequestValues.strOrNull(body.get("modePaiement"));
            req.setModePaiement(PaiementRequestValues.normalizeModeHuman(modeHuman));
            req.setMontantTotal(PaiementRequestValues.doubleOrNull(body.get("montantTotal")));
            req.setNombreEcheances(PaiementRequestValues.intOrNull(body.get("nombreEcheances")));
            req.setDatePaiement(LocalDate.now().toString());

            Paiement paiement = paiementService.ajouterPaiementParent(req, parent.getId());
            PaiementDTO out = paiementService.toPaiementDTO(paiement);
            return createdDTO("/api/paiements/" + out.getId(), out);
        } catch (Exception e) {
            log.error("[PARENT] Erreur ajout paiement parent: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private ResponseEntity<PaiementDTO> createdDTO(String location, PaiementDTO body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(location));
        return new ResponseEntity<>(body, headers, HttpStatus.CREATED);
    }
}
