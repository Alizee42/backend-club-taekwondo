package club.taekwondo.service.jpa;

import club.taekwondo.dto.ProduitDTO;
import club.taekwondo.entity.jpa.Produit;
import club.taekwondo.repository.jpa.ProduitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProduitService {

    @Autowired
    private ProduitRepository produitRepository;

    // 🔁 Récupérer tous les produits 
    public List<ProduitDTO> getAllProduits() {
        return produitRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // 🔁 Récupérer un produit par ID (DTO)
    public Optional<ProduitDTO> getProduitById(Long id) {
        return produitRepository.findById(id).map(this::convertToDTO);
    }

    // 🚨 Nouvelle méthode pour obtenir un produit Entity directement par ID
    public Optional<Produit> getProduitEntityById(Long id) {
        return produitRepository.findById(id);
    }

    // 🔁 Créer un produit 
    public ProduitDTO createProduit(ProduitDTO produitDTO) {
        if (produitRepository.existsByNom(produitDTO.getNom())) {
            throw new IllegalArgumentException("Un produit avec ce nom existe déjà.");
        }
        Produit produit = convertToEntity(produitDTO);
        
        // Comparaison avec BigDecimal
        if (produit.getPrix().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le prix du produit doit être supérieur à zéro.");
        }
        if (produit.getStock() < 0) {
            throw new IllegalArgumentException("Le stock du produit ne peut pas être négatif.");
        }
        return convertToDTO(produitRepository.save(produit));
    }

    // 🔁 Mettre à jour un produit 
    public ProduitDTO updateProduit(Long id, ProduitDTO produitDTO) {
        if (!produitRepository.existsById(id)) {
            throw new IllegalArgumentException("Le produit avec l'ID " + id + " n'existe pas.");
        }
        Produit produit = convertToEntity(produitDTO);
        produit.setId(id);
        
        // Comparaison avec BigDecimal
        if (produit.getPrix().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le prix du produit doit être supérieur à zéro.");
        }
        if (produit.getStock() < 0) {
            throw new IllegalArgumentException("Le stock du produit ne peut pas être négatif.");
        }
        return convertToDTO(produitRepository.save(produit));
    }

    // ❌ Supprimer un produit
    public void deleteProduit(Long id) {
        if (!produitRepository.existsById(id)) {
            throw new IllegalArgumentException("Le produit avec l'ID " + id + " n'existe pas.");
        }
        produitRepository.deleteById(id);
    }

    // 🔁 Conversion Entity → DTO
    private ProduitDTO convertToDTO(Produit produit) {
        ProduitDTO produitDTO = new ProduitDTO();
        produitDTO.setId(produit.getId());
        produitDTO.setNom(produit.getNom());
        produitDTO.setDescription(produit.getDescription());
        produitDTO.setPrix(produit.getPrix());
        produitDTO.setStock(produit.getStock());
        produitDTO.setCategorie(produit.getCategorie());
        produitDTO.setImageUrl(produit.getImageUrl());
        return produitDTO;
    }

    // 🔁 Conversion DTO → Entity
    private Produit convertToEntity(ProduitDTO produitDTO) {
        Produit produit = new Produit();
        produit.setId(produitDTO.getId());
        produit.setNom(produitDTO.getNom());
        produit.setDescription(produitDTO.getDescription());
        produit.setPrix(produitDTO.getPrix());
        produit.setStock(produitDTO.getStock());
        produit.setCategorie(produitDTO.getCategorie());
        produit.setImageUrl(produitDTO.getImageUrl());
        return produit;
    }
}


