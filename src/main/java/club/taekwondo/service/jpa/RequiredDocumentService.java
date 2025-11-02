package club.taekwondo.service.jpa;

import club.taekwondo.dto.RequiredDocumentDTO;
import club.taekwondo.entity.jpa.Club;
import club.taekwondo.entity.jpa.RequiredDocument;
import club.taekwondo.repository.jpa.ClubRepository;
import club.taekwondo.repository.jpa.RequiredDocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RequiredDocumentService {

    @Autowired private RequiredDocumentRepository repo;
    @Autowired private ClubRepository clubRepo;

    public List<RequiredDocumentDTO> getByClub(Long clubId) {
        validateClubId(clubId);
        return repo.findByClub_IdOrderByOrderIndexAsc(clubId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public RequiredDocumentDTO create(RequiredDocumentDTO dto) {
        validate(dto);
        RequiredDocument entity = toEntity(dto);
        // avoid duplicates for same (club, code)
        Optional<RequiredDocument> existing = repo.findByClub_IdAndCode(dto.getClubId(), dto.getCode());
        if (existing.isPresent()) {
            RequiredDocument e = existing.get();
            e.setLabel(entity.getLabel());
            e.setRequired(entity.isRequired());
            e.setActive(entity.isActive());
            e.setOrderIndex(entity.getOrderIndex());
            return toDTO(repo.save(e));
        }
        return toDTO(repo.save(entity));
    }

    public RequiredDocumentDTO update(Long id, RequiredDocumentDTO dto) {
        if (id == null || id <= 0) throw new IllegalArgumentException("ID invalide");
        RequiredDocument e = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Introuvable"));
        if (dto.getCode() != null) e.setCode(dto.getCode());
        if (dto.getLabel() != null) e.setLabel(dto.getLabel());
        if (dto.getRequired() != null) e.setRequired(dto.getRequired());
        if (dto.getActive() != null) e.setActive(dto.getActive());
        if (dto.getOrderIndex() != null) e.setOrderIndex(dto.getOrderIndex());
        return toDTO(repo.save(e));
    }

    public void delete(Long id) {
        if (id == null || id <= 0) throw new IllegalArgumentException("ID invalide");
        repo.deleteById(id);
    }

    private void validate(RequiredDocumentDTO dto) {
        if (dto == null) throw new IllegalArgumentException("DTO requis");
        validateClubId(dto.getClubId());
        if (dto.getCode() == null || dto.getCode().isBlank()) throw new IllegalArgumentException("Code requis");
        if (dto.getLabel() == null || dto.getLabel().isBlank()) throw new IllegalArgumentException("Label requis");
        if (dto.getRequired() == null) dto.setRequired(true);
        if (dto.getActive() == null) dto.setActive(true);
    }

    private void validateClubId(Long clubId) {
        if (clubId == null || clubId <= 0) throw new IllegalArgumentException("clubId invalide");
        if (!clubRepo.existsById(clubId)) throw new IllegalArgumentException("Club introuvable: " + clubId);
    }

    private RequiredDocumentDTO toDTO(RequiredDocument e) {
        RequiredDocumentDTO dto = new RequiredDocumentDTO();
        dto.setId(e.getId());
        dto.setClubId(e.getClub() != null ? e.getClub().getId() : null);
        dto.setCode(e.getCode());
        dto.setLabel(e.getLabel());
        dto.setRequired(e.isRequired());
        dto.setActive(e.isActive());
        dto.setOrderIndex(e.getOrderIndex());
        return dto;
    }

    private RequiredDocument toEntity(RequiredDocumentDTO dto) {
        RequiredDocument e = new RequiredDocument();
        e.setId(dto.getId());
        Club club = clubRepo.findById(dto.getClubId()).orElseThrow(() -> new IllegalArgumentException("Club introuvable"));
        e.setClub(club);
        e.setCode(dto.getCode());
        e.setLabel(dto.getLabel());
        e.setRequired(Boolean.TRUE.equals(dto.getRequired()));
        e.setActive(Boolean.TRUE.equals(dto.getActive()));
        e.setOrderIndex(dto.getOrderIndex());
        return e;
    }
}
