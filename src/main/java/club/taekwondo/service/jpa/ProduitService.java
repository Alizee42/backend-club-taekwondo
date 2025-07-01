package club.taekwondo.service.jpa;

import club.taekwondo.dto.ProduitDTO;
import club.taekwondo.entity.jpa.Produit;
import club.taekwondo.repository.jpa.ProduitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    // 🔁 Créer un produit 
    public ProduitDTO createProduit(ProduitDTO produitDTO) {
        Produit produit = convertToEntity(produitDTO);
        return convertToDTO(produitRepository.save(produit));
    }

    // 🔁 Mettre à jour un produit 
    public ProduitDTO updateProduit(Long id, ProduitDTO produitDTO) {
        if (!produitRepository.existsById(id)) {
            throw new IllegalArgumentException("Le produit avec l'ID " + id + " n'existe pas.");
        }
        Produit produit = convertToEntity(produitDTO);
        produit.setId(id);
        return convertToDTO(produitRepository.save(produit));
    }

    // ❌ Supprimer un produit
    public void deleteProduit(Long id) {
        if (produitRepository.existsById(id)) {
            produitRepository.deleteById(id);
        } else {
            throw new IllegalArgumentException("Le produit avec l'ID " + id + " n'existe pas.");
        }
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
