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
        List<CommandeDTO> commandes = commandeService.getAllCommandes();
        return ResponseEntity.ok(commandes);
    }

    // 🔹 Récupérer une commande par ID
    @GetMapping("/{id}")
    public ResponseEntity<CommandeDTO> getCommandeById(@PathVariable Long id) {
        Optional<CommandeDTO> commande = commandeService.getCommandeById(id);
        return commande.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    // 🔹 Créer une commande
    @PostMapping
    public ResponseEntity<CommandeDTO> createCommande(@RequestBody CommandeDTO commandeDTO) {
        CommandeDTO saved = commandeService.createCommande(commandeDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // 🔹 Mettre à jour une commande
    @PutMapping("/{id}")
    public ResponseEntity<CommandeDTO> updateCommande(@PathVariable Long id, @RequestBody CommandeDTO commandeDTO) {
        try {
            CommandeDTO updated = commandeService.updateCommande(id, commandeDTO);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // 🔹 Supprimer une commande
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCommande(@PathVariable Long id) {
        commandeService.deleteCommande(id);
        return ResponseEntity.noContent().build();
    }
}
