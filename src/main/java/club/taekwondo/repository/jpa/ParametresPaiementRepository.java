package club.taekwondo.repository.jpa;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import club.taekwondo.entity.jpa.ParametresPaiement;

@Repository
public interface ParametresPaiementRepository extends JpaRepository<ParametresPaiement, Long> {
}