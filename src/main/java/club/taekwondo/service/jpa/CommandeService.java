package club.taekwondo.service.jpa;

import club.taekwondo.dto.CommandeDTO;
import club.taekwondo.dto.CommandeUpdateDTO;
import club.taekwondo.dto.LigneCommandeDTO;
import club.taekwondo.dto.UtilisateurCommandeDTO;
import club.taekwondo.entity.jpa.Commande;
import club.taekwondo.entity.jpa.LigneCommande;
import club.taekwondo.entity.jpa.Produit;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.repository.jpa.CommandeRepository;
import club.taekwondo.repository.jpa.LigneCommandeRepository;
import club.taekwondo.repository.jpa.ProduitRepository;
import club.taekwondo.repository.jpa.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    // 🔹 Récupérer toutes les commandes
    public List<CommandeDTO> getAllCommandes() {
        return commandeRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // 🔹 Récupérer une commande par son ID
    public Optional<CommandeDTO> getCommandeById(Long id) {
        return commandeRepository.findById(id).map(this::convertToDTO);
    }

    // 🔹 Créer une commande simple (sans lignes)
    public CommandeDTO createCommande(CommandeDTO commandeDTO) {
        Commande commande = new Commande();
        commande.setDateCommande(LocalDate.now());
        commande.setMontantTotal(BigDecimal.ZERO);

        // Initialisation du statut en fonction du mode de paiement
        if ("CB".equalsIgnoreCase(commandeDTO.getModePaiement())) {
            commande.setStatut("PAYEE");
        } else if ("especes".equalsIgnoreCase(commandeDTO.getModePaiement()) || "virement".equalsIgnoreCase(commandeDTO.getModePaiement())) {
            commande.setStatut("EN_ATTENTE");
        } else {
            commande.setStatut("EN_COURS");
        }

        commande.setModePaiement(commandeDTO.getModePaiement());

        // Associer l'utilisateur si présent
        if (commandeDTO.getUtilisateurId() != null) {
            Utilisateur utilisateur = utilisateurRepository.findById(commandeDTO.getUtilisateurId())
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID : " + commandeDTO.getUtilisateurId()));
            commande.setUtilisateur(utilisateur);
        }

        commande = commandeRepository.save(commande);
        return convertToDTO(commande);
    }

    // 🔹 Créer une commande avec lignes
    public CommandeDTO createCommandeWithLignes(CommandeDTO commandeDTO) {
        Commande commande = new Commande();
        commande.setDateCommande(LocalDate.now());
        commande.setMontantTotal(BigDecimal.ZERO);

        // Initialisation du statut
        if ("CB".equalsIgnoreCase(commandeDTO.getModePaiement())) {
            commande.setStatut("PAYEE");
        } else {
            commande.setStatut("EN_ATTENTE");
        }

        commande.setModePaiement(commandeDTO.getModePaiement());

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
                ligne.setSousTotal(ligneDTO.getPrixUnitaire() * ligneDTO.getQuantite());
                ligne.setTaille(ligneDTO.getTaille());
                ligne.setCouleur(ligneDTO.getCouleur());
                ligne.setFlocage(ligneDTO.getFlocage());

                total = total.add(BigDecimal.valueOf(ligne.getSousTotal()));
                ligne = ligneCommandeRepository.save(ligne);
                lignes.add(ligne);
            }
        }

        commande.setMontantTotal(total);
        commande = commandeRepository.save(commande);

        CommandeDTO resultDTO = convertToDTO(commande);
        resultDTO.setLignesCommande(lignes.stream().map(this::convertLigneToDTO).collect(Collectors.toList()));
        return resultDTO;
    }

    // 🔹 Supprimer une commande
    public void deleteCommande(Long id) {
        if (commandeRepository.existsById(id)) {
            commandeRepository.deleteById(id);
        } else {
            throw new IllegalArgumentException("Commande avec ID " + id + " non trouvée.");
        }
    }

    // 🔹 Valider manuellement un paiement
    public void validerPaiementManuel(Long id, String statut, String modePaiement, String datePaiement) {
        Commande commande = commandeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Commande non trouvée avec l'ID : " + id));

        commande.setStatut(statut);
        commande.setModePaiement(modePaiement);
        commande.setDatePaiement(LocalDate.parse(datePaiement)); // Conversion de la date
        commandeRepository.save(commande);
    }
    public void mettreAJourCommande(Long id, CommandeUpdateDTO updateDTO) {
        Commande commande = commandeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Commande non trouvée avec l'ID : " + id));

        if (updateDTO.getStatut() != null) {
            commande.setStatut(updateDTO.getStatut());
        }
        if (updateDTO.getModePaiement() != null) {
            commande.setModePaiement(updateDTO.getModePaiement());
        }
        if (updateDTO.getDatePaiement() != null) {
            commande.setDatePaiement(updateDTO.getDatePaiement());
        }

        commandeRepository.save(commande);
    }
    // 🔹 Récupérer les commandes à payer au club
    public List<CommandeDTO> getCommandesPaiementClub() {
        return commandeRepository.findAll().stream()
            .filter(c -> "EN_ATTENTE".equals(c.getStatut()) && c.getModePaiement() != null
                && (
                    c.getModePaiement().equalsIgnoreCase("especes") ||
                    c.getModePaiement().equalsIgnoreCase("virement") ||
                    c.getModePaiement().equalsIgnoreCase("CLUB") // ✅ Ajouté ici
                )
            )
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    public void definirDisponibiliteAuClub(Long id, boolean disponible) {
        Commande commande = commandeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Commande non trouvée avec l'ID : " + id));
        commande.setDisponibleAuClub(disponible);
        commandeRepository.save(commande);
    }
    // 🔁 Conversion Commande -> DTO
    private CommandeDTO convertToDTO(Commande commande) {
        CommandeDTO dto = new CommandeDTO();
        dto.setId(commande.getId());
        dto.setDateCommande(commande.getDateCommande());
        dto.setMontantTotal(commande.getMontantTotal());
        dto.setModePaiement(commande.getModePaiement());
        dto.setDatePaiement(commande.getDatePaiement());
        dto.setStatut(commande.getStatut());
        dto.setDisponibleAuClub(commande.getDisponibleAuClub());

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

    // 🔁 Conversion LigneCommande -> DTO
    private LigneCommandeDTO convertLigneToDTO(LigneCommande ligne) {
        LigneCommandeDTO dto = new LigneCommandeDTO();
        dto.setId(ligne.getId());
        dto.setCommandeId(ligne.getCommande().getId());

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
        return dto;
    }
}