package club.taekwondo.service.jpa;

import club.taekwondo.entity.jpa.Commande;
import club.taekwondo.repository.jpa.CommandeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CommandeService {

    @Autowired
    private CommandeRepository commandeRepository;

    // Récupérer toutes les commandes
    public List<Commande> getAllCommandes() {
        return commandeRepository.findAll();
    }

    // Récupérer une commande par son ID
    public Optional<Commande> getCommandeById(Long id) {
        return commandeRepository.findById(id);
    }

    // Ajouter une nouvelle commande
    public Commande createCommande(Commande commande) {
        return commandeRepository.save(commande);
    }

    // Mettre à jour une commande existante
    public Commande updateCommande(Long id, Commande commande) {
        if (commandeRepository.existsById(id)) {
            commande.setId(id);
            return commandeRepository.save(commande);
        }
        return null; // ou lever une exception
    }

    // Supprimer une commande
    public void deleteCommande(Long id) {
        if (commandeRepository.existsById(id)) {
            commandeRepository.deleteById(id);
        } else {
            throw new IllegalArgumentException("La commande avec l'ID " + id + " n'existe pas.");
        }
    }
}