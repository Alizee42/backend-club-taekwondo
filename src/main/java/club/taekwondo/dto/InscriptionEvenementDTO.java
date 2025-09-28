package club.taekwondo.dto;

import java.time.LocalDateTime;

import club.taekwondo.enums.StatutInscription;
import jakarta.validation.constraints.NotNull;

public class InscriptionEvenementDTO {

    private Long id;

    @NotNull(message = "L'ID de l'événement est obligatoire.")
    private Long evenementId;

    @NotNull(message = "L'ID de l'utilisateur est obligatoire.")
    private Long utilisateurId;

    private LocalDateTime dateInscription;  // ✅ maintenant cohérent avec l’entité
    private StatutInscription statut;
    private Boolean presence;
    private String commentaire;

    // Champs supplémentaires pour l'affichage
    private String utilisateurNom;
    private String utilisateurPrenom;
    private String utilisateurEmail;
    private String evenementTitre;

    // Constructeurs
    public InscriptionEvenementDTO() {}

    public InscriptionEvenementDTO(Long id, Long evenementId, Long utilisateurId, LocalDateTime dateInscription,
                                    StatutInscription statut, Boolean presence, String commentaire,
                                    String utilisateurNom, String utilisateurPrenom, String utilisateurEmail,
                                    String evenementTitre) {
        this.id = id;
        this.evenementId = evenementId;
        this.utilisateurId = utilisateurId;
        this.dateInscription = dateInscription;
        this.statut = statut;
        this.presence = presence;
        this.commentaire = commentaire;
        this.utilisateurNom = utilisateurNom;
        this.utilisateurPrenom = utilisateurPrenom;
        this.utilisateurEmail = utilisateurEmail;
        this.evenementTitre = evenementTitre;
    }

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEvenementId() {
        return evenementId;
    }

    public void setEvenementId(Long evenementId) {
        this.evenementId = evenementId;
    }

    public Long getUtilisateurId() {
        return utilisateurId;
    }

    public void setUtilisateurId(Long utilisateurId) {
        this.utilisateurId = utilisateurId;
    }

    public LocalDateTime getDateInscription() {
        return dateInscription;
    }

    public void setDateInscription(LocalDateTime dateInscription) {
        this.dateInscription = dateInscription;
    }

    public StatutInscription getStatut() {
        return statut;
    }

    public void setStatut(StatutInscription statut) {
        this.statut = statut;
    }

    public Boolean getPresence() {
        return presence;
    }

    public void setPresence(Boolean presence) {
        this.presence = presence;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public void setCommentaire(String commentaire) {
        this.commentaire = commentaire;
    }

    public String getUtilisateurNom() {
        return utilisateurNom;
    }

    public void setUtilisateurNom(String utilisateurNom) {
        this.utilisateurNom = utilisateurNom;
    }

    public String getUtilisateurPrenom() {
        return utilisateurPrenom;
    }

    public void setUtilisateurPrenom(String utilisateurPrenom) {
        this.utilisateurPrenom = utilisateurPrenom;
    }

    public String getUtilisateurEmail() {
        return utilisateurEmail;
    }

    public void setUtilisateurEmail(String utilisateurEmail) {
        this.utilisateurEmail = utilisateurEmail;
    }

    public String getEvenementTitre() {
        return evenementTitre;
    }

    public void setEvenementTitre(String evenementTitre) {
        this.evenementTitre = evenementTitre;
    }
}
