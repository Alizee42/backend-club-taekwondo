package club.taekwondo.service.jpa;

import club.taekwondo.entity.jpa.LigneCommande;
import club.taekwondo.repository.jpa.LigneCommandeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class LigneCommandeService {

    @Autowired
    private LigneCommandeRepository ligneCommandeRepository;

    // Récupérer toutes les lignes de commande
    public List<LigneCommande> getAllLignesCommande() {
        return ligneCommandeRepository.findAll();
    }

    // Récupérer une ligne de commande par son ID
    public Optional<LigneCommande> getLigneCommandeById(Long id) {
        return ligneCommandeRepository.findById(id);
    }

    // Ajouter une nouvelle ligne de commande
    public LigneCommande createLigneCommande(LigneCommande ligneCommande) {
        return ligneCommandeRepository.save(ligneCommande);
    }

    // Mettre à jour une ligne de commande existante
    public LigneCommande updateLigneCommande(Long id, LigneCommande ligneCommande) {
        if (ligneCommandeRepository.existsById(id)) {
            ligneCommande.setId(id);
            return ligneCommandeRepository.save(ligneCommande);
        }
        return null; // ou lever une exception
    }

    // Supprimer une ligne de commande
    public void deleteLigneCommande(Long id) {
        if (ligneCommandeRepository.existsById(id)) {
            ligneCommandeRepository.deleteById(id);
        } else {
            throw new IllegalArgumentException("La ligne de commande avec l'ID " + id + " n'existe pas.");
        }
    }
}