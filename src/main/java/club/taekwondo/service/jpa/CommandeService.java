package club.taekwondo.service.jpa;

import club.taekwondo.dto.BonCommandeRequestDTO;
import club.taekwondo.dto.CommandeDTO;
import club.taekwondo.dto.CommandeUpdateDTO;
import club.taekwondo.dto.LigneCommandeDTO;
import club.taekwondo.dto.UtilisateurCommandeDTO;
import club.taekwondo.entity.jpa.CampagneCommande;
import club.taekwondo.entity.jpa.Commande;
import club.taekwondo.entity.jpa.LigneCommande;
import club.taekwondo.entity.jpa.Membre;
import club.taekwondo.entity.jpa.Produit;
import club.taekwondo.entity.jpa.Club;
import club.taekwondo.repository.jpa.ClubRepository;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.repository.jpa.CommandeRepository;
import club.taekwondo.repository.jpa.LigneCommandeRepository;
import club.taekwondo.repository.jpa.MembreRepository;
import club.taekwondo.repository.jpa.ProduitRepository;
import club.taekwondo.repository.jpa.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CommandeService {

    private static final Logger logger = LoggerFactory.getLogger(CommandeService.class);

    @Autowired private CommandeRepository commandeRepository;
    @Autowired private UtilisateurRepository utilisateurRepository;
    @Autowired private ProduitRepository produitRepository;
    @Autowired private LigneCommandeRepository ligneCommandeRepository;
    @Autowired private MembreRepository membreRepository; // bénéficiaire (enfant) par ligne
    @Autowired private ClubRepository clubRepository;

    // =========================
    //         CONFIGURATION
    // =========================
    
    private static final BigDecimal COUT_FLOCAGE = BigDecimal.valueOf(10.0); // 10€ par défaut
    
    // =========================
    //         Public API
    // =========================

    // Récupérer toutes les commandes (non filtré par club)
    public List<CommandeDTO> getAllCommandes() {
        return commandeRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Récupérer toutes les commandes
    public List<CommandeDTO> getAllCommandesByClubId(Long clubId) {
        return commandeRepository.findByClub_Id(clubId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Récupérer une commande par son ID
    public Optional<CommandeDTO> getCommandeById(Long id) {
        return commandeRepository.findById(id).map(this::convertToDTO);
    }

    public Optional<Commande> getCommandeEntityById(Long id) {
        return commandeRepository.findById(id);
    }

    public CommandeDTO toCommandeDTO(Commande commande) {
        return convertToDTO(commande);
    }

    // Récupérer les commandes d’un membre
    public List<CommandeDTO> getCommandesParMembre(Long membreId) {
        LinkedHashMap<Long, CommandeDTO> merged = new LinkedHashMap<>();

        commandeRepository.findByMembreId(membreId).stream()
                .map(this::convertToDTO)
                .forEach(dto -> merged.put(dto.getId(), dto));

        membreRepository.findById(membreId)
                .map(Membre::getCompteUtilisateur)
                .map(Utilisateur::getId)
                .ifPresent(utilisateurId ->
                        commandeRepository.findByUtilisateurId(utilisateurId).stream()
                                .map(this::convertToDTO)
                                .forEach(dto -> merged.putIfAbsent(dto.getId(), dto))
                );

        return new ArrayList<>(merged.values());
    }

    // Récupérer les commandes d’un parent (lui + ses enfants)
    public List<CommandeDTO> getCommandesParParent(Long parentId) {
        LinkedHashMap<Long, CommandeDTO> merged = new LinkedHashMap<>();

        commandeRepository.findByUtilisateurId(parentId).stream()
                .map(this::convertToDTO)
                .forEach(dto -> merged.put(dto.getId(), dto));

        membreRepository.findByParentId(parentId).stream()
                .map(Membre::getId)
                .forEach(membreId ->
                        commandeRepository.findByMembreId(membreId).stream()
                                .map(this::convertToDTO)
                                .forEach(dto -> merged.putIfAbsent(dto.getId(), dto))
                );

        return new ArrayList<>(merged.values());
    }

    // Créer une commande simple (maintenant avec gestion des lignes)
    @Transactional
    public CommandeDTO createCommande(CommandeDTO commandeDTO) {
        // Déléguer à la méthode complète qui gère les lignes correctement
        return createCommandeWithLignes(commandeDTO);
    }

    // Créer une commande avec lignes (gère aussi le bénéficiaire enfant par ligne)
    @Transactional
    public CommandeDTO createCommandeWithLignes(CommandeDTO commandeDTO) {
        Commande commande = new Commande();
        commande.setDateCommande(LocalDate.now());
        commande.setMontantTotal(BigDecimal.ZERO);

        final String modeNorm = normalizeMode(commandeDTO.getModePaiement());
        commande.setModePaiement(modeNorm);

        if ("CB".equals(modeNorm)) {
            commande.setStatut("PAYEE");
            commande.setDatePaiement(LocalDate.now());
        } else {
            commande.setStatut("EN_ATTENTE");
            commande.setDatePaiement(null);
        }

        // Associer l'utilisateur
        if (commandeDTO.getUtilisateurId() != null) {
            Utilisateur utilisateur = utilisateurRepository.findById(commandeDTO.getUtilisateurId())
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID : " + commandeDTO.getUtilisateurId()));
            commande.setUtilisateur(utilisateur);
        }
        // Associer le club
        if (commandeDTO.getClubId() != null) {
            Club club = clubRepository.findById(commandeDTO.getClubId()).orElse(null);
            commande.setClub(club);
        }

        commande = commandeRepository.save(commande);

        BigDecimal total = BigDecimal.ZERO;
        List<LigneCommande> lignes = new ArrayList<>();

        if (commandeDTO.getLignesCommande() != null) {
            for (LigneCommandeDTO ligneDTO : commandeDTO.getLignesCommande()) {
                Produit produit = produitRepository.findById(ligneDTO.getProduitId())
                        .orElseThrow(() -> new RuntimeException("Produit non trouvé avec l'ID : " + ligneDTO.getProduitId()));

                LigneCommande ligne = new LigneCommande();
                ligne.setCommande(commande);
                ligne.setProduit(produit);
                ligne.setQuantite(ligneDTO.getQuantite());

                // 🔹 Utiliser le prix unitaire fourni (déjà calculé avec flocage si nécessaire)
                // Ne pas recalculer le flocage ici pour éviter le double comptage
                BigDecimal prixUnitaireCalcule;
                
                if (ligneDTO.getPrixUnitaire() != null && ligneDTO.getPrixUnitaire() > 0) {
                    // Prix déjà calculé (probablement par PaiementService)
                    prixUnitaireCalcule = BigDecimal.valueOf(ligneDTO.getPrixUnitaire());
                    logger.info("💰 Produit '{}' - Prix unitaire fourni: {}€", produit.getNom(), prixUnitaireCalcule);
                } else {
                    // Fallback: calcul basique (prix produit + flocage si nécessaire)
                    BigDecimal prixBase = produit.getPrix();
                    prixUnitaireCalcule = prixBase;
                    logger.info("💰 Produit '{}' - Prix base: {}€", produit.getNom(), prixBase);
                    
                    if (ligneDTO.getFlocage() != null && !ligneDTO.getFlocage().trim().isEmpty()) {
                        prixUnitaireCalcule = prixUnitaireCalcule.add(COUT_FLOCAGE);
                        logger.info("🏷️ Flocage '{}' ajouté - Coût: {}€ - Prix final: {}€", 
                            ligneDTO.getFlocage(), COUT_FLOCAGE, prixUnitaireCalcule);
                    }
                }
                
                ligne.setPrixUnitaire(prixUnitaireCalcule.doubleValue());

                BigDecimal qte = BigDecimal.valueOf(ligneDTO.getQuantite() != null ? ligneDTO.getQuantite() : 0);
                BigDecimal ligneTotal = prixUnitaireCalcule.multiply(qte);
                ligne.setSousTotal(ligneTotal.doubleValue());
                
                logger.info("📊 Ligne: {}x {} = {}€", qte, prixUnitaireCalcule, ligneTotal);

                ligne.setTaille(ligneDTO.getTaille());
                ligne.setCouleur(ligneDTO.getCouleur());
                ligne.setFlocage(ligneDTO.getFlocage());

                if (ligneDTO.getBeneficiaireId() != null) {
                    Membre enfant = membreRepository.findById(ligneDTO.getBeneficiaireId())
                            .orElseThrow(() -> new RuntimeException("Bénéficiaire (membre) introuvable: " + ligneDTO.getBeneficiaireId()));
                    ligne.setBeneficiaire(enfant);
                }

                total = total.add(ligneTotal);
                lignes.add(ligneCommandeRepository.save(ligne));
            }
        }

        commande.setMontantTotal(total);
        logger.info("💳 MONTANT TOTAL COMMANDE: {}€", total);
        commande = commandeRepository.save(commande);

        CommandeDTO resultDTO = convertToDTO(commande);
        resultDTO.setLignesCommande(lignes.stream().map(this::convertLigneToDTO).collect(Collectors.toList()));
        return resultDTO;
    }

    @Transactional
    public CommandeDTO createCommandeForCampagne(BonCommandeRequestDTO req, Utilisateur utilisateur, CampagneCommande campagne) {
        if (req.getLignesCommande() == null || req.getLignesCommande().isEmpty()) {
            throw new IllegalArgumentException("La commande doit contenir au moins une ligne.");
        }

        Commande commande = new Commande();
        commande.setDateCommande(LocalDate.now());
        commande.setMontantTotal(BigDecimal.ZERO);
        commande.setUtilisateur(utilisateur);
        commande.setCampagne(campagne);
        if (utilisateur.getClub() != null) {
            commande.setClub(utilisateur.getClub());
        }

        final String modeNorm = normalizeMode(req.getModePaiement());
        commande.setModePaiement(modeNorm);
        commande.setStatut("EN_ATTENTE");
        commande.setDatePaiement(null);

        commande = commandeRepository.save(commande);

        BigDecimal total = BigDecimal.ZERO;
        List<LigneCommande> lignes = new ArrayList<>();

        for (LigneCommandeDTO ligneDTO : req.getLignesCommande()) {
            Produit produit = produitRepository.findById(ligneDTO.getProduitId())
                    .orElseThrow(() -> new RuntimeException("Produit non trouvé id=" + ligneDTO.getProduitId()));

            BigDecimal prixUnitaire;
            if (ligneDTO.getPrixUnitaire() != null && ligneDTO.getPrixUnitaire() > 0) {
                prixUnitaire = BigDecimal.valueOf(ligneDTO.getPrixUnitaire());
            } else {
                prixUnitaire = produit.getPrix();
                if (ligneDTO.getFlocage() != null && !ligneDTO.getFlocage().trim().isEmpty()) {
                    prixUnitaire = prixUnitaire.add(COUT_FLOCAGE);
                }
            }

            LigneCommande ligne = new LigneCommande();
            ligne.setCommande(commande);
            ligne.setProduit(produit);
            ligne.setQuantite(ligneDTO.getQuantite());
            ligne.setPrixUnitaire(prixUnitaire.doubleValue());
            BigDecimal qte = BigDecimal.valueOf(ligneDTO.getQuantite() != null ? ligneDTO.getQuantite() : 0);
            ligne.setSousTotal(prixUnitaire.multiply(qte).doubleValue());
            ligne.setTaille(ligneDTO.getTaille());
            ligne.setCouleur(ligneDTO.getCouleur());
            ligne.setFlocage(ligneDTO.getFlocage());

            if (ligneDTO.getBeneficiaireId() != null) {
                Membre enfant = membreRepository.findById(ligneDTO.getBeneficiaireId())
                        .orElseThrow(() -> new RuntimeException("Bénéficiaire introuvable id=" + ligneDTO.getBeneficiaireId()));
                ligne.setBeneficiaire(enfant);
            }

            total = total.add(prixUnitaire.multiply(qte));
            lignes.add(ligneCommandeRepository.save(ligne));
        }

        commande.setMontantTotal(total);
        commande = commandeRepository.save(commande);

        CommandeDTO resultDTO = convertToDTO(commande);
        resultDTO.setLignesCommande(lignes.stream().map(this::convertLigneToDTO).collect(Collectors.toList()));
        return resultDTO;
    }

    // Supprimer une commande
    @Transactional
    public void deleteCommande(Long id) {
        if (commandeRepository.existsById(id)) {
            commandeRepository.deleteById(id);
        } else {
            throw new IllegalArgumentException("Commande avec ID " + id + " non trouvée.");
        }
    }

    // Mise à jour partielle d’une commande (statut/mode/date)
    @Transactional
    public void mettreAJourCommande(Long id, CommandeUpdateDTO updateDTO) {
        Commande commande = commandeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Commande non trouvée avec l'ID : " + id));

        if (updateDTO.getStatut() != null) {
            commande.setStatut(nullSafeUpper(updateDTO.getStatut()));
        }
        if (updateDTO.getModePaiement() != null) {
            commande.setModePaiement(normalizeMode(updateDTO.getModePaiement()));
        }
        if (updateDTO.getDatePaiement() != null) {
            commande.setDatePaiement(updateDTO.getDatePaiement());
        }

        commandeRepository.save(commande);
    }

    // Récupérer les commandes à payer au club
    public List<CommandeDTO> getCommandesPaiementClub() {
        return commandeRepository.findAll().stream()
                .filter(c -> "EN_ATTENTE".equalsIgnoreCase(nullSafeUpper(c.getStatut()))
                        && c.getModePaiement() != null
                        && (
                            "CLUB".equals(normalizeMode(c.getModePaiement())) ||
                            "ESPECES".equals(normalizeMode(c.getModePaiement())) ||
                            "VIREMENT".equals(normalizeMode(c.getModePaiement()))
                        )
                )
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<CommandeDTO> getCommandesPaiementClubByClubId(Long clubId) {
        return commandeRepository.findByClub_Id(clubId).stream()
                .filter(c -> "EN_ATTENTE".equalsIgnoreCase(nullSafeUpper(c.getStatut()))
                        && c.getModePaiement() != null
                        && (
                            "CLUB".equals(normalizeMode(c.getModePaiement())) ||
                            "ESPECES".equals(normalizeMode(c.getModePaiement())) ||
                            "VIREMENT".equals(normalizeMode(c.getModePaiement()))
                        )
                )
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // =========================
    //    Actions sur commande
    // =========================

    @Transactional
    public CommandeDTO validerCommande(Long id, String modePaiement) {
        Commande commande = commandeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Commande non trouvée avec ID " + id));

        commande.setStatut("PAYEE");
        commande.setModePaiement(normalizeMode(modePaiement));
        commande.setDatePaiement(LocalDate.now());

        commande = commandeRepository.save(commande);
        return convertToDTO(commande);
    }

    @Transactional
    public CommandeDTO annulerCommande(Long id, String motif) {
        Commande commande = commandeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Commande non trouvée avec ID " + id));

        commande.setStatut("ANNULEE");
        commande.setDatePaiement(null);

        commande = commandeRepository.save(commande);
        return convertToDTO(commande);
    }

    @Transactional
    public CommandeDTO marquerCommandeARetirer(Long id) {
        Commande commande = commandeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Commande non trouvée avec ID " + id));

        commande.setStatut("A_RETIRER");

        commande = commandeRepository.save(commande);
        return convertToDTO(commande);
    }

    // =========================
    //       Conversions
    // =========================

    private CommandeDTO convertToDTO(Commande commande) {
        CommandeDTO dto = new CommandeDTO();
        dto.setId(commande.getId());
        dto.setDateCommande(commande.getDateCommande());
        dto.setMontantTotal(commande.getMontantTotal());
        dto.setModePaiement(commande.getModePaiement());
        dto.setDatePaiement(commande.getDatePaiement());
        dto.setStatut(commande.getStatut());

        if (commande.getClub() != null) {
            dto.setClubId(commande.getClub().getId());
        }

        if (commande.getCampagne() != null) {
            dto.setCampagneId(commande.getCampagne().getId());
            dto.setCampagneTitre(commande.getCampagne().getTitre());
        }

        if (commande.getUtilisateur() != null) {
            dto.setUtilisateurId(commande.getUtilisateur().getId());
            Utilisateur utilisateur = commande.getUtilisateur();
            UtilisateurCommandeDTO utilisateurDTO = new UtilisateurCommandeDTO();
            utilisateurDTO.setId(utilisateur.getId());
            utilisateurDTO.setNom(utilisateur.getNom());
            utilisateurDTO.setPrenom(utilisateur.getPrenom());
            utilisateurDTO.setEmail(utilisateur.getEmail());
            dto.setUtilisateur(utilisateurDTO);
        }

        List<LigneCommandeDTO> lignes = ligneCommandeRepository.findByCommandeId(commande.getId())
                .stream()
                .map(this::convertLigneToDTO)
                .collect(Collectors.toList());
        dto.setLignesCommande(lignes);

        return dto;
    }

    private LigneCommandeDTO convertLigneToDTO(LigneCommande ligne) {
        LigneCommandeDTO dto = new LigneCommandeDTO();
        dto.setId(ligne.getId());
        dto.setCommandeId(ligne.getCommande() != null ? ligne.getCommande().getId() : null);

        if (ligne.getProduit() != null) {
            dto.setProduitId(ligne.getProduit().getId());
            dto.setProduitNom(ligne.getProduit().getNom());
        } else {
            dto.setProduitId(null);
            dto.setProduitNom("Produit inconnu");
        }

        dto.setQuantite(ligne.getQuantite());
        dto.setPrixUnitaire(ligne.getPrixUnitaire());
        dto.setSousTotal(ligne.getSousTotal());
        dto.setTaille(ligne.getTaille());
        dto.setCouleur(ligne.getCouleur());
        dto.setFlocage(ligne.getFlocage());

        Membre ben = ligne.getBeneficiaire();
        if (ben != null) {
            dto.setBeneficiaireId(ben.getId());
            dto.setBeneficiairePrenom(ben.getPrenom());
            dto.setBeneficiaireNom(ben.getNom());
        }

        return dto;
    }

    // =========================
    //        Utilitaires
    // =========================

    private static String nullSafeUpper(String s) {
        return s == null ? null : s.toUpperCase();
    }

    private static String normalizeMode(String raw) {
        if (raw == null) return null;
        String m = raw.trim().toLowerCase();
        switch (m) {
            case "stripe":
            case "cb":
            case "carte":
            case "carte bancaire":
            case "carte_bancaire":
                return "CB";
            case "club":
                return "CLUB";
            case "especes":
            case "espèces":
                return "ESPECES";
            case "virement":
                return "VIREMENT";
            default:
                return raw.trim().toUpperCase();
        }
    }
}
