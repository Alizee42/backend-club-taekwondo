package club.taekwondo.service.jpa;

import club.taekwondo.dto.ClubDto;
import club.taekwondo.entity.jpa.Club;
import club.taekwondo.repository.jpa.ClubRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ClubService {
    @Autowired
    private ClubRepository clubRepository;

    public List<ClubDto> getAllClubs() {
        return clubRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    public ClubDto getClubById(Long id) {
        Optional<Club> club = clubRepository.findById(id);
        return club.map(this::toDto).orElse(null);
    }

    public ClubDto createClub(ClubDto dto) {
        Club club = toEntity(dto);
        club.setId(null);
        Club saved = clubRepository.save(club);
        return toDto(saved);
    }

    public ClubDto updateClub(Long id, ClubDto dto) {
        Optional<Club> clubOpt = clubRepository.findById(id);
        if (clubOpt.isEmpty()) return null;
        Club club = clubOpt.get();
        applyDto(club, dto);
        Club saved = clubRepository.save(club);
        return toDto(saved);
    }

    public void deleteClub(Long id) {
        clubRepository.deleteById(id);
    }

    private ClubDto toDto(Club club) {
        ClubDto dto = new ClubDto();
        dto.setId(club.getId());
        dto.setName(club.getName());
        dto.setAdresse(club.getAdresse());
        dto.setTelephone(club.getTelephone());
        dto.setEmail(club.getEmail());
        dto.setLogo(club.getLogo());
        dto.setRib(club.getRib());
        dto.setStripeAccountId(club.getStripeAccountId());
        dto.setStripeChargesEnabled(club.isStripeChargesEnabled());
        return dto;
    }

    private Club toEntity(ClubDto dto) {
        Club club = new Club();
        applyDto(club, dto);
        return club;
    }

    private void applyDto(Club club, ClubDto dto) {
        club.setName(clean(dto.getName()));
        club.setAdresse(clean(dto.getAdresse()));
        club.setTelephone(clean(dto.getTelephone()));
        club.setEmail(clean(dto.getEmail()));
        club.setLogo(clean(dto.getLogo()));
        club.setRib(clean(dto.getRib()));
    }

    private String clean(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
