package club.taekwondo.repository.jpa;

import club.taekwondo.entity.jpa.LigneCommande;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LigneCommandeRepository extends JpaRepository<LigneCommande, Long> {
	List<LigneCommande> findByCommandeId(Long commandeId);

	// Filtrage par club
	@org.springframework.data.jpa.repository.Query("SELECT lc FROM LigneCommande lc WHERE lc.commande.club.id = :clubId")
	List<LigneCommande> findByClubId(Long clubId);

}