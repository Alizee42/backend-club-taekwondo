package club.taekwondo.repository.jpa;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import club.taekwondo.entity.jpa.Echeance;

@Repository
public interface EcheanceRepository extends JpaRepository<Echeance, Long> {
	List<Echeance> findByPaiementIdOrderByNumeroAsc(Long paiementId);
	List<Echeance> findByStatutAndDateEcheanceBefore(String statut, LocalDate date);

}
