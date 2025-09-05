package club.taekwondo.service.jpa;

import club.taekwondo.dto.CommandeDTO;
import club.taekwondo.dto.CommandeUpdateDTO;
import club.taekwondo.dto.LigneCommandeDTO;
import club.taekwondo.dto.UtilisateurCommandeDTO;
import club.taekwondo.entity.jpa.Commande;
import club.taekwondo.entity.jpa.LigneCommande;
import club.taekwondo.entity.jpa.Membre;
import club.taekwondo.entity.jpa.Produit;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.repository.jpa.CommandeRepository;
import club.taekwondo.repository.jpa.LigneCommandeRepository;
import club.taekwondo.repository.jpa.MembreRepository;
import club.taekwondo.repository.jpa.ProduitRepository;
import club.taekwondo.repository.jpa.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CommandeService {

    @Autowired private CommandeRepository commandeRepository;
    @Autowired private UtilisateurRepository utilisateurRepository;
    @Autowired private ProduitRepository produitRepository;
    @Autowired private LigneCommandeRepository ligneCommandeRepository;
    @Autowired private MembreRepository membreRepository; // bénéficiaire (enfant) par ligne

    // =========================
    //         Public API
    // =========================

    // Récupérer toutes les commandes
    public List<CommandeDTO> getAllCommandes() {
        return commandeRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Récupérer une commande par son ID
    public Optional<CommandeDTO> getCommandeById(Long id) {
        return commandeRepository.findById(id).map(this::convertToDTO);
    }

    // Créer une commande simple (sans lignes)
    @Transactional
    public CommandeDTO createCommande(CommandeDTO commandeDTO) {
        Commande commande = new Commande();
        commande.setDateCommande(LocalDate.now());
        commande.setMontantTotal(BigDecimal.ZERO);

        final String modeNorm = normalizeMode(commandeDTO.getModePaiement());
        commande.setModePaiement(modeNorm);

        if ("CB".equals(modeNorm)) {
            commande.setStatut("PAYEE");
            commande.setDatePaiement(LocalDate.now());
        } else if ("CLUB".equals(modeNorm) || "ESPECES".equals(modeNorm) || "VIREMENT".equals(modeNorm)) {
            commande.setStatut("EN_ATTENTE");
            commande.setDatePaiement(null);
        } else {
            commande.setStatut("EN_COURS");
            commande.setDatePaiement(null);
        }

        // Associer l'utilisateur si présent
        if (commandeDTO.getUtilisateurId() != null) {
            Utilisateur utilisateur = utilisateurRepository.findById(commandeDTO.getUtilisateurId())
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID : " + commandeDTO.getUtilisateurId()));
            commande.setUtilisateur(utilisateur);
        }

        commande = commandeRepository.save(commande);
        return convertToDTO(commande);
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
            // CLUB / ESPECES / VIREMENT / autres -> en attente
            commande.setStatut("EN_ATTENTE");
            commande.setDatePaiement(null);
        }

        // Associer l'utilisateur
        if (commandeDTO.getUtilisateurId() != null) {
            Utilisateur utilisateur = utilisateurRepository.findById(commandeDTO.getUtilisateurId())
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID : " + commandeDTO.getUtilisateurId()));
            commande.setUtilisateur(utilisateur);
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
                ligne.setPrixUnitaire(ligneDTO.getPrixUnitaire());

                // Sous-total en BigDecimal pour éviter les erreurs d'arrondi
                BigDecimal pu = BigDecimal.valueOf(ligneDTO.getPrixUnitaire() != null ? ligneDTO.getPrixUnitaire() : 0.0);
                BigDecimal qte = BigDecimal.valueOf(ligneDTO.getQuantite() != null ? ligneDTO.getQuantite() : 0);
                BigDecimal ligneTotal = pu.multiply(qte);
                ligne.setSousTotal(ligneTotal.doubleValue());

                ligne.setTaille(ligneDTO.getTaille());
                ligne.setCouleur(ligneDTO.getCouleur());
                ligne.setFlocage(ligneDTO.getFlocage());

                // Bénéficiaire (enfant) optionnel par ligne
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

    // Valider manuellement un paiement (admin)
    @Transactional
    public void validerPaiementManuel(Long id, String statut, String modePaiement, String datePaiement) {
        Commande commande = commandeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Commande non trouvée avec l'ID : " + id));

        final String statutNorm = nullSafeUpper(statut);
        final String modeNorm = normalizeMode(modePaiement);

        commande.setStatut(statutNorm);
        commande.setModePaiement(modeNorm);

        if (datePaiement != null && !datePaiement.isBlank()) {
            try {
                commande.setDatePaiement(LocalDate.parse(datePaiement)); // "YYYY-MM-DD"
            } catch (DateTimeParseException ex) {
                throw new IllegalArgumentException("Format de datePaiement invalide (attendu YYYY-MM-DD) : " + datePaiement);
            }
        } else {
            // Si on valide en PAYEE et mode CB sans date fournie, on pose la date du jour
            if ("PAYEE".equals(statutNorm) && "CB".equals(modeNorm)) {
                commande.setDatePaiement(LocalDate.now());
            } else {
                commande.setDatePaiement(null);
            }
        }

        commandeRepository.save(commande);
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

    // =========================
    //       Conversions
    // =========================

    // Conversion Commande -> DTO (avec lignes + utilisateur)
    private CommandeDTO convertToDTO(Commande commande) {
        CommandeDTO dto = new CommandeDTO();
        dto.setId(commande.getId());
        dto.setDateCommande(commande.getDateCommande());
        dto.setMontantTotal(commande.getMontantTotal());
        dto.setModePaiement(commande.getModePaiement());
        dto.setDatePaiement(commande.getDatePaiement());
        dto.setStatut(commande.getStatut());

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

    // Conversion LigneCommande -> DTO (incluant le bénéficiaire enfant)
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

        // mapping bénéficiaire
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

    /**
     * Normalise le mode de paiement pour le stocker en base :
     * - "stripe", "cb", "carte", "carte bancaire" -> "CB"
     * - "club" -> "CLUB"
     * - "especes"/"espèces" -> "ESPECES"
     * - "virement" -> "VIREMENT"
     * - sinon renvoie la version UPPERCASE trim.
     */
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

    // NOTE: plus de champ "disponible_au_club" -> méthode retirée (ou no-op si appelée ailleurs)
}
