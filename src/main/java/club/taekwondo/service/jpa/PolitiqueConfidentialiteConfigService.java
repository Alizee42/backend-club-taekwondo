package club.taekwondo.service.jpa;

import club.taekwondo.dto.PolitiqueConfidentialiteConfigDto;
import club.taekwondo.entity.jpa.PolitiqueConfidentialiteConfig;
import club.taekwondo.repository.jpa.ClubRepository;
import club.taekwondo.repository.jpa.PolitiqueConfidentialiteConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PolitiqueConfidentialiteConfigService {

    @Autowired
    private PolitiqueConfidentialiteConfigRepository repo;

    @Autowired
    private ClubRepository clubRepository;

    public PolitiqueConfidentialiteConfigDto get(Long clubId) {
        return toDto(repo.findByClub_Id(clubId).orElseGet(PolitiqueConfidentialiteConfig::new));
    }

    public PolitiqueConfidentialiteConfigDto update(Long clubId, PolitiqueConfidentialiteConfigDto dto) {
        PolitiqueConfidentialiteConfig config = repo.findByClub_Id(clubId).orElseGet(() -> {
            PolitiqueConfidentialiteConfig c = new PolitiqueConfidentialiteConfig();
            clubRepository.findById(clubId).ifPresent(c::setClub);
            return c;
        });
        applyDto(config, dto);
        return toDto(repo.save(config));
    }

    private PolitiqueConfidentialiteConfigDto toDto(PolitiqueConfidentialiteConfig c) {
        PolitiqueConfidentialiteConfigDto dto = new PolitiqueConfidentialiteConfigDto();
        dto.setClubId(c.getClub() != null ? c.getClub().getId() : null);
        dto.setNomAssociation(c.getNomAssociation());
        dto.setAdresse(c.getAdresse());
        dto.setEmailContact(c.getEmailContact());
        dto.setEmailRgpd(c.getEmailRgpd());
        dto.setRepresentantLegal(c.getRepresentantLegal());
        return dto;
    }

    private void applyDto(PolitiqueConfidentialiteConfig c, PolitiqueConfidentialiteConfigDto dto) {
        if (dto.getNomAssociation() != null) c.setNomAssociation(dto.getNomAssociation());
        if (dto.getAdresse() != null) c.setAdresse(dto.getAdresse());
        if (dto.getEmailContact() != null) c.setEmailContact(dto.getEmailContact());
        if (dto.getEmailRgpd() != null) c.setEmailRgpd(dto.getEmailRgpd());
        if (dto.getRepresentantLegal() != null) c.setRepresentantLegal(dto.getRepresentantLegal());
    }
}
