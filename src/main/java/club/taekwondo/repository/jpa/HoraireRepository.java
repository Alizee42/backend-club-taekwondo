package club.taekwondo.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import club.taekwondo.entity.jpa.Horaire;

import java.util.List;

public interface HoraireRepository extends JpaRepository<Horaire, Long> {
    List<Horaire> findByClubId(Long clubId);
}
