package club.taekwondo.controller.jpa;

import club.taekwondo.dto.PolitiqueConfidentialiteConfigDto;
import club.taekwondo.service.jpa.PolitiqueConfidentialiteConfigService;
import club.taekwondo.service.jpa.UtilisateurService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/politique-confidentialite-config")
public class PolitiqueConfidentialiteConfigController {

    @Autowired
    private PolitiqueConfidentialiteConfigService politiqueConfidentialiteConfigService;

    @Autowired
    private UtilisateurService utilisateurService;

    @GetMapping
    public ResponseEntity<PolitiqueConfidentialiteConfigDto> get(@RequestParam(required = false) Long clubId) {
        if (clubId == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(politiqueConfidentialiteConfigService.get(clubId));
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<PolitiqueConfidentialiteConfigDto> update(@RequestBody PolitiqueConfidentialiteConfigDto dto, Authentication authentication) {
        Long clubId = resolveClubId(dto.getClubId(), authentication);
        if (clubId == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(politiqueConfidentialiteConfigService.update(clubId, dto));
    }

    // ADMIN : club toujours dérivé de son propre compte. SUPER_ADMIN : doit fournir explicitement clubId.
    private Long resolveClubId(Long requestedClubId, Authentication authentication) {
        return utilisateurService.findByEmail(authentication.getName())
                .map(caller -> {
                    if (caller.getClub() != null) {
                        return caller.getClub().getId();
                    }
                    return requestedClubId;
                })
                .orElse(requestedClubId);
    }
}
