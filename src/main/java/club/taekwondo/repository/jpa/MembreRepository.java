package club.taekwondo.repository.jpa;

import club.taekwondo.entity.jpa.Membre;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MembreRepository extends JpaRepository<Membre, Long> {

    // 🔹 Trouver un membre par email du compte utilisateur lié
    Optional<Membre> findByCompteUtilisateur_Email(String email);

    // 🔹 Trouver tous les enfants d’un parent (via ID parent)
    List<Membre> findByParentId(Long parentId);

    // 🔹 Trouver un membre par ID du compte utilisateur lié
    Optional<Membre> findByCompteUtilisateur_Id(Long utilisateurId);

    // =====================
    // 📊 Méthodes KPI
    // =====================

    // 🔹 Nombre total de membres
    long count();

    // 🔹 Nombre de membres adultes (ou actifs si c’est le sens que tu veux donner)
    long countByEstAdulteTrue();
}
