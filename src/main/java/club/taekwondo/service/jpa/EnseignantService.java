package club.taekwondo.service.jpa;

import club.taekwondo.dto.EnseignantDTO;
import club.taekwondo.entity.jpa.Club;
import club.taekwondo.entity.jpa.Enseignant;
import club.taekwondo.enums.Role;
import club.taekwondo.repository.jpa.ClubRepository;
import club.taekwondo.repository.jpa.EnseignantRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EnseignantService {

    private final EnseignantRepository enseignantRepository;
    private final ClubRepository clubRepository;

    public EnseignantService(EnseignantRepository enseignantRepository, ClubRepository clubRepository) {
        this.enseignantRepository = enseignantRepository;
        this.clubRepository = clubRepository;
    }

    public List<EnseignantDTO> getByClub(Long clubId) {
        return enseignantRepository.findByClub_Id(clubId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public EnseignantDTO create(EnseignantDTO dto, String userRole, Long userClubId) {
        validateClub(dto.getClubId());
        enforceClubRestriction(userRole, userClubId, dto.getClubId());

        Enseignant entity = new Enseignant();
        apply(dto, entity);
        return toDTO(enseignantRepository.save(entity));
    }

    public Optional<EnseignantDTO> update(Long id, EnseignantDTO dto, String userRole, Long userClubId) {
        return enseignantRepository.findById(id).map(existing -> {
            Long targetClubId = dto.getClubId() != null ? dto.getClubId() :
                    (existing.getClub() != null ? existing.getClub().getId() : null);
            if (targetClubId == null) throw new IllegalArgumentException("ClubId requis");
            validateClub(targetClubId);
            enforceClubRestriction(userRole, userClubId, targetClubId);

            apply(dto, existing);
            return toDTO(enseignantRepository.save(existing));
        });
    }

    public boolean delete(Long id, String userRole, Long userClubId) {
        return enseignantRepository.findById(id).map(existing -> {
            Long targetClubId = existing.getClub() != null ? existing.getClub().getId() : null;
            if (targetClubId == null) throw new IllegalStateException("Enseignant sans club");
            enforceClubRestriction(userRole, userClubId, targetClubId);
            enseignantRepository.deleteById(id);
            return true;
        }).orElse(false);
    }

    private void validateClub(Long clubId) {
        if (clubId == null || !clubRepository.existsById(clubId)) {
            throw new IllegalArgumentException("Club introuvable: " + clubId);
        }
    }

    private void enforceClubRestriction(String userRole, Long userClubId, Long targetClubId) {
        if (userRole == null) return; // fallback: la sécurité HTTP doit déjà filtrer
        try {
            Role role = Role.valueOf(userRole.toUpperCase());
            if (role == Role.ADMIN) {
                if (userClubId == null || !userClubId.equals(targetClubId)) {
                    throw new SecurityException("ADMIN ne peut gérer que son propre club");
                }
            }
        } catch (IllegalArgumentException ignored) { /* rôle inconnu -> laisser la couche HTTP décider */ }
    }

    private void apply(EnseignantDTO dto, Enseignant entity) {
        if (dto.getClubId() != null) {
            Club club = clubRepository.findById(dto.getClubId()).orElseThrow(() -> new IllegalArgumentException("Club introuvable"));
            entity.setClub(club);
        }
        if (dto.getNom() != null) entity.setNom(dto.getNom());
        if (dto.getPrenom() != null) entity.setPrenom(dto.getPrenom());
        entity.setSpecialite(dto.getSpecialite());
        entity.setDescription(dto.getDescription());
        entity.setPhotoUrl(dto.getPhotoUrl());
        entity.setFacebook(dto.getFacebook());
        entity.setInstagram(dto.getInstagram());
        entity.setLinkedin(dto.getLinkedin());
    }

    private EnseignantDTO toDTO(Enseignant e) {
        EnseignantDTO dto = new EnseignantDTO();
        dto.setId(e.getId());
        dto.setClubId(e.getClub() != null ? e.getClub().getId() : null);
        dto.setNom(e.getNom());
        dto.setPrenom(e.getPrenom());
        dto.setSpecialite(e.getSpecialite());
        dto.setDescription(e.getDescription());
        dto.setPhotoUrl(e.getPhotoUrl());
        dto.setFacebook(e.getFacebook());
        dto.setInstagram(e.getInstagram());
        dto.setLinkedin(e.getLinkedin());
        return dto;
    }
}
