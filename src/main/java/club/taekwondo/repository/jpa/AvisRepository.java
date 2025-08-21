package club.taekwondo.repository.jpa;

import club.taekwondo.entity.jpa.Avis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AvisRepository extends JpaRepository<Avis, Integer> {

    /* ======= LISTES ======= */
    List<Avis> findByApprouve(Boolean approuve);
    List<Avis> findByTypeAvisIgnoreCase(String typeAvis);
    List<Avis> findByApprouveAndTypeAvisIgnoreCase(Boolean approuve, String typeAvis);

    // Utile si tu veux récupérer les "Avis général" (type nul)
    List<Avis> findByTypeAvisIsNull();

    /* ======= COMPTEURS (badges) ======= */
    long countByApprouve(Boolean approuve);
    long countByTypeAvisIgnoreCase(String typeAvis);
    long countByApprouveAndTypeAvisIgnoreCase(Boolean approuve, String typeAvis);

    // Compter les "Avis général" (type nul)
    long countByTypeAvisIsNull();
}
