package club.taekwondo.repository.jpa;

import club.taekwondo.entity.jpa.LigneCommande;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LigneCommandeRepository extends JpaRepository<LigneCommande, Long> {
	@Query("""
		SELECT lc FROM LigneCommande lc
		LEFT JOIN FETCH lc.produit
		LEFT JOIN FETCH lc.beneficiaire
		WHERE lc.commande.id = :commandeId
		""")
	List<LigneCommande> findByCommandeId(@Param("commandeId") Long commandeId);

	// Filtrage par club
	@Query("SELECT lc FROM LigneCommande lc WHERE lc.commande.club.id = :clubId")
	List<LigneCommande> findByClubId(Long clubId);

}
