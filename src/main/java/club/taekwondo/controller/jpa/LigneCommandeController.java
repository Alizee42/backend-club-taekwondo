package club.taekwondo.controller.jpa;

import club.taekwondo.entity.jpa.LigneCommande;
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

    // Endpoint pour récupérer toutes les lignes de commande
    @GetMapping
    public List<LigneCommande> getAllLignesCommande() {
        return ligneCommandeService.getAllLignesCommande();
    }

    // Endpoint pour récupérer une ligne de commande par son ID
    @GetMapping("/{id}")
    public ResponseEntity<LigneCommande> getLigneCommandeById(@PathVariable Long id) {
        Optional<LigneCommande> ligneCommande = ligneCommandeService.getLigneCommandeById(id);
        return ligneCommande.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Endpoint pour créer une nouvelle ligne de commande
    @PostMapping
    public ResponseEntity<LigneCommande> createLigneCommande(@RequestBody LigneCommande ligneCommande) {
        LigneCommande savedLigneCommande = ligneCommandeService.createLigneCommande(ligneCommande);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedLigneCommande);
    }

    // Endpoint pour mettre à jour une ligne de commande existante
    @PutMapping("/{id}")
    public ResponseEntity<LigneCommande> updateLigneCommande(@PathVariable Long id, @RequestBody LigneCommande ligneCommande) {
        LigneCommande updatedLigneCommande = ligneCommandeService.updateLigneCommande(id, ligneCommande);
        return updatedLigneCommande != null ? ResponseEntity.ok(updatedLigneCommande) : ResponseEntity.notFound().build();
    }

    // Endpoint pour supprimer une ligne de commande
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLigneCommande(@PathVariable Long id) {
        ligneCommandeService.deleteLigneCommande(id);
        return ResponseEntity.noContent().build();
    }
}