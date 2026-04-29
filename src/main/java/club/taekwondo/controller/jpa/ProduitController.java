package club.taekwondo.controller.jpa;

import club.taekwondo.dto.ProduitDTO;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.service.common.FileUploadService;
import club.taekwondo.service.jpa.ProduitService;
import club.taekwondo.service.jpa.UtilisateurService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/produits")
public class ProduitController {
    // 🔒 Récupérer tous les produits d'un club
    @GetMapping("/club/{clubId}")
    public ResponseEntity<List<ProduitDTO>> getProduitsByClub(@PathVariable Long clubId) {
        return ResponseEntity.ok(produitService.getProduitsByClubId(clubId));
    }

    @Autowired
    private ProduitService produitService;
    @Autowired
    private UtilisateurService utilisateurService;
    @Autowired
    private FileUploadService fileUploadService;

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
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ProduitDTO> createProduit(@RequestBody ProduitDTO produitDTO,
                                                     Authentication authentication) {
        if (produitDTO.getClubId() == null) {
            utilisateurService.findByEmail(authentication.getName()).ifPresent(caller -> {
                if (caller.getClub() != null) {
                    produitDTO.setClubId(caller.getClub().getId());
                }
            });
        }
        try {
            ProduitDTO createdProduit = produitService.createProduit(produitDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdProduit);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // 🔄 Mettre à jour un produit existant
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ProduitDTO> updateProduit(@PathVariable Long id,
                                                     @RequestBody ProduitDTO produitDTO,
                                                     Authentication authentication) {
        if (produitDTO.getClubId() == null) {
            utilisateurService.findByEmail(authentication.getName()).ifPresent(caller -> {
                if (caller.getClub() != null) {
                    produitDTO.setClubId(caller.getClub().getId());
                }
            });
        }
        try {
            ProduitDTO updatedProduit = produitService.updateProduit(id, produitDTO);
            return ResponseEntity.ok(updatedProduit);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // 🖼️ Upload d'image produit
    @PostMapping("/upload-image")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("image") MultipartFile image) {
        try {
            String path = fileUploadService.uploadFile(image, "produits");
            return ResponseEntity.ok(Map.of("url", "/uploads/" + path));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ❌ Supprimer un produit
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> deleteProduit(@PathVariable Long id) {
        try {
            produitService.deleteProduit(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
