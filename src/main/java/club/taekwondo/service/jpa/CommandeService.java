package club.taekwondo.service.jpa;

import club.taekwondo.dto.CommandeDTO;
import club.taekwondo.entity.jpa.Commande;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.repository.jpa.CommandeRepository;
import club.taekwondo.repository.jpa.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CommandeService {

    @Autowired
    private CommandeRepository commandeRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    // 🔹 Récupérer toutes les commandes
    public List<CommandeDTO> getAllCommandes() {
        return commandeRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // 🔹 Récupérer une commande par son ID
    public Optional<CommandeDTO> getCommandeById(Long id) {
        return commandeRepository.findById(id).map(this::convertToDTO);
    }

    // 🔹 Créer une commande
    public CommandeDTO createCommande(CommandeDTO commandeDTO) {
        Commande commande = convertToEntity(commandeDTO);
        Commande saved = commandeRepository.save(commande);
        return convertToDTO(saved);
    }

    // 🔹 Mettre à jour une commande existante
    public CommandeDTO updateCommande(Long id, CommandeDTO commandeDTO) {
        Commande existing = commandeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Commande non trouvée avec l'ID : " + id));
        
        existing.setDateCommande(commandeDTO.getDateCommande());
        existing.setMontantTotal(commandeDTO.getMontantTotal());

        if (commandeDTO.getUtilisateurId() != null) {
            Utilisateur utilisateur = utilisateurRepository.findById(commandeDTO.getUtilisateurId())
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
            existing.setUtilisateur(utilisateur);
        }

        return convertToDTO(commandeRepository.save(existing));
    }

    // 🔹 Supprimer une commande
    public void deleteCommande(Long id) {
        if (commandeRepository.existsById(id)) {
            commandeRepository.deleteById(id);
        } else {
            throw new IllegalArgumentException("Commande avec ID " + id + " non trouvée.");
        }
    }

    // 🔁 Conversion Commande -> DTO
    private CommandeDTO convertToDTO(Commande commande) {
        CommandeDTO dto = new CommandeDTO();
        dto.setId(commande.getId());
        dto.setDateCommande(commande.getDateCommande());
        dto.setMontantTotal(commande.getMontantTotal());
        dto.setUtilisateurId(commande.getUtilisateur() != null ? commande.getUtilisateur().getId() : null);
        return dto;
    }

    // 🔁 Conversion DTO -> Commande
    private Commande convertToEntity(CommandeDTO dto) {
        Commande commande = new Commande();
        commande.setDateCommande(dto.getDateCommande());
        commande.setMontantTotal(dto.getMontantTotal());

        if (dto.getUtilisateurId() != null) {
            Utilisateur utilisateur = utilisateurRepository.findById(dto.getUtilisateurId())
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID : " + dto.getUtilisateurId()));
            commande.setUtilisateur(utilisateur);
        }

        return commande;
    }
}
