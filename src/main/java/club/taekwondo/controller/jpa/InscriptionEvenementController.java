package club.taekwondo.controller.jpa;

import club.taekwondo.dto.InscriptionEvenementDTO;
import club.taekwondo.dto.InscriptionRequestDTO;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.service.jpa.InscriptionEvenementService;
import club.taekwondo.service.jpa.MembreService;
import club.taekwondo.service.jpa.UtilisateurService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inscriptions")
public class InscriptionEvenementController {

    private final InscriptionEvenementService inscriptionService;
    private final MembreService membreService;
    private final UtilisateurService utilisateurService;

    public InscriptionEvenementController(InscriptionEvenementService inscriptionService,
                                          MembreService membreService,
                                          UtilisateurService utilisateurService) {
        this.inscriptionService = inscriptionService;
        this.membreService = membreService;
        this.utilisateurService = utilisateurService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<InscriptionEvenementDTO>> getAllInscriptions() {
        return ResponseEntity.ok(inscriptionService.getAllInscriptions());
    }

    @GetMapping("/evenement/{evenementId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<InscriptionEvenementDTO>> getInscriptionsByEvenement(@PathVariable Long evenementId,
                                                                                    @RequestParam(required = false) String statut) {
        return ResponseEntity.ok(inscriptionService.getInscriptionsByEvenementAndStatut(evenementId, statut));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','PARENT','MEMBRE')")
    public ResponseEntity<InscriptionEvenementDTO> getInscriptionById(@PathVariable Long id,
                                                                      Authentication authentication) {
        return inscriptionService.getInscriptionById(id)
                .map(inscription -> {
                    assertCanAccessMember(authentication, inscription.getMembreId());
                    return ResponseEntity.ok(inscription);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','PARENT','MEMBRE')")
    public ResponseEntity<?> inscrireMembres(@RequestBody InscriptionRequestDTO request,
                                             Authentication authentication) {
        try {
            List<Long> membreIds = resolveMembreIds(request);

            if (membreIds == null || membreIds.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Aucun membre selectionne"));
            }

            for (Long membreId : membreIds) {
                assertCanAccessMember(authentication, membreId);
            }

            List<InscriptionEvenementDTO> inscriptions = inscriptionService.inscrireMembres(
                    request.getEvenementId(),
                    membreIds,
                    request.getCommentaire()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(inscriptions);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", e.getReason()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/me")
    @PreAuthorize("hasRole('MEMBRE')")
    public ResponseEntity<?> inscrireMembreConnecte(@RequestBody InscriptionRequestDTO request,
                                                    Authentication authentication) {
        try {
            Utilisateur currentUser = requireAuthenticatedUser(authentication);
            Long membreId = membreService.getMembreEntityByIdUtilisateur(currentUser.getId())
                    .map(membre -> membre.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membre introuvable pour cet utilisateur"));

            List<InscriptionEvenementDTO> inscriptions = inscriptionService.inscrireMembres(
                    request.getEvenementId(),
                    List.of(membreId),
                    request.getCommentaire()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(inscriptions.get(0));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", e.getReason()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('MEMBRE')")
    public ResponseEntity<List<InscriptionEvenementDTO>> getMesInscriptions(Authentication authentication) {
        Utilisateur currentUser = requireAuthenticatedUser(authentication);
        Long membreId = membreService.getMembreEntityByIdUtilisateur(currentUser.getId())
                .map(membre -> membre.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membre introuvable pour cet utilisateur"));

        return ResponseEntity.ok(inscriptionService.getInscriptionsByMembreId(membreId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<?> updateInscription(@PathVariable Long id,
                                               @RequestBody InscriptionEvenementDTO dto,
                                               Authentication authentication) {
        try {
            Long memberId = dto.getMembreId();
            if (memberId == null) {
                memberId = inscriptionService.getInscriptionById(id)
                        .map(InscriptionEvenementDTO::getMembreId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inscription introuvable"));
            }
            assertCanAccessMember(authentication, memberId);
            return ResponseEntity.ok(inscriptionService.updateInscription(id, dto));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", e.getReason()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/statut")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<?> updateStatutInscription(@PathVariable Long id,
                                                     @RequestParam String statut,
                                                     Authentication authentication) {
        try {
            Long memberId = inscriptionService.getInscriptionById(id)
                    .map(InscriptionEvenementDTO::getMembreId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inscription introuvable"));
            assertCanAccessMember(authentication, memberId);
            inscriptionService.updateStatutInscription(id, statut);
            return ResponseEntity.ok().build();
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", e.getReason()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','PARENT','MEMBRE')")
    public ResponseEntity<?> annulerInscription(@PathVariable Long id, Authentication authentication) {
        try {
            Long memberId = inscriptionService.getInscriptionById(id)
                    .map(InscriptionEvenementDTO::getMembreId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inscription introuvable"));
            assertCanAccessMember(authentication, memberId);
            inscriptionService.annulerInscription(id);
            return ResponseEntity.noContent().build();
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", e.getReason()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/club/{clubId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<InscriptionEvenementDTO>> getInscriptionsByClub(@PathVariable Long clubId,
                                                                               Authentication authentication) {
        assertAdminClubScope(authentication, clubId);
        return ResponseEntity.ok(inscriptionService.getInscriptionsByClubId(clubId));
    }

    private Utilisateur requireAuthenticatedUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifie");
        }
        return utilisateurService.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non trouve"));
    }

    private void assertAdminClubScope(Authentication authentication, Long clubId) {
        if (hasAnyRole(authentication, "SUPER_ADMIN")) {
            return;
        }

        Utilisateur currentUser = requireAuthenticatedUser(authentication);
        if (currentUser.getClub() == null
                || currentUser.getClub().getId() == null
                || !currentUser.getClub().getId().equals(clubId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acces refuse");
        }
    }

    private void assertCanAccessMember(Authentication authentication, Long membreId) {
        if (hasAnyRole(authentication, "SUPER_ADMIN")) {
            return;
        }

        Utilisateur currentUser = requireAuthenticatedUser(authentication);
        var member = membreService.findById(membreId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membre introuvable"));

        if (hasAnyRole(authentication, "ADMIN")) {
            if (currentUser.getClub() == null
                    || currentUser.getClub().getId() == null
                    || member.getClub() == null
                    || member.getClub().getId() == null
                    || !currentUser.getClub().getId().equals(member.getClub().getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acces refuse");
            }
            return;
        }

        if (hasAnyRole(authentication, "PARENT")) {
            boolean ownsChild = membreService.getEnfantsDuParent(currentUser.getId()).stream()
                    .anyMatch(enfant -> membreId.equals(enfant.getId()));
            if (!ownsChild) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acces refuse");
            }
            return;
        }

        if (hasAnyRole(authentication, "MEMBRE")) {
            Long ownMemberId = membreService.getMembreEntityByIdUtilisateur(currentUser.getId())
                    .map(membre -> membre.getId())
                    .orElse(null);
            if (ownMemberId == null || !ownMemberId.equals(membreId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acces refuse");
            }
            return;
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acces refuse");
    }

    private List<Long> resolveMembreIds(InscriptionRequestDTO request) {
        if (request.getMembreIds() != null && !request.getMembreIds().isEmpty()) {
            return request.getMembreIds();
        }
        return request.getEnfantsIds();
    }

    private boolean hasAnyRole(Authentication authentication, String... roles) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }

        for (String role : roles) {
            String authority = "ROLE_" + role;
            boolean found = authentication.getAuthorities().stream()
                    .anyMatch(grantedAuthority -> authority.equals(grantedAuthority.getAuthority()));
            if (found) {
                return true;
            }
        }

        return false;
    }
}
