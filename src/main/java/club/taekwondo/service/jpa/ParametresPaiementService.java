package club.taekwondo.service.jpa;

import club.taekwondo.dto.ParametresPaiementDTO;
import club.taekwondo.entity.jpa.ParametresPaiement;
import club.taekwondo.repository.jpa.ParametresPaiementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ParametresPaiementService {

    @Autowired
    private ParametresPaiementRepository parametresPaiementRepository;

    // Récupérer les paramètres
    public ParametresPaiementDTO getParametresPaiement() {
        ParametresPaiement entity = parametresPaiementRepository.findById(1L).orElse(new ParametresPaiement());
        return mapToDTO(entity);
    }

    // Mettre à jour les paramètres
    public void updateParametresPaiement(ParametresPaiementDTO parametres) {
        ParametresPaiement entity = mapToEntity(parametres);
        entity.setId(1L); // Utilisez un ID fixe si vous gérez un seul ensemble de paramètres
        parametresPaiementRepository.save(entity);
    }

    // Mapper l'entité vers le DTO
    private ParametresPaiementDTO mapToDTO(ParametresPaiement entity) {
        ParametresPaiementDTO dto = new ParametresPaiementDTO();
        dto.setMontantCotisation(entity.getMontantCotisation());
        dto.setVirement(entity.isVirement());
        dto.setEspeces(entity.isEspeces());
        dto.setStripe(entity.isStripe());
        dto.setModePaiementParDefaut(entity.getModePaiementParDefaut());
        dto.setEcheancesAutorisees(entity.getEcheancesAutorisees());
        dto.setIntervalleEcheance(entity.getIntervalleEcheance());
        return dto;
    }

    // Mapper le DTO vers l'entité
    private ParametresPaiement mapToEntity(ParametresPaiementDTO dto) {
        ParametresPaiement entity = new ParametresPaiement();
        entity.setMontantCotisation(dto.getMontantCotisation());
        entity.setVirement(dto.isVirement());
        entity.setEspeces(dto.isEspeces());
        entity.setStripe(dto.isStripe());
        entity.setModePaiementParDefaut(dto.getModePaiementParDefaut());
        entity.setEcheancesAutorisees(dto.getEcheancesAutorisees());
        entity.setIntervalleEcheance(dto.getIntervalleEcheance());
        return entity;
    }
}