package club.taekwondo.dto;

import java.time.LocalDate;

public class MembreDTO {

    private Long id;
    private String nom;
    private String prenom;
    private LocalDate dateNaissance;
    private String ceinture;
    private String numeroLicence;
    private Long utilisateurId;
    private boolean estAdulte;
    private Long clubId;
    private Long parentId;
    private String nomParent;
    private String prenomParent;
    private String genre;
    public Long getClubId() {
        return clubId;
    }
    public void setClubId(Long clubId) {
        this.clubId = clubId;
    }

    // --- Getters & Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public LocalDate getDateNaissance() {
        return dateNaissance;
    }

    public void setDateNaissance(LocalDate dateNaissance) {
        this.dateNaissance = dateNaissance;
    }

    public String getCeinture() {
        return ceinture;
    }

    public void setCeinture(String ceinture) {
        this.ceinture = ceinture;
    }

    public String getNumeroLicence() {
        return numeroLicence;
    }

    public void setNumeroLicence(String numeroLicence) {
        this.numeroLicence = numeroLicence;
    }

    public Long getUtilisateurId() {
        return utilisateurId;
    }

    public void setUtilisateurId(Long utilisateurId) {
        this.utilisateurId = utilisateurId;
    }

    public boolean isEstAdulte() {
        return estAdulte;
    }

    public void setEstAdulte(boolean estAdulte) {
        this.estAdulte = estAdulte;
    }

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }

    public String getNomParent() { return nomParent; }
    public void setNomParent(String nomParent) { this.nomParent = nomParent; }

    public String getPrenomParent() { return prenomParent; }
    public void setPrenomParent(String prenomParent) { this.prenomParent = prenomParent; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }
}

