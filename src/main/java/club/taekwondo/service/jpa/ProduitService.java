package club.taekwondo.service.jpa;

import club.taekwondo.entity.jpa.Produit;
import club.taekwondo.repository.jpa.ProduitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProduitService {

    @Autowired
    private ProduitRepository produitRepository;

    // Récupérer tous les produits
    public List<Produit> getAllProduits() {
        return produitRepository.findAll();
    }

    // Récupérer un produit par son ID
    public Optional<Produit> getProduitById(Long id) {
        return produitRepository.findById(id);
    }

    // Ajouter un nouveau produit
    public Produit createProduit(Produit produit) {
        return produitRepository.save(produit);
    }

    // Mettre à jour un produit existant
    public Produit updateProduit(Long id, Produit produit) {
        if (produitRepository.existsById(id)) {
            produit.setId(id);
            return produitRepository.save(produit);
        }
        return null; // ou lever une exception
    }

    // Supprimer un produit
    public void deleteProduit(Long id) {
        if (produitRepository.existsById(id)) {
            produitRepository.deleteById(id);
        } else {
            throw new IllegalArgumentException("Le produit avec l'ID " + id + " n'existe pas.");
        }
    }
}