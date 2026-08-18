package club.taekwondo.controller.jpa;

import club.taekwondo.dto.MentionsLegalesConfigDto;
import club.taekwondo.service.jpa.MentionsLegalesConfigService;
import club.taekwondo.service.jpa.UtilisateurService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mentions-legales-config")
public class MentionsLegalesConfigController {

    @Autowired
    private MentionsLegalesConfigService mentionsLegalesConfigService;

    @Autowired
    private UtilisateurService utilisateurService;

    @GetMapping
    public ResponseEntity<MentionsLegalesConfigDto> get(@RequestParam(required = false) Long clubId) {
        if (clubId == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(mentionsLegalesConfigService.get(clubId));
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<MentionsLegalesConfigDto> update(@RequestBody MentionsLegalesConfigDto dto, Authentication authentication) {
        Long clubId = resolveClubId(dto.getClubId(), authentication);
        if (clubId == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(mentionsLegalesConfigService.update(clubId, dto));
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
