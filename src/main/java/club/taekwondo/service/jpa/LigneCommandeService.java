package club.taekwondo.service.jpa;

import club.taekwondo.dto.LigneCommandeDTO;
import club.taekwondo.entity.jpa.Commande;
import club.taekwondo.entity.jpa.LigneCommande;
import club.taekwondo.entity.jpa.Membre;
import club.taekwondo.entity.jpa.Paiement;
import club.taekwondo.entity.jpa.Produit;
import club.taekwondo.repository.jpa.CommandeRepository;
import club.taekwondo.repository.jpa.LigneCommandeRepository;
import club.taekwondo.repository.jpa.ProduitRepository;
import club.taekwondo.repository.jpa.MembreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class LigneCommandeService {

    private static final Logger log = LoggerFactory.getLogger(LigneCommandeService.class);

    @Autowired private LigneCommandeRepository ligneCommandeRepository;
    @Autowired private CommandeRepository commandeRepository;
    @Autowired private ProduitRepository produitRepository;
    @Autowired private MembreRepository membreRepository; // ✅ manquant auparavant

    public List<LigneCommandeDTO> getAllLignesCommande() {
        return ligneCommandeRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public Optional<LigneCommandeDTO> getLigneCommandeById(Long id) {
        return ligneCommandeRepository.findById(id).map(this::convertToDTO);
    }

    @Transactional
    public LigneCommandeDTO createLigneCommande(LigneCommandeDTO ligneCommande) {
        LigneCommande entity = convertToEntity(ligneCommande);
        return convertToDTO(ligneCommandeRepository.save(entity));
    }

    @Transactional
    public LigneCommandeDTO updateLigneCommande(Long id, LigneCommandeDTO ligneCommande) {
        if (!ligneCommandeRepository.existsById(id)) {
            throw new RuntimeException("Ligne de commande non trouvée avec ID : " + id);
        }
        LigneCommande entity = convertToEntity(ligneCommande);
        entity.setId(id);
        return convertToDTO(ligneCommandeRepository.save(entity));
    }

    @Transactional
    public void deleteLigneCommande(Long id) {
        ligneCommandeRepository.deleteById(id);
    }

    // =========================
    //    Conversion Entity → DTO
    // =========================
    private LigneCommandeDTO convertToDTO(LigneCommande ligneCommande) {
        LigneCommandeDTO dto = new LigneCommandeDTO();
        dto.setId(ligneCommande.getId());

        // null-safety
        Commande cmd = ligneCommande.getCommande();
        Produit prod = ligneCommande.getProduit();
        Membre ben = ligneCommande.getBeneficiaire();

        dto.setCommandeId(cmd != null ? cmd.getId() : null);
        dto.setProduitId(prod != null ? prod.getId() : null);

        dto.setQuantite(ligneCommande.getQuantite());
        dto.setPrixUnitaire(ligneCommande.getPrixUnitaire());
        dto.setSousTotal(ligneCommande.getSousTotal());

        // Champs personnalisés
        dto.setTaille(ligneCommande.getTaille());
        dto.setCouleur(ligneCommande.getCouleur());
        dto.setFlocage(ligneCommande.getFlocage());

        // ✅ Bénéficiaire (enfant)
        if (ben != null) {
            dto.setBeneficiaireId(ben.getId());
            dto.setBeneficiairePrenom(ben.getPrenom());
            dto.setBeneficiaireNom(ben.getNom());
        }

        return dto;
    }

    // =========================
    //    Conversion DTO → Entity
    // =========================
    private LigneCommande convertToEntity(LigneCommandeDTO dto) {
        LigneCommande entity = new LigneCommande();

        entity.setQuantite(dto.getQuantite());
        entity.setPrixUnitaire(dto.getPrixUnitaire());
        entity.setSousTotal(dto.getSousTotal());

        entity.setTaille(dto.getTaille());
        entity.setCouleur(dto.getCouleur());
        entity.setFlocage(dto.getFlocage());

        // Rattachements obligatoires
        Commande commande = commandeRepository.findById(dto.getCommandeId())
                .orElseThrow(() -> new RuntimeException("Commande non trouvée: " + dto.getCommandeId()));
        Produit produit = produitRepository.findById(dto.getProduitId())
                .orElseThrow(() -> new RuntimeException("Produit non trouvé: " + dto.getProduitId()));

        entity.setCommande(commande);
        entity.setProduit(produit);

        // ✅ Bénéficiaire (enfant) optionnel
        if (dto.getBeneficiaireId() != null) {
            Membre enfant = membreRepository.findById(dto.getBeneficiaireId())
                    .orElseThrow(() -> new RuntimeException("Bénéficiaire (membre) introuvable: " + dto.getBeneficiaireId()));
            entity.setBeneficiaire(enfant);
        } else {
            entity.setBeneficiaire(null);
        }

        return entity;
    }

    /**
     * ✅ Utilise la vraie commande liée au paiement
     */
    @Transactional
public LigneCommande creerPourPaiement(Paiement paiement,
                                       Produit produit,
                                       double prixUnitaire,
                                       int quantite,
                                       String taille,
                                       String couleur,
                                       boolean flocageActif,
                                       String flocage,
                                       Long beneficiaireId) {

    // Vérifs de base
    if (paiement == null || paiement.getId() == null) {
        throw new IllegalArgumentException("Le paiement doit avoir un ID valide.");
    }
    if (paiement.getCommande() == null || paiement.getCommande().getId() == null) {
        throw new IllegalArgumentException("Le paiement doit être lié à une commande valide.");
    }
    if (produit == null) {
        throw new IllegalArgumentException("Produit invalide.");
    }
    if (prixUnitaire < 0) {
        throw new IllegalArgumentException("Prix unitaire invalide.");
    }
    if (quantite <= 0) {
        throw new IllegalArgumentException("Quantité invalide.");
    }

    // Arrondis
    double unit = Math.round(prixUnitaire * 100.0) / 100.0;
    double sousTotal = Math.round(unit * quantite * 100.0) / 100.0;

    // Création de la ligne
    LigneCommande ligne = new LigneCommande();
    ligne.setCommande(paiement.getCommande());
    ligne.setProduit(produit);
    ligne.setQuantite(quantite);
    ligne.setPrixUnitaire(unit);
    ligne.setSousTotal(sousTotal);
    ligne.setTaille(taille);
    ligne.setCouleur(couleur);
    ligne.setFlocage(flocageActif ? flocage : null);

    // ✅ Bénéficiaire (enfant) optionnel
    if (beneficiaireId != null) {
        Membre ben = membreRepository.findById(beneficiaireId)
                .orElseThrow(() -> new IllegalArgumentException("Bénéficiaire introuvable id=" + beneficiaireId));
        ligne.setBeneficiaire(ben);
    }

    LigneCommande saved = ligneCommandeRepository.save(ligne);

    // ✅ PAS de mise à jour cumulative - le total est déjà correct dans PaiementService
    // La commande a déjà le bon montant total calculé, pas besoin de l'additionner à nouveau
    
    log.info("📦 LigneCommande créée: id={} | cmd={} | produit={} | qte={} | prix={} | total={} | benef={}",
            saved.getId(),
            paiement.getCommande().getId(),
            produit.getId(),
            saved.getQuantite(),
            saved.getPrixUnitaire(),
            saved.getSousTotal(),
            saved.getBeneficiaire() != null ? saved.getBeneficiaire().getId() : null
    );

    return saved;
}

    /**
     * 🔍 Méthode pour récupérer les lignes d'une commande
     * (Astuce: idéalement, crée ligneCommandeRepository.findByCommandeId(commandeId))
     */
    public List<LigneCommande> getLignesParCommande(Long commandeId) {
        // TODO: remplace par un vrai finder du repository pour éviter de charger tout
        return ligneCommandeRepository.findAll().stream()
                .filter(ligne -> ligne.getCommande() != null &&
                        commandeId.equals(ligne.getCommande().getId()))
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
