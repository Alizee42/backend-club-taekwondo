package club.taekwondo.controller.jpa;

import club.taekwondo.entity.jpa.Commande;
import club.taekwondo.service.jpa.CommandeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/commandes")
public class CommandeController {

    @Autowired
    private CommandeService commandeService;

    // Endpoint pour récupérer toutes les commandes
    @GetMapping
    public List<Commande> getAllCommandes() {
        return commandeService.getAllCommandes();
    }

    // Endpoint pour récupérer une commande par son ID
    @GetMapping("/{id}")
    public ResponseEntity<Commande> getCommandeById(@PathVariable Long id) {
        Optional<Commande> commande = commandeService.getCommandeById(id);
        return commande.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Endpoint pour créer une nouvelle commande
    @PostMapping
    public ResponseEntity<Commande> createCommande(@RequestBody Commande commande) {
        Commande savedCommande = commandeService.createCommande(commande);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedCommande);
    }

    // Endpoint pour mettre à jour une commande existante
    @PutMapping("/{id}")
    public ResponseEntity<Commande> updateCommande(@PathVariable Long id, @RequestBody Commande commande) {
        Commande updatedCommande = commandeService.updateCommande(id, commande);
        return updatedCommande != null ? ResponseEntity.ok(updatedCommande) : ResponseEntity.notFound().build();
    }

    // Endpoint pour supprimer une commande
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCommande(@PathVariable Long id) {
        commandeService.deleteCommande(id);
        return ResponseEntity.noContent().build();
    }
}