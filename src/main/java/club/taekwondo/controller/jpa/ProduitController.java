package club.taekwondo.controller.jpa;

import club.taekwondo.dto.ProduitDTO;
import club.taekwondo.service.jpa.ProduitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/produits")
public class ProduitController {

    @Autowired
    private ProduitService produitService;

    // 🔁 Récupérer tous les produits
    @GetMapping
    public ResponseEntity<List<ProduitDTO>> getAllProduits() {
        return ResponseEntity.ok(produitService.getAllProduits());
    }

    // 🔁 Récupérer un produit par ID
    @GetMapping("/{id}")
    public ResponseEntity<ProduitDTO> getProduitById(@PathVariable Long id) {
        Optional<ProduitDTO> produit = produitService.getProduitById(id);
        return produit.map(ResponseEntity::ok)
                      .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ➕ Créer un nouveau produit
    @PostMapping
    public ResponseEntity<ProduitDTO> createProduit(@RequestBody ProduitDTO produitDTO) {
        ProduitDTO createdProduit = produitService.createProduit(produitDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdProduit);
    }

    // 🔄 Mettre à jour un produit existant
    @PutMapping("/{id}")
    public ResponseEntity<ProduitDTO> updateProduit(@PathVariable Long id, @RequestBody ProduitDTO produitDTO) {
        try {
            ProduitDTO updatedProduit = produitService.updateProduit(id, produitDTO);
            return ResponseEntity.ok(updatedProduit);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ❌ Supprimer un produit
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduit(@PathVariable Long id) {
        try {
            produitService.deleteProduit(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
