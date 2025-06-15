package club.taekwondo.controller.jpa;

import club.taekwondo.dto.LigneCommandeDTO;
import club.taekwondo.service.jpa.LigneCommandeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/lignes-commande")
public class LigneCommandeController {

    @Autowired
    private LigneCommandeService ligneCommandeService;

    // ✅ Récupérer toutes les lignes de commande
    @GetMapping
    public ResponseEntity<List<LigneCommandeDTO>> getAllLignesCommande() {
        List<LigneCommandeDTO> lignes = ligneCommandeService.getAllLignesCommande();
        return ResponseEntity.ok(lignes);
    }

    // ✅ Récupérer une ligne par ID
    @GetMapping("/{id}")
    public ResponseEntity<LigneCommandeDTO> getLigneCommandeById(@PathVariable Long id) {
        Optional<LigneCommandeDTO> ligne = ligneCommandeService.getLigneCommandeById(id);
        return ligne.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ✅ Créer une nouvelle ligne
    @PostMapping
    public ResponseEntity<LigneCommandeDTO> createLigneCommande(@RequestBody LigneCommandeDTO dto) {
        LigneCommandeDTO created = ligneCommandeService.createLigneCommande(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ✅ Mettre à jour une ligne existante
    @PutMapping("/{id}")
    public ResponseEntity<LigneCommandeDTO> updateLigneCommande(@PathVariable Long id, @RequestBody LigneCommandeDTO dto) {
        try {
            LigneCommandeDTO updated = ligneCommandeService.updateLigneCommande(id, dto);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ✅ Supprimer une ligne
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLigneCommande(@PathVariable Long id) {
        ligneCommandeService.deleteLigneCommande(id);
        return ResponseEntity.noContent().build();
    }
}
