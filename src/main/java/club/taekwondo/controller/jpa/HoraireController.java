
package club.taekwondo.controller.jpa;

import club.taekwondo.entity.jpa.Horaire;
import club.taekwondo.entity.jpa.Club;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.repository.jpa.UtilisateurRepository;
import club.taekwondo.service.jpa.HoraireService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/horaires")
public class HoraireController {
    @Autowired
    private HoraireService horaireService;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    // Un ADMIN ne doit gerer que les horaires de son propre club ; un SUPER_ADMIN
    // n'a pas cette restriction. Renvoie une reponse 401/403 a retourner tel quel
    // si l'acces doit etre refuse, sinon null.
    private ResponseEntity<?> refuserSiPasProprietaireDuClub(Long clubId, Authentication authentication) {
        if (authentication == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        boolean isSuperAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a.getAuthority()));
        if (isSuperAdmin) return null;
        Utilisateur caller = utilisateurRepository.findByEmail(authentication.getName()).orElse(null);
        if (caller == null || caller.getClub() == null || clubId == null || !caller.getClub().getId().equals(clubId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return null;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateHoraire(@PathVariable Long id, @RequestBody Horaire horaire, Authentication auth) {
        Horaire existant = horaireService.getHoraireById(id).orElse(null);
        if (existant == null) return ResponseEntity.notFound().build();
        Long clubExistant = existant.getClub() != null ? existant.getClub().getId() : null;
        ResponseEntity<?> refus = refuserSiPasProprietaireDuClub(clubExistant, auth);
        if (refus != null) return refus;

        horaire.setId(id);
        horaire.setClub(existant.getClub());
        return ResponseEntity.ok(horaireService.updateHoraire(horaire));
    }

    @GetMapping("/all")
    public List<Horaire> getAllHoraires() {
        return horaireService.getAllHoraires();
    }

    @GetMapping("/club/{clubId}")
    public List<Horaire> getHorairesByClub(@PathVariable Long clubId) {
        return horaireService.getHorairesByClub(clubId);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @PostMapping("/club/{clubId}")
    public ResponseEntity<?> addHoraire(@PathVariable Long clubId, @RequestBody Horaire horaire, Authentication auth) {
        ResponseEntity<?> refus = refuserSiPasProprietaireDuClub(clubId, auth);
        if (refus != null) return refus;

        // Associer le club à l'horaire
        horaire.setClub(new Club());
        horaire.getClub().setId(clubId);
        return ResponseEntity.ok(horaireService.addHoraire(horaire));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteHoraire(@PathVariable Long id, Authentication auth) {
        Horaire existant = horaireService.getHoraireById(id).orElse(null);
        if (existant == null) return ResponseEntity.notFound().build();
        Long clubExistant = existant.getClub() != null ? existant.getClub().getId() : null;
        ResponseEntity<?> refus = refuserSiPasProprietaireDuClub(clubExistant, auth);
        if (refus != null) return refus;

        horaireService.deleteHoraire(id);
        return ResponseEntity.ok().build();
    }
}
