package club.taekwondo.dto;

import java.util.List;

/**
 * DTO utilisé pour inscrire un parent et ses enfants à un événement.
 * - parentId : identifiant du parent connecté (optionnel si tu relies via JWT)
 * - evenementId : identifiant de l'événement
 * - enfantsIds : liste des IDs des enfants à inscrire
 */
public class InscriptionRequestDTO {

    private Long parentId;        // le parent connecté
    private Long evenementId;     // l'événement ciblé
    private List<Long> enfantsIds;
    private String commentaire; // les enfants à inscrire

    // --- Getters & Setters ---

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public Long getEvenementId() {
        return evenementId;
    }

    public void setEvenementId(Long evenementId) {
        this.evenementId = evenementId;
    }

    public List<Long> getEnfantsIds() {
        return enfantsIds;
    }

    public void setEnfantsIds(List<Long> enfantsIds) {
        this.enfantsIds = enfantsIds;
    }

	public String getCommentaire() {
		return commentaire;
	}

	public void setCommentaire(String commentaire) {
		this.commentaire = commentaire;
	}
    
}
