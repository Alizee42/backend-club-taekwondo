package club.taekwondo.service.jpa;

import club.taekwondo.dto.LigneCommandeDTO;
import club.taekwondo.entity.jpa.Commande;
import club.taekwondo.entity.jpa.LigneCommande;
import club.taekwondo.entity.jpa.Paiement;
import club.taekwondo.entity.jpa.Produit;
import club.taekwondo.repository.jpa.CommandeRepository;
import club.taekwondo.repository.jpa.LigneCommandeRepository;
import club.taekwondo.repository.jpa.ProduitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class LigneCommandeService {

    private static final Logger log = LoggerFactory.getLogger(LigneCommandeService.class);

    @Autowired
    private LigneCommandeRepository ligneCommandeRepository;

    @Autowired
    private CommandeRepository commandeRepository;

    @Autowired
    private ProduitRepository produitRepository;

    public List<LigneCommandeDTO> getAllLignesCommande() {
        return ligneCommandeRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public Optional<LigneCommandeDTO> getLigneCommandeById(Long id) {
        return ligneCommandeRepository.findById(id).map(this::convertToDTO);
    }

    public LigneCommandeDTO createLigneCommande(LigneCommandeDTO ligneCommande) {
        LigneCommande entity = convertToEntity(ligneCommande);
        return convertToDTO(ligneCommandeRepository.save(entity));
    }

    public LigneCommandeDTO updateLigneCommande(Long id, LigneCommandeDTO ligneCommande) {
        if (!ligneCommandeRepository.existsById(id)) {
            throw new RuntimeException("Ligne de commande non trouvée avec ID : " + id);
        }
        LigneCommande entity = convertToEntity(ligneCommande);
        entity.setId(id);
        return convertToDTO(ligneCommandeRepository.save(entity));
    }

    public void deleteLigneCommande(Long id) {
        ligneCommandeRepository.deleteById(id);
    }

    // Conversion Entity → DTO
    private LigneCommandeDTO convertToDTO(LigneCommande ligneCommande) {
        LigneCommandeDTO ligneCommandeDTO = new LigneCommandeDTO();
        ligneCommandeDTO.setId(ligneCommande.getId());
        ligneCommandeDTO.setCommandeId(ligneCommande.getCommande().getId());
        ligneCommandeDTO.setProduitId(ligneCommande.getProduit().getId());
        ligneCommandeDTO.setQuantite(ligneCommande.getQuantite());
        ligneCommandeDTO.setPrixUnitaire(ligneCommande.getPrixUnitaire());
        ligneCommandeDTO.setSousTotal(ligneCommande.getSousTotal());

        // Champs personnalisés :
        ligneCommandeDTO.setTaille(ligneCommande.getTaille());
        ligneCommandeDTO.setCouleur(ligneCommande.getCouleur());
        ligneCommandeDTO.setFlocage(ligneCommande.getFlocage());

        return ligneCommandeDTO;
    }

    // Conversion DTO → Entity
    private LigneCommande convertToEntity(LigneCommandeDTO ligneCommandeDTO) {
        LigneCommande ligneCommande = new LigneCommande();
        ligneCommande.setQuantite(ligneCommandeDTO.getQuantite());
        ligneCommande.setPrixUnitaire(ligneCommandeDTO.getPrixUnitaire());
        ligneCommande.setSousTotal(ligneCommandeDTO.getSousTotal());

        ligneCommande.setTaille(ligneCommandeDTO.getTaille());
        ligneCommande.setCouleur(ligneCommandeDTO.getCouleur());
        ligneCommande.setFlocage(ligneCommandeDTO.getFlocage());

        Commande commande = commandeRepository.findById(ligneCommandeDTO.getCommandeId())
                .orElseThrow(() -> new RuntimeException("Commande non trouvée"));
        Produit produit = produitRepository.findById(ligneCommandeDTO.getProduitId())
                .orElseThrow(() -> new RuntimeException("Produit non trouvé"));

        ligneCommande.setCommande(commande);
        ligneCommande.setProduit(produit);

        return ligneCommande;
    }

    /**
     * ✅ MÉTHODE CORRIGÉE : Utilise la vraie commande liée au paiement
     */
    public LigneCommande creerPourPaiement(Paiement paiement, Produit produit, double prixUnitaire, int quantite, 
                                            String taille, String couleur, boolean flocageActif, String flocage) {

        // Vérifications
        if (paiement == null || paiement.getId() == null) {
            throw new IllegalArgumentException("Le paiement doit avoir un ID valide");
        }
        if (paiement.getCommande() == null || paiement.getCommande().getId() == null) {
            throw new IllegalArgumentException("Le paiement doit être lié à une commande valide");
        }
        if (produit == null || prixUnitaire <= 0 || quantite <= 0) {
            throw new IllegalArgumentException("Produit, prix ou quantité invalides.");
        }

        // Calcul du sous-total
        double sousTotal = prixUnitaire * quantite;

        // Créer la ligne de commande
        LigneCommande ligneCommande = new LigneCommande();
        ligneCommande.setQuantite(quantite);
        ligneCommande.setPrixUnitaire(prixUnitaire);
        ligneCommande.setSousTotal(sousTotal);
        ligneCommande.setTaille(taille);
        ligneCommande.setCouleur(couleur);
        ligneCommande.setFlocage(flocageActif ? flocage : null);

        // ✅ UTILISER LA VRAIE COMMANDE du paiement
        ligneCommande.setCommande(paiement.getCommande());
        ligneCommande.setProduit(produit);

        // Sauvegarder
        LigneCommande saved = ligneCommandeRepository.save(ligneCommande);
        
        log.info("📦 LigneCommande créée: ID={} | CommandeID={} | ProduitID={} | Quantité={} | Prix={} | Total={}",
                saved.getId(), saved.getCommande().getId(), saved.getProduit().getId(), 
                saved.getQuantite(), saved.getPrixUnitaire(), saved.getSousTotal());

        return saved;
    }

    /**
     * 🔍 Méthode pour récupérer les lignes d'une commande
     */
    public List<LigneCommande> getLignesParCommande(Long commandeId) {
        return ligneCommandeRepository.findAll().stream()
                .filter(ligne -> ligne.getCommande() != null && 
                               ligne.getCommande().getId().equals(commandeId))
                .collect(Collectors.toList());
    }

    /**
     * 🔍 Méthode pour récupérer les lignes d'un paiement (via la commande liée)
     */
    public List<LigneCommande> getLignesParPaiement(Paiement paiement) {
        if (paiement == null || paiement.getCommande() == null) {
            return List.of();
        }
        return getLignesParCommande(paiement.getCommande().getId());
    }
}