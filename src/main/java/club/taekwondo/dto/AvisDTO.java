package club.taekwondo.dto;

import java.time.LocalDate;

public class AvisDTO {
    private Integer id;
    private LocalDate datePub;
    private String contenu;
    private Boolean approuve;
    private String pseudoVisiteur;
    private Integer note;
    private String typeAvis;
    private String photo;
    private Long utilisateurId; // Expose juste l'ID utilisateur pour garder la structure légère

    // Getters & Setters

    public Integer getId() {
        return id;
    }
    public void setId(Integer id) {
        this.id = id;
    }

    public LocalDate getDatePub() {
        return datePub;
    }
    public void setDatePub(LocalDate datePub) {
        this.datePub = datePub;
    }

    public String getContenu() {
        return contenu;
    }
    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public Boolean getApprouve() {
        return approuve;
    }
    public void setApprouve(Boolean approuve) {
        this.approuve = approuve;
    }

    public String getPseudoVisiteur() {
        return pseudoVisiteur;
    }
    public void setPseudoVisiteur(String pseudoVisiteur) {
        this.pseudoVisiteur = pseudoVisiteur;
    }

    public Integer getNote() {
        return note;
    }
    public void setNote(Integer note) {
        this.note = note;
    }

    public String getTypeAvis() {
        return typeAvis;
    }
    public void setTypeAvis(String typeAvis) {
        this.typeAvis = typeAvis;
    }

    public String getPhoto() {
        return photo;
    }
    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public Long getUtilisateurId() {
        return utilisateurId;
    }
    public void setUtilisateurId(Long utilisateurId) {
        this.utilisateurId = utilisateurId;
    }
}
