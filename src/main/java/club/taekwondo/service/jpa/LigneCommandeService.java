package club.taekwondo.service.jpa;

import club.taekwondo.dto.LigneCommandeDTO;
import club.taekwondo.entity.jpa.Commande;
import club.taekwondo.entity.jpa.LigneCommande;
import club.taekwondo.entity.jpa.Produit;
import club.taekwondo.repository.jpa.CommandeRepository;
import club.taekwondo.repository.jpa.LigneCommandeRepository;
import club.taekwondo.repository.jpa.ProduitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class LigneCommandeService {

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
        return ligneCommandeDTO;
    }

    // Conversion DTO → Entity
    private LigneCommande convertToEntity(LigneCommandeDTO ligneCommandeDTO) {
        LigneCommande ligneCommande = new LigneCommande();
        ligneCommande.setQuantite(ligneCommandeDTO.getQuantite());
        ligneCommande.setPrixUnitaire(ligneCommandeDTO.getPrixUnitaire());
        ligneCommande.setSousTotal(ligneCommandeDTO.getSousTotal());

        Commande commande = commandeRepository.findById(ligneCommandeDTO.getCommandeId())
                .orElseThrow(() -> new RuntimeException("Commande non trouvée"));
        Produit produit = produitRepository.findById(ligneCommandeDTO.getProduitId())
                .orElseThrow(() -> new RuntimeException("Produit non trouvé"));

        ligneCommande.setCommande(commande);
        ligneCommande.setProduit(produit);

        return ligneCommande;
    }
}
