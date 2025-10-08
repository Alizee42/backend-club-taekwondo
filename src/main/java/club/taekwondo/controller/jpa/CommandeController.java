package club.taekwondo.controller.jpa;

import club.taekwondo.dto.CommandeDTO;
import club.taekwondo.dto.CommandeUpdateDTO;
import club.taekwondo.service.jpa.CommandeService;
import club.taekwondo.service.jpa.UtilisateurService;
import club.taekwondo.entity.jpa.Utilisateur;
import org.springframework.security.core.Authentication;
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

    @Autowired
    private UtilisateurService utilisateurService;

    // =========================
    //      ADMIN / GLOBAL
    // =========================

    @GetMapping
    public ResponseEntity<List<CommandeDTO>> getAllCommandes(Authentication authentication) {
        logger.info("📦 Récupération des commandes du club de l'admin connecté");
        Utilisateur user = utilisateurService.findByEmail(authentication.getName()).orElseThrow();
        Long clubId = user.getClub().getId();
        List<CommandeDTO> commandes = commandeService.getAllCommandesByClubId(clubId);
        return ResponseEntity.ok(commandes);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommandeDTO> getCommandeById(@PathVariable Long id) {
        return commandeService.getCommandeById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> {
                    logger.warn("❌ Commande ID {} non trouvée", id);
                    return ResponseEntity.notFound().build();
                });
    }

    // =========================
    //      PARENT / MEMBRE
    // =========================

    @GetMapping("/membre/{membreId}")
    public ResponseEntity<List<CommandeDTO>> getCommandesParMembre(@PathVariable Long membreId) {
        logger.info("👤 Récupération commandes du membre {}", membreId);
        return ResponseEntity.ok(commandeService.getCommandesParMembre(membreId));
    }

    @GetMapping("/parent/{parentId}")
    public ResponseEntity<List<CommandeDTO>> getCommandesParParent(@PathVariable Long parentId) {
        logger.info("👨‍👩‍👧 Récupération commandes du parent {}", parentId);
        return ResponseEntity.ok(commandeService.getCommandesParParent(parentId));
    }

    // =========================
    //     CREATION / UPDATE
    // =========================

    @PostMapping
    public ResponseEntity<CommandeDTO> createCommande(@Valid @RequestBody CommandeDTO commandeDTO) {
        try {
            CommandeDTO saved = commandeService.createCommande(commandeDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (RuntimeException e) {
            logger.error("❌ Erreur création commande simple : {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/with-lignes")
    public ResponseEntity<CommandeDTO> createCommandeAvecLignes(@Valid @RequestBody CommandeDTO commandeDTO) {
        try {
            CommandeDTO saved = commandeService.createCommandeWithLignes(commandeDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (RuntimeException e) {
            logger.error("❌ Erreur création commande with-lignes : {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> mettreAJourCommande(@PathVariable Long id, @Valid @RequestBody CommandeUpdateDTO updateDTO) {
        try {
            commandeService.mettreAJourCommande(id, updateDTO);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            logger.error("❌ Erreur MAJ commande {} : {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCommande(@PathVariable Long id) {
        try {
            commandeService.deleteCommande(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            logger.error("❌ Erreur suppression commande {} : {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    // =========================
    //     ACTIONS ADMIN
    // =========================

    @GetMapping("/paiement-club")
    public ResponseEntity<List<CommandeDTO>> getCommandesPaiementClub() {
        logger.info("📥 Récupération commandes à payer au club");
        return ResponseEntity.ok(commandeService.getCommandesPaiementClub());
    }

    @PutMapping("/{id}/valider")
    public ResponseEntity<CommandeDTO> validerCommande(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload) {
        try {
            String modePaiement = (String) payload.get("modePaiement");
            if (modePaiement == null) {
                return ResponseEntity.badRequest().build();
            }
            return ResponseEntity.ok(commandeService.validerCommande(id, modePaiement));
        } catch (RuntimeException e) {
            logger.error("❌ Erreur validation commande {} : {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}/annuler")
    public ResponseEntity<CommandeDTO> annulerCommande(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        try {
            String motif = (String) payload.getOrDefault("motif", "Motif non renseigné");
            return ResponseEntity.ok(commandeService.annulerCommande(id, motif));
        } catch (RuntimeException e) {
            logger.error("❌ Erreur annulation commande {} : {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}/a-retirer")
    public ResponseEntity<CommandeDTO> marquerCommandeARetirer(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(commandeService.marquerCommandeARetirer(id));
        } catch (RuntimeException e) {
            logger.error("❌ Erreur marquer commande à retirer {} : {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}
