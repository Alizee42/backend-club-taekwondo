package club.taekwondo.service.jpa;

import club.taekwondo.dto.CampagneCommandeDTO;
import club.taekwondo.entity.jpa.CampagneCommande;
import club.taekwondo.entity.jpa.Club;
import club.taekwondo.repository.jpa.CampagneCommandeRepository;
import club.taekwondo.repository.jpa.ClubRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CampagneCommandeService {

    private final CampagneCommandeRepository campagneRepository;
    private final ClubRepository clubRepository;

    public CampagneCommandeService(CampagneCommandeRepository campagneRepository, ClubRepository clubRepository) {
        this.campagneRepository = campagneRepository;
        this.clubRepository = clubRepository;
    }

    @Transactional(readOnly = true)
    public List<CampagneCommandeDTO> getParClub(Long clubId) {
        return campagneRepository.findByClub_Id(clubId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<CampagneCommandeDTO> getActive(Long clubId) {
        return campagneRepository.findActivePourClub(clubId, LocalDate.now()).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Optional<CampagneCommande> getActiveEntity(Long clubId) {
        return campagneRepository.findActivePourClub(clubId, LocalDate.now());
    }

    @Transactional(readOnly = true)
    public Optional<CampagneCommande> findById(Long id) {
        return campagneRepository.findById(id);
    }

    @Transactional
    public CampagneCommandeDTO creer(CampagneCommandeDTO dto) {
        Club club = clubRepository.findById(dto.getClubId())
                .orElseThrow(() -> new IllegalArgumentException("Club introuvable id=" + dto.getClubId()));
        CampagneCommande entity = new CampagneCommande();
        entity.setTitre(dto.getTitre());
        entity.setDescription(dto.getDescription());
        entity.setDateOuverture(dto.getDateOuverture());
        entity.setDateFermeture(dto.getDateFermeture());
        entity.setActif(dto.isActif());
        entity.setClub(club);
        return toDTO(campagneRepository.save(entity));
    }

    @Transactional
    public CampagneCommandeDTO modifier(Long id, CampagneCommandeDTO dto) {
        CampagneCommande entity = campagneRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Campagne introuvable id=" + id));
        if (dto.getTitre() != null) entity.setTitre(dto.getTitre());
        if (dto.getDescription() != null) entity.setDescription(dto.getDescription());
        if (dto.getDateOuverture() != null) entity.setDateOuverture(dto.getDateOuverture());
        if (dto.getDateFermeture() != null) entity.setDateFermeture(dto.getDateFermeture());
        entity.setActif(dto.isActif());
        return toDTO(campagneRepository.save(entity));
    }

    @Transactional
    public void supprimer(Long id) {
        campagneRepository.deleteById(id);
    }

    public CampagneCommandeDTO toDTO(CampagneCommande entity) {
        CampagneCommandeDTO dto = new CampagneCommandeDTO();
        dto.setId(entity.getId());
        dto.setTitre(entity.getTitre());
        dto.setDescription(entity.getDescription());
        dto.setDateOuverture(entity.getDateOuverture());
        dto.setDateFermeture(entity.getDateFermeture());
        dto.setActif(entity.isActif());
        if (entity.getClub() != null) dto.setClubId(entity.getClub().getId());
        return dto;
    }
}
