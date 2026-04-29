package club.taekwondo.controller.jpa;

import club.taekwondo.dto.BonCommandeRequestDTO;
import club.taekwondo.dto.CartCheckoutRequestDTO;
import club.taekwondo.dto.CommandeDTO;
import club.taekwondo.dto.CommandeUpdateDTO;
import club.taekwondo.entity.jpa.CampagneCommande;
import club.taekwondo.entity.jpa.Commande;
import club.taekwondo.entity.jpa.Membre;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.repository.jpa.MembreRepository;
import club.taekwondo.service.jpa.CampagneCommandeService;
import club.taekwondo.service.jpa.CommandeService;
import club.taekwondo.service.jpa.UtilisateurService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/api/commandes")
public class CommandeController {

    private static final Logger logger = LoggerFactory.getLogger(CommandeController.class);

    private final CommandeService commandeService;
    private final UtilisateurService utilisateurService;
    private final MembreRepository membreRepository;
    private final CampagneCommandeService campagneService;

    public CommandeController(CommandeService commandeService,
                              UtilisateurService utilisateurService,
                              MembreRepository membreRepository,
                              CampagneCommandeService campagneService) {
        this.commandeService = commandeService;
        this.utilisateurService = utilisateurService;
        this.membreRepository = membreRepository;
        this.campagneService = campagneService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<CommandeDTO>> getAllCommandes(Authentication authentication) {
        Utilisateur caller = requireCaller(authentication);

        if (hasRole(authentication, "SUPER_ADMIN")) {
            return ResponseEntity.ok(commandeService.getAllCommandes());
        }

        Long clubId = getClubId(caller);
        if (clubId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(commandeService.getAllCommandesByClubId(clubId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MEMBRE','PARENT','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<CommandeDTO> getCommandeById(@PathVariable Long id, Authentication authentication) {
        Optional<Commande> maybeCommande = commandeService.getCommandeEntityById(id);
        if (maybeCommande.isEmpty()) {
            logger.warn("Commande ID {} non trouvee", id);
            return ResponseEntity.notFound().build();
        }

        Utilisateur caller = requireCaller(authentication);
        if (!canAccessCommande(authentication, caller, maybeCommande.get())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(commandeService.toCommandeDTO(maybeCommande.get()));
    }

    @GetMapping("/membre/{membreId}")
    @PreAuthorize("hasAnyRole('MEMBRE','PARENT','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<CommandeDTO>> getCommandesParMembre(@PathVariable Long membreId,
                                                                   Authentication authentication) {
        Optional<Membre> maybeMembre = membreRepository.findById(membreId);
        if (maybeMembre.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Utilisateur caller = requireCaller(authentication);
        if (!canAccessMemberScope(authentication, caller, maybeMembre.get())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(commandeService.getCommandesParMembre(membreId));
    }

    @GetMapping("/parent/{parentId}")
    @PreAuthorize("hasAnyRole('PARENT','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<CommandeDTO>> getCommandesParParent(@PathVariable Long parentId,
                                                                   Authentication authentication) {
        Optional<Utilisateur> maybeParent = utilisateurService.getUtilisateurEntityById(parentId);
        if (maybeParent.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Utilisateur caller = requireCaller(authentication);
        if (!canAccessParentScope(authentication, caller, maybeParent.get())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(commandeService.getCommandesParParent(parentId));
    }

    @PostMapping("/bon-de-commande")
    @PreAuthorize("hasAnyRole('MEMBRE','PARENT')")
    public ResponseEntity<?> soumettreEvenementCommande(@RequestBody BonCommandeRequestDTO req,
                                                        Authentication authentication) {
        Utilisateur caller = requireCaller(authentication);
        Long clubId = getClubId(caller);
        if (clubId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Aucun club associe au compte"));
        }

        CampagneCommande campagne;
        if (req.getCampagneId() != null) {
            campagne = campagneService.findById(req.getCampagneId())
                    .orElse(null);
            if (campagne == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Campagne introuvable"));
            }
            if (!campagne.isActif()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Cette campagne est fermee"));
            }
        } else {
            campagne = campagneService.getActiveEntity(clubId).orElse(null);
            if (campagne == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Aucune campagne de commande active pour votre club"));
            }
        }

        try {
            CommandeDTO result = commandeService.createCommandeForCampagne(req, caller, campagne);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (Exception e) {
            logger.error("Erreur creation bon de commande : {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/from-cart")
    @PreAuthorize("hasAnyRole('MEMBRE','PARENT')")
    public ResponseEntity<?> passerCommandeDepuisPanier(@RequestBody CartCheckoutRequestDTO req,
                                                         Authentication authentication) {
        Utilisateur caller = requireCaller(authentication);
        Long clubId = getClubId(caller);
        if (clubId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Aucun club associé au compte"));
        }

        try {
            CommandeDTO result = commandeService.createCommandeFromCart(req, caller);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Erreur création commande depuis panier : {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<CommandeDTO> createCommande(@Valid @RequestBody CommandeDTO commandeDTO,
                                                      Authentication authentication) {
        Optional<Utilisateur> maybeTargetUser = utilisateurService.getUtilisateurEntityById(commandeDTO.getUtilisateurId());
        if (maybeTargetUser.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        Utilisateur caller = requireCaller(authentication);
        if (!canManageCommandeCreation(authentication, caller, maybeTargetUser.get(), commandeDTO.getClubId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Long effectiveClubId = commandeDTO.getClubId() != null ? commandeDTO.getClubId() : getClubId(maybeTargetUser.get());
        if (effectiveClubId != null) {
            commandeDTO.setClubId(effectiveClubId);
        }

        try {
            CommandeDTO saved = commandeService.createCommande(commandeDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (RuntimeException e) {
            logger.error("Erreur creation commande simple : {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/with-lignes")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<CommandeDTO> createCommandeAvecLignes(@Valid @RequestBody CommandeDTO commandeDTO,
                                                                Authentication authentication) {
        Optional<Utilisateur> maybeTargetUser = utilisateurService.getUtilisateurEntityById(commandeDTO.getUtilisateurId());
        if (maybeTargetUser.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        Utilisateur caller = requireCaller(authentication);
        if (!canManageCommandeCreation(authentication, caller, maybeTargetUser.get(), commandeDTO.getClubId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Long effectiveClubId = commandeDTO.getClubId() != null ? commandeDTO.getClubId() : getClubId(maybeTargetUser.get());
        if (effectiveClubId != null) {
            commandeDTO.setClubId(effectiveClubId);
        }

        try {
            CommandeDTO saved = commandeService.createCommandeWithLignes(commandeDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (RuntimeException e) {
            logger.error("Erreur creation commande with-lignes : {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Void> mettreAJourCommande(@PathVariable Long id,
                                                    @Valid @RequestBody CommandeUpdateDTO updateDTO,
                                                    Authentication authentication) {
        Optional<Commande> maybeCommande = commandeService.getCommandeEntityById(id);
        if (maybeCommande.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Utilisateur caller = requireCaller(authentication);
        if (!canManageCommande(authentication, caller, maybeCommande.get())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            commandeService.mettreAJourCommande(id, updateDTO);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            logger.error("Erreur MAJ commande {} : {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Void> deleteCommande(@PathVariable Long id, Authentication authentication) {
        Optional<Commande> maybeCommande = commandeService.getCommandeEntityById(id);
        if (maybeCommande.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Utilisateur caller = requireCaller(authentication);
        if (!canManageCommande(authentication, caller, maybeCommande.get())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            commandeService.deleteCommande(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            logger.error("Erreur suppression commande {} : {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/paiement-club")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<CommandeDTO>> getCommandesPaiementClub(Authentication authentication) {
        Utilisateur caller = requireCaller(authentication);

        if (hasRole(authentication, "SUPER_ADMIN")) {
            return ResponseEntity.ok(commandeService.getCommandesPaiementClub());
        }

        Long clubId = getClubId(caller);
        if (clubId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(commandeService.getCommandesPaiementClubByClubId(clubId));
    }

    @PutMapping("/{id}/valider")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<CommandeDTO> validerCommande(@PathVariable Long id,
                                                       @RequestBody Map<String, Object> payload,
                                                       Authentication authentication) {
        Optional<Commande> maybeCommande = commandeService.getCommandeEntityById(id);
        if (maybeCommande.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Utilisateur caller = requireCaller(authentication);
        if (!canManageCommande(authentication, caller, maybeCommande.get())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        String modePaiement = (String) payload.get("modePaiement");
        if (modePaiement == null || modePaiement.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            return ResponseEntity.ok(commandeService.validerCommande(id, modePaiement));
        } catch (RuntimeException e) {
            logger.error("Erreur validation commande {} : {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}/annuler")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<CommandeDTO> annulerCommande(@PathVariable Long id,
                                                       @RequestBody Map<String, Object> payload,
                                                       Authentication authentication) {
        Optional<Commande> maybeCommande = commandeService.getCommandeEntityById(id);
        if (maybeCommande.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Utilisateur caller = requireCaller(authentication);
        if (!canManageCommande(authentication, caller, maybeCommande.get())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            String motif = (String) payload.getOrDefault("motif", "Motif non renseigne");
            return ResponseEntity.ok(commandeService.annulerCommande(id, motif));
        } catch (RuntimeException e) {
            logger.error("Erreur annulation commande {} : {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}/a-retirer")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<CommandeDTO> marquerCommandeARetirer(@PathVariable Long id,
                                                               Authentication authentication) {
        Optional<Commande> maybeCommande = commandeService.getCommandeEntityById(id);
        if (maybeCommande.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Utilisateur caller = requireCaller(authentication);
        if (!canManageCommande(authentication, caller, maybeCommande.get())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            return ResponseEntity.ok(commandeService.marquerCommandeARetirer(id));
        } catch (RuntimeException e) {
            logger.error("Erreur marquer commande a retirer {} : {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    private Utilisateur requireCaller(Authentication authentication) {
        return utilisateurService.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Utilisateur authentifie introuvable"));
    }

    private boolean hasRole(Authentication authentication, String role) {
        String expected = "ROLE_" + role;
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> expected.equals(authority.getAuthority()) || role.equals(authority.getAuthority()));
    }

    private Long getClubId(Utilisateur utilisateur) {
        return utilisateur != null && utilisateur.getClub() != null ? utilisateur.getClub().getId() : null;
    }

    private Long getCommandeClubId(Commande commande) {
        if (commande.getClub() != null) {
            return commande.getClub().getId();
        }
        if (commande.getUtilisateur() != null) {
            return getClubId(commande.getUtilisateur());
        }
        return null;
    }

    private boolean sameClub(Long left, Long right) {
        return left != null && left.equals(right);
    }

    private boolean canManageCommande(Authentication authentication, Utilisateur caller, Commande commande) {
        if (hasRole(authentication, "SUPER_ADMIN")) {
            return true;
        }
        return hasRole(authentication, "ADMIN") && sameClub(getClubId(caller), getCommandeClubId(commande));
    }

    private boolean canManageCommandeCreation(Authentication authentication,
                                              Utilisateur caller,
                                              Utilisateur targetUser,
                                              Long requestedClubId) {
        if (hasRole(authentication, "SUPER_ADMIN")) {
            return true;
        }

        Long callerClubId = getClubId(caller);
        Long targetClubId = getClubId(targetUser);
        Long effectiveClubId = requestedClubId != null ? requestedClubId : targetClubId;

        return sameClub(callerClubId, targetClubId)
                && (effectiveClubId == null || sameClub(callerClubId, effectiveClubId));
    }

    private boolean canAccessMemberScope(Authentication authentication, Utilisateur caller, Membre membre) {
        if (hasRole(authentication, "SUPER_ADMIN")) {
            return true;
        }
        if (hasRole(authentication, "ADMIN")) {
            return sameClub(getClubId(caller), membre.getClub() != null ? membre.getClub().getId() : null);
        }
        if (hasRole(authentication, "PARENT")) {
            return membre.getParent() != null && Objects.equals(membre.getParent().getId(), caller.getId());
        }
        return membre.getCompteUtilisateur() != null
                && Objects.equals(membre.getCompteUtilisateur().getId(), caller.getId());
    }

    private boolean canAccessParentScope(Authentication authentication, Utilisateur caller, Utilisateur parent) {
        if (hasRole(authentication, "SUPER_ADMIN")) {
            return true;
        }
        if (hasRole(authentication, "ADMIN")) {
            return sameClub(getClubId(caller), getClubId(parent));
        }
        return hasRole(authentication, "PARENT") && Objects.equals(caller.getId(), parent.getId());
    }

    private boolean canAccessCommande(Authentication authentication, Utilisateur caller, Commande commande) {
        if (canManageCommande(authentication, caller, commande)) {
            return true;
        }

        if (commande.getUtilisateur() != null && Objects.equals(commande.getUtilisateur().getId(), caller.getId())) {
            return true;
        }

        if (commande.getLignes() == null) {
            return false;
        }

        if (hasRole(authentication, "PARENT")) {
            return commande.getLignes().stream()
                    .map(ligne -> ligne.getBeneficiaire())
                    .filter(Objects::nonNull)
                    .anyMatch(membre -> membre.getParent() != null && Objects.equals(membre.getParent().getId(), caller.getId()));
        }

        if (hasRole(authentication, "MEMBRE")) {
            return commande.getLignes().stream()
                    .map(ligne -> ligne.getBeneficiaire())
                    .filter(Objects::nonNull)
                    .anyMatch(membre -> membre.getCompteUtilisateur() != null
                            && Objects.equals(membre.getCompteUtilisateur().getId(), caller.getId()));
        }

        return false;
    }
}
