package club.taekwondo.service.jpa;

import club.taekwondo.dto.MentionsLegalesConfigDto;
import club.taekwondo.entity.jpa.MentionsLegalesConfig;
import club.taekwondo.repository.jpa.ClubRepository;
import club.taekwondo.repository.jpa.MentionsLegalesConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MentionsLegalesConfigService {

    @Autowired
    private MentionsLegalesConfigRepository repo;

    @Autowired
    private ClubRepository clubRepository;

    public MentionsLegalesConfigDto get(Long clubId) {
        return toDto(repo.findByClub_Id(clubId).orElseGet(MentionsLegalesConfig::new));
    }

    public MentionsLegalesConfigDto update(Long clubId, MentionsLegalesConfigDto dto) {
        MentionsLegalesConfig config = repo.findByClub_Id(clubId).orElseGet(() -> {
            MentionsLegalesConfig c = new MentionsLegalesConfig();
            clubRepository.findById(clubId).ifPresent(c::setClub);
            return c;
        });
        applyDto(config, dto);
        return toDto(repo.save(config));
    }

    private MentionsLegalesConfigDto toDto(MentionsLegalesConfig c) {
        MentionsLegalesConfigDto dto = new MentionsLegalesConfigDto();
        dto.setClubId(c.getClub() != null ? c.getClub().getId() : null);
        dto.setNomAssociation(c.getNomAssociation());
        dto.setStatutJuridique(c.getStatutJuridique());
        dto.setAdresse(c.getAdresse());
        dto.setNumeroRna(c.getNumeroRna());
        dto.setNumeroSiren(c.getNumeroSiren());
        dto.setRepresentantLegal(c.getRepresentantLegal());
        dto.setEmail(c.getEmail());
        dto.setTelephone(c.getTelephone());
        dto.setHebergeurNom(c.getHebergeurNom());
        dto.setHebergeurAdresse(c.getHebergeurAdresse());
        dto.setHebergeurSiteWeb(c.getHebergeurSiteWeb());
        dto.setMediateurNom(c.getMediateurNom());
        dto.setMediateurContact(c.getMediateurContact());
        return dto;
    }

    private void applyDto(MentionsLegalesConfig c, MentionsLegalesConfigDto dto) {
        if (dto.getNomAssociation() != null) c.setNomAssociation(dto.getNomAssociation());
        if (dto.getStatutJuridique() != null) c.setStatutJuridique(dto.getStatutJuridique());
        if (dto.getAdresse() != null) c.setAdresse(dto.getAdresse());
        if (dto.getNumeroRna() != null) c.setNumeroRna(dto.getNumeroRna());
        if (dto.getNumeroSiren() != null) c.setNumeroSiren(dto.getNumeroSiren());
        if (dto.getRepresentantLegal() != null) c.setRepresentantLegal(dto.getRepresentantLegal());
        if (dto.getEmail() != null) c.setEmail(dto.getEmail());
        if (dto.getTelephone() != null) c.setTelephone(dto.getTelephone());
        if (dto.getHebergeurNom() != null) c.setHebergeurNom(dto.getHebergeurNom());
        if (dto.getHebergeurAdresse() != null) c.setHebergeurAdresse(dto.getHebergeurAdresse());
        if (dto.getHebergeurSiteWeb() != null) c.setHebergeurSiteWeb(dto.getHebergeurSiteWeb());
        if (dto.getMediateurNom() != null) c.setMediateurNom(dto.getMediateurNom());
        if (dto.getMediateurContact() != null) c.setMediateurContact(dto.getMediateurContact());
    }
}
