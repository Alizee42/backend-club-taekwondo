package club.taekwondo.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import club.taekwondo.entity.jpa.Echeance;

@Repository
public interface EcheanceRepository extends JpaRepository<Echeance, Long> {
}
