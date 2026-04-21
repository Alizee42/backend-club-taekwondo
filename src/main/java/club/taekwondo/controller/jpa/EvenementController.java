package club.taekwondo.controller.jpa;

import club.taekwondo.dto.EvenementDTO;
import club.taekwondo.dto.InscriptionEvenementDTO;
import club.taekwondo.entity.jpa.Evenement;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.service.jpa.EvenementService;
import club.taekwondo.service.jpa.InscriptionEvenementService;
import club.taekwondo.service.jpa.UtilisateurService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/evenements")
public class EvenementController {

    private final EvenementService evenementService;
    private final InscriptionEvenementService inscriptionService;
    private final UtilisateurService utilisateurService;

    public EvenementController(EvenementService evenementService,
                               InscriptionEvenementService inscriptionService,
                               UtilisateurService utilisateurService) {
        this.evenementService = evenementService;
        this.inscriptionService = inscriptionService;
        this.utilisateurService = utilisateurService;
    }

    @GetMapping
    public ResponseEntity<List<EvenementDTO>> getAllEvenements() {
        return ResponseEntity.ok(evenementService.getAllEvenements());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EvenementDTO> getEvenementById(@PathVariable Long id) {
        Optional<EvenementDTO> evenement = evenementService.getEvenementById(id);
        return evenement.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/actifs")
    public ResponseEntity<List<EvenementDTO>> getEvenementsActifs() {
        return ResponseEntity.ok(evenementService.getEvenementsActifs());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<EvenementDTO> ajouterEvenement(@RequestParam("titre") String titre,
                                                         @RequestParam("dateDebut") String dateDebut,
                                                         @RequestParam("dateFin") String dateFin,
                                                         @RequestParam("lieu") String lieu,
                                                         @RequestParam("capacite") int capacite,
                                                         @RequestParam("description") String description,
                                                         @RequestParam("image") MultipartFile image,
                                                         @RequestParam(value = "clubId", required = false) Long clubId,
                                                         Authentication authentication) {
        try {
            Long scopedClubId = resolveScopedClubId(authentication, clubId);
            EvenementDTO created = evenementService.ajouterEvenement(
                    titre,
                    dateDebut,
                    dateFin,
                    lieu,
                    capacite,
                    description,
                    image,
                    scopedClubId
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<EvenementDTO> updateEvenement(@PathVariable Long id,
                                                        @RequestBody EvenementDTO dto,
                                                        Authentication authentication) {
        assertEventInScope(authentication, id);
        try {
            EvenementDTO updated = evenementService.updateEvenement(id, dto);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<EvenementDTO> updateEvenementMultipart(@PathVariable Long id,
                                                                 @RequestParam("titre") String titre,
                                                                 @RequestParam("dateDebut") String dateDebut,
                                                                 @RequestParam("dateFin") String dateFin,
                                                                 @RequestParam("lieu") String lieu,
                                                                 @RequestParam("capacite") int capacite,
                                                                 @RequestParam("description") String description,
                                                                 @RequestParam(value = "image", required = false) MultipartFile image,
                                                                 Authentication authentication) {
        assertEventInScope(authentication, id);

        try {
            EvenementDTO dto = new EvenementDTO();
            dto.setTitre(titre);
            dto.setDateDebut(parseDateTime(dateDebut));
            dto.setDateFin(parseDateTime(dateFin));
            dto.setLieu(lieu);
            dto.setCapacite(capacite);
            dto.setDescription(description);
            if (image != null && !image.isEmpty()) {
                dto.setImageFilename(evenementService.saveImage(image));
            }

            return ResponseEntity.ok(evenementService.updateEvenement(id, dto));
        } catch (ResponseStatusException e) {
            throw e;
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}/statut")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<EvenementDTO> changerStatutEvenement(@PathVariable Long id,
                                                               @RequestBody Map<String, Boolean> statutMap,
                                                               Authentication authentication) {
        assertEventInScope(authentication, id);
        try {
            Boolean actif = statutMap.get("actif");
            EvenementDTO updated = evenementService.changerStatutEvenement(id, actif);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Void> deleteEvenement(@PathVariable Long id, Authentication authentication) {
        assertEventInScope(authentication, id);
        try {
            evenementService.deleteEvenement(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/inscriptions-enfants")
    @PreAuthorize("hasAnyRole('PARENT','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<InscriptionEvenementDTO>> getInscriptionsEnfants(@RequestParam Long parentId,
                                                                                Authentication authentication) {
        Utilisateur currentUser = requireAuthenticatedUser(authentication);
        if (!hasAnyRole(authentication, "ADMIN", "SUPER_ADMIN") && !parentId.equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (hasAnyRole(authentication, "ADMIN") && !hasAnyRole(authentication, "SUPER_ADMIN")) {
            Utilisateur targetParent = utilisateurService.getUtilisateurEntityById(parentId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            if (currentUser.getClub() == null
                    || targetParent.getClub() == null
                    || !currentUser.getClub().getId().equals(targetParent.getClub().getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }

        return ResponseEntity.ok(inscriptionService.getInscriptionsByParent(parentId));
    }

    @GetMapping("/club/{clubId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','PARENT','MEMBRE')")
    public ResponseEntity<List<EvenementDTO>> getEvenementsByClub(@PathVariable Long clubId,
                                                                  Authentication authentication) {
        Long scopedClubId = resolveScopedClubId(authentication, clubId);
        return ResponseEntity.ok(evenementService.getEvenementsByClubId(scopedClubId));
    }

    private Utilisateur requireAuthenticatedUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifie");
        }
        return utilisateurService.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non trouve"));
    }

    private Long resolveScopedClubId(Authentication authentication, Long requestedClubId) {
        Utilisateur currentUser = requireAuthenticatedUser(authentication);

        if (hasAnyRole(authentication, "SUPER_ADMIN")) {
            if (requestedClubId != null) {
                return requestedClubId;
            }
            if (currentUser.getClub() != null) {
                return currentUser.getClub().getId();
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "clubId requis pour ce compte");
        }

        if (currentUser.getClub() == null || currentUser.getClub().getId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Aucun club rattache");
        }

        Long currentClubId = currentUser.getClub().getId();
        if (requestedClubId != null && !currentClubId.equals(requestedClubId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acces refuse");
        }
        return currentClubId;
    }

    private void assertEventInScope(Authentication authentication, Long evenementId) {
        if (hasAnyRole(authentication, "SUPER_ADMIN")) {
            return;
        }

        Utilisateur currentUser = requireAuthenticatedUser(authentication);
        Evenement evenement = evenementService.getEvenementEntityById(evenementId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (currentUser.getClub() == null
                || currentUser.getClub().getId() == null
                || evenement.getClub() == null
                || evenement.getClub().getId() == null
                || !currentUser.getClub().getId().equals(evenement.getClub().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acces refuse");
        }
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

    private LocalDateTime parseDateTime(String value) {
        try {
            return LocalDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME);
        } catch (DateTimeParseException ignored) {
            return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
    }
}
