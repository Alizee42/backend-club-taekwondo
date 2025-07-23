package club.taekwondo.controller.jpa;

import club.taekwondo.dto.CommandeDTO;
import club.taekwondo.dto.CommandeUpdateDTO;
import club.taekwondo.service.jpa.CommandeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/commandes")
public class CommandeController {

    private static final Logger logger = LoggerFactory.getLogger(CommandeController.class);

    @Autowired
    private CommandeService commandeService;

    @GetMapping
    public ResponseEntity<List<CommandeDTO>> getAllCommandes() {
        logger.info("📦 Récupération de toutes les commandes");
        return ResponseEntity.ok(commandeService.getAllCommandes());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommandeDTO> getCommandeById(@PathVariable Long id) {
        logger.info("🔍 Récupération de la commande avec ID : {}", id);
        return commandeService.getCommandeById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> {
                    logger.warn("❌ Commande ID {} non trouvée", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @PostMapping
    public ResponseEntity<CommandeDTO> createCommande(@Valid @RequestBody CommandeDTO commandeDTO) {
        logger.info("📝 Création d'une commande simple : {}", commandeDTO);
        try {
            CommandeDTO saved = commandeService.createCommande(commandeDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (RuntimeException e) {
            logger.error("❌ Erreur création commande : {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/with-lignes")
    public ResponseEntity<CommandeDTO> createCommandeAvecLignes(@Valid @RequestBody CommandeDTO commandeDTO) {
        logger.info("🛒 Création commande avec lignes : {}", commandeDTO);
        try {
            CommandeDTO saved = commandeService.createCommandeWithLignes(commandeDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (RuntimeException e) {
            logger.error("❌ Erreur commande with-lignes : {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> mettreAJourCommande(@PathVariable Long id, @Valid @RequestBody CommandeUpdateDTO updateDTO) {
        logger.info("✏️ MAJ commande ID {} → statut='{}', modePaiement='{}', datePaiement={}",
                id, updateDTO.getStatut(), updateDTO.getModePaiement(), updateDTO.getDatePaiement());

        try {
            commandeService.mettreAJourCommande(id, updateDTO);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            logger.error("❌ Erreur MAJ commande ID {} : {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCommande(@PathVariable Long id) {
        logger.info("🗑️ Suppression commande ID : {}", id);
        try {
            commandeService.deleteCommande(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            logger.error("❌ Erreur suppression commande ID {} : {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/statut")
    public ResponseEntity<Void> changerStatutCommande(@PathVariable Long id, @RequestBody String nouveauStatut) {
        logger.info("🔄 Changement de statut commande ID {} → {}", id, nouveauStatut);
        try {
            CommandeUpdateDTO updateDTO = new CommandeUpdateDTO();
            updateDTO.setStatut(nouveauStatut);
            commandeService.mettreAJourCommande(id, updateDTO);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            logger.error("❌ Erreur changement statut ID {} : {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/disponibilite-club")
    public ResponseEntity<Void> definirDisponibiliteAuClub(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> payload) {
        boolean dispo = payload.getOrDefault("disponible", false);
        commandeService.definirDisponibiliteAuClub(id, dispo);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/paiement-club")
    public ResponseEntity<List<CommandeDTO>> getCommandesPaiementClub() {
        logger.info("📥 Récupération des commandes à payer au club");
        List<CommandeDTO> commandes = commandeService.getCommandesPaiementClub();
        return ResponseEntity.ok(commandes);
    }

    @PutMapping("/{id}/valider")
    public ResponseEntity<Void> validerPaiementManuel(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload) {
        logger.info("💶 Validation manuelle du paiement pour commande ID : {}", id);
        try {
            String statut = (String) payload.get("statut");
            String modePaiement = (String) payload.get("modePaiement");
            String datePaiement = (String) payload.get("datePaiement");

            if (statut == null || modePaiement == null || datePaiement == null) {
                logger.error("❌ Données manquantes pour la validation du paiement");
                return ResponseEntity.badRequest().build();
            }

            commandeService.validerPaiementManuel(id, statut, modePaiement, datePaiement);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            logger.error("❌ Erreur validation manuelle commande ID {} : {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}