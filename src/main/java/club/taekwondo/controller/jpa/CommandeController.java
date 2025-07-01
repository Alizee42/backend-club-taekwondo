package club.taekwondo.controller.jpa;

import club.taekwondo.dto.CommandeDTO;
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

    // 🔹 Récupérer toutes les commandes
    @GetMapping
    public ResponseEntity<List<CommandeDTO>> getAllCommandes() {
        System.out.println("Récupération de toutes les commandes");
        List<CommandeDTO> commandes = commandeService.getAllCommandes();
        return ResponseEntity.ok(commandes);
    }

    // 🔹 Récupérer une commande par ID
    @GetMapping("/{id}")
    public ResponseEntity<CommandeDTO> getCommandeById(@PathVariable Long id) {
        System.out.println("Récupération de la commande avec ID : " + id);
        Optional<CommandeDTO> commande = commandeService.getCommandeById(id);
        return commande.map(ResponseEntity::ok)
                       .orElseGet(() -> {
                           System.out.println("Commande avec ID " + id + " non trouvée");
                           return ResponseEntity.notFound().build();
                       });
    }

    // 🔹 Créer une commande simple (sans lignes)
    @PostMapping
    public ResponseEntity<CommandeDTO> createCommande(@RequestBody CommandeDTO commandeDTO) {
        System.out.println("Création d'une commande simple : " + commandeDTO);
        CommandeDTO saved = commandeService.createCommande(commandeDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // 🔹 Créer une commande avec ses lignes (boutique)
    @PostMapping("/with-lignes")
    public ResponseEntity<CommandeDTO> createCommandeAvecLignes(@RequestBody CommandeDTO commandeDTO) {
        try {
            System.out.println("Création d'une commande avec lignes : " + commandeDTO);
            CommandeDTO saved = commandeService.createCommandeWithLignes(commandeDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (RuntimeException e) {
            System.out.println("Erreur lors de la création de la commande avec lignes : " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    // 🔹 Mettre à jour une commande
    @PutMapping("/{id}")
    public ResponseEntity<CommandeDTO> updateCommande(@PathVariable Long id, @RequestBody CommandeDTO commandeDTO) {
        try {
            System.out.println("Mise à jour de la commande avec ID : " + id);
            CommandeDTO updated = commandeService.updateCommande(id, commandeDTO);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            System.out.println("Erreur lors de la mise à jour de la commande avec ID " + id + " : " + e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    // 🔹 Supprimer une commande
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCommande(@PathVariable Long id) {
        try {
            System.out.println("Suppression de la commande avec ID : " + id);
            commandeService.deleteCommande(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            System.out.println("Erreur lors de la suppression de la commande avec ID " + id + " : " + e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}
