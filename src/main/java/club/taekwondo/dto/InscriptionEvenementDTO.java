package club.taekwondo.dto;

import java.time.LocalDateTime;

import club.taekwondo.enums.StatutInscription;
import jakarta.validation.constraints.NotNull;

public class InscriptionEvenementDTO {

    private Long id;

    @NotNull(message = "L'ID de l'événement est obligatoire.")
    private Long evenementId;

    @NotNull(message = "L'ID du membre est obligatoire.")
    private Long membreId;   // ✅ au lieu de utilisateurId

    private LocalDateTime dateInscription;
    private StatutInscription statut;
    private Boolean presence;
    private String commentaire;

    // Champs supplémentaires pour l'affichage
    private String membreNom;
    private String membrePrenom;
    private String membreEmail;
    private String evenementTitre;

    // --- Constructeurs ---
    public InscriptionEvenementDTO() {}

    public InscriptionEvenementDTO(Long id, Long evenementId, Long membreId, LocalDateTime dateInscription,
                                   StatutInscription statut, Boolean presence, String commentaire,
                                   String membreNom, String membrePrenom, String evenementTitre) {
        this.id = id;
        this.evenementId = evenementId;
        this.membreId = membreId;
        this.dateInscription = dateInscription;
        this.statut = statut;
        this.presence = presence;
        this.commentaire = commentaire;
        this.membreNom = membreNom;
        this.membrePrenom = membrePrenom;
        this.evenementTitre = evenementTitre;
    }

    // --- Getters & Setters ---
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

    public Long getMembreId() {
        return membreId;
    }
    public void setMembreId(Long membreId) {
        this.membreId = membreId;
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

    public String getMembreNom() {
        return membreNom;
    }
    public void setMembreNom(String membreNom) {
        this.membreNom = membreNom;
    }

    public String getMembrePrenom() {
        return membrePrenom;
    }
    public void setMembrePrenom(String membrePrenom) {
        this.membrePrenom = membrePrenom;
    }

    public String getMembreEmail() {
        return membreEmail;
    }
    public void setMembreEmail(String membreEmail) {
        this.membreEmail = membreEmail;
    }

    public String getEvenementTitre() {
        return evenementTitre;
    }
    public void setEvenementTitre(String evenementTitre) {
        this.evenementTitre = evenementTitre;
    }
}
