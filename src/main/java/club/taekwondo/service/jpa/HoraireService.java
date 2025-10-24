package club.taekwondo.service.jpa;

import club.taekwondo.entity.jpa.Horaire;
import club.taekwondo.repository.jpa.HoraireRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class HoraireService {
    @Autowired
    private HoraireRepository horaireRepository;

    public Horaire updateHoraire(Horaire horaire) {
        return horaireRepository.save(horaire);
    }

    public List<Horaire> getAllHoraires() {
        return horaireRepository.findAll();
    }

    public List<Horaire> getHorairesByClub(Long clubId) {
        return horaireRepository.findByClubId(clubId);
    }

    public Horaire addHoraire(Horaire horaire) {
        return horaireRepository.save(horaire);
    }

    public void deleteHoraire(Long id) {
        horaireRepository.deleteById(id);
    }
}