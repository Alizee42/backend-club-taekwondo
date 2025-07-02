package club.taekwondo.service.jpa;

import club.taekwondo.dto.CommandeDTO;
import club.taekwondo.dto.LigneCommandeDTO;
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

    @Autowired
    private CommandeRepository commandeRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private ProduitRepository produitRepository;

    @Autowired
    private LigneCommandeRepository ligneCommandeRepository;

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
        Commande commande = convertToEntity(commandeDTO);
        commande.setStatut("EN_COURS"); // Ajout d'une valeur par défaut pour statut
        commande = commandeRepository.save(commande);
        return convertToDTO(commande);
    }

    // 🔹 Créer une commande avec lignes (depuis la boutique)
    public CommandeDTO createCommandeWithLignes(CommandeDTO commandeDTO) {
        Commande commande = new Commande();
        commande.setDateCommande(LocalDate.now());
        commande.setMontantTotal(BigDecimal.ZERO);
        commande.setStatut("EN_COURS"); // Ajout d'une valeur par défaut pour statut

        // Vérification de l'utilisateur
        if (commandeDTO.getUtilisateurId() != null) {
            Utilisateur utilisateur = utilisateurRepository.findById(commandeDTO.getUtilisateurId())
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID : " + commandeDTO.getUtilisateurId()));
            commande.setUtilisateur(utilisateur);
        }

        commande = commandeRepository.save(commande);

        BigDecimal total = BigDecimal.ZERO;
        List<LigneCommande> lignes = new ArrayList<>();

        // Création des lignes de commande
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
                ligne = ligneCommandeRepository.save(ligne); // Sauvegarde la ligne de commande
                lignes.add(ligne);
            }
        }

        commande.setMontantTotal(total); // Met à jour le montant total
        commande = commandeRepository.save(commande);

        CommandeDTO resultDTO = convertToDTO(commande);
        resultDTO.setLignesCommande(lignes.stream().map(this::convertLigneToDTO).collect(Collectors.toList())); // Retourne les lignes avec leurs IDs
        return resultDTO;
    }

    // 🔹 Mettre à jour une commande existante
    public CommandeDTO updateCommande(Long id, CommandeDTO commandeDTO) {
        Commande existing = commandeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Commande non trouvée avec l'ID : " + id));

        existing.setDateCommande(commandeDTO.getDateCommande());
        existing.setMontantTotal(commandeDTO.getMontantTotal());

        // Mise à jour de l'utilisateur
        if (commandeDTO.getUtilisateurId() != null) {
            Utilisateur utilisateur = utilisateurRepository.findById(commandeDTO.getUtilisateurId())
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            existing.setUtilisateur(utilisateur);
        }

        return convertToDTO(commandeRepository.save(existing));
    }

    // 🔹 Supprimer une commande
    public void deleteCommande(Long id) {
        if (commandeRepository.existsById(id)) {
            commandeRepository.deleteById(id);
        } else {
            throw new IllegalArgumentException("Commande avec ID " + id + " non trouvée.");
        }
    }

    // 🔁 Conversion Commande -> DTO
    private CommandeDTO convertToDTO(Commande commande) {
        CommandeDTO dto = new CommandeDTO();
        dto.setId(commande.getId());
        dto.setDateCommande(commande.getDateCommande());
        dto.setMontantTotal(commande.getMontantTotal());
        dto.setUtilisateurId(commande.getUtilisateur() != null ? commande.getUtilisateur().getId() : null);
        return dto;
    }

    // 🔁 Conversion LigneCommande -> DTO
    private LigneCommandeDTO convertLigneToDTO(LigneCommande ligne) {
        LigneCommandeDTO dto = new LigneCommandeDTO();
        dto.setId(ligne.getId());
        dto.setCommandeId(ligne.getCommande().getId());
        dto.setProduitNom(ligne.getProduit().getNom()); // Utilise le nom du produit
        dto.setQuantite(ligne.getQuantite());
        dto.setPrixUnitaire(ligne.getPrixUnitaire());
        dto.setSousTotal(ligne.getSousTotal());
        dto.setTaille(ligne.getTaille());
        dto.setCouleur(ligne.getCouleur());
        dto.setFlocage(ligne.getFlocage());
        return dto;
    }

    // 🔁 Conversion DTO -> Commande (pour create simple)
    private Commande convertToEntity(CommandeDTO dto) {
        Commande commande = new Commande();
        commande.setDateCommande(dto.getDateCommande());
        commande.setMontantTotal(dto.getMontantTotal());
        commande.setStatut("EN_COURS"); 

        if (dto.getUtilisateurId() != null) {
            Utilisateur utilisateur = utilisateurRepository.findById(dto.getUtilisateurId())
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID : " + dto.getUtilisateurId()));
            commande.setUtilisateur(utilisateur);
        }
        return commande;
    }
}
