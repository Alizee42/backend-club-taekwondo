
package club.taekwondo.controller.jpa;

import club.taekwondo.entity.jpa.Horaire;
import club.taekwondo.entity.jpa.Club;
import club.taekwondo.service.jpa.HoraireService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/horaires")
public class HoraireController {
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @PutMapping("/{id}")
    public Horaire updateHoraire(@PathVariable Long id, @RequestBody Horaire horaire) {
        horaire.setId(id);
        return horaireService.updateHoraire(horaire);
    }
    @Autowired
    private HoraireService horaireService;

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
    public Horaire addHoraire(@PathVariable Long clubId, @RequestBody Horaire horaire) {
        // Associer le club à l'horaire
        horaire.setClub(new Club());
        horaire.getClub().setId(clubId);
        return horaireService.addHoraire(horaire);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @DeleteMapping("/{id}")
    public void deleteHoraire(@PathVariable Long id) {
        horaireService.deleteHoraire(id);
    }
}
