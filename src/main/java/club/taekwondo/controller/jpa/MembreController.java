package club.taekwondo.controller.jpa;

import club.taekwondo.entity.jpa.Membre;
import club.taekwondo.service.jpa.MembreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/membres")
@CrossOrigin(origins = "*")
public class MembreController {

    @Autowired
    private MembreService membreService;

    @GetMapping
    public List<Membre> getAllMembres() {
        return membreService.getAllMembres();
    }

    @GetMapping("/{id}")
    public Optional<Membre> getMembreById(@PathVariable Long id) {
        return membreService.getMembreById(id);
    }

    @PostMapping
    public Membre createMembre(@RequestBody Membre membre) {
        return membreService.createMembre(membre);
    }

    @PutMapping("/{id}")
    public Membre updateMembre(@PathVariable Long id, @RequestBody Membre membre) {
        return membreService.updateMembre(id, membre);
    }

    @DeleteMapping("/{id}")
    public void deleteMembre(@PathVariable Long id) {
        membreService.deleteMembre(id);
    }
}

