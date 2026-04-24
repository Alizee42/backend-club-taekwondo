package club.taekwondo.service.jpa;

import club.taekwondo.dto.ParametresPaiementDTO;
import club.taekwondo.entity.jpa.Club;
import club.taekwondo.entity.jpa.ParametresPaiement;
import club.taekwondo.repository.jpa.ParametresPaiementRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ParametresPaiementService {

    @Autowired
    private ParametresPaiementRepository parametresPaiementRepository;

    @PersistenceContext
    private EntityManager entityManager;

    /** ✅ Lecture avec fallback (utilisé par ADMIN et par le GET public) */
       // ...existing code...
    
    public ParametresPaiementDTO getParametresPaiementByClub(Long clubId) {
        ParametresPaiement entity = parametresPaiementRepository.findByClub_Id(clubId);
        if (entity != null) {
            return mapToDTO(entity);
        } else {
            ParametresPaiementDTO defaut = new ParametresPaiementDTO();
            defaut.setMontantCotisation(100);
            defaut.setVirement(true);
            defaut.setEspeces(true);
            defaut.setStripe(true);
            defaut.setModePaiementParDefaut("stripe");
            defaut.setEcheancesAutorisees(4);
            defaut.setIntervalleEcheance("MENSUEL");
            return defaut;
        }
    }
    
    // ...existing code...

    /** ✅ Écriture ADMIN : met à jour (ou crée) la ligne ID=1 de façon sûre */
    @Transactional
    public void updateParametresPaiement(Long clubId, ParametresPaiementDTO dto) {
        ParametresPaiement entity = parametresPaiementRepository.findByClub_Id(clubId);
        if (entity == null) {
            entity = new ParametresPaiement();
            entity.setId(clubId);
            entity.setClub(entityManager.getReference(Club.class, clubId));
        }
        entity.setMontantCotisation(dto.getMontantCotisation());
        entity.setVirement(dto.isVirement());
        entity.setEspeces(dto.isEspeces());
        entity.setStripe(dto.isStripe());
        entity.setModePaiementParDefaut(dto.getModePaiementParDefaut());
        entity.setEcheancesAutorisees(dto.getEcheancesAutorisees());
        entity.setIntervalleEcheance(dto.getIntervalleEcheance());
        parametresPaiementRepository.save(entity);
    }

    // ---------- Mappers ----------
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
}

