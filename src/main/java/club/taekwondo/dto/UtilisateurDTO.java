package club.taekwondo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import club.taekwondo.enums.Role;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public class UtilisateurDTO {
    private boolean passwordTemporaire = false;
    public boolean isPasswordTemporaire() {
        return passwordTemporaire;
    }

    public void setPasswordTemporaire(boolean passwordTemporaire) {
        this.passwordTemporaire = passwordTemporaire;
    }

    private Long id;
    private String nom;
    private String prenom;
    private LocalDate dateNaissance;
    private String adresse;
    private String email;
    private String telephone;
    private Role role;
    private Long clubId;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private OffsetDateTime passwordUpdatedAt;
    
    // Informations supplémentaires pour les parents
    private String nomParent;
    private String prenomParent;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

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
    public String getAdresse() {
        return adresse;
    }
    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getTelephone() {
        return telephone;
    }
    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }
    public Long getClubId() {
        return clubId;
    }
    public void setClubId(Long clubId) {
        this.clubId = clubId;
    }
    public Role getRole() {
        return role;
    }
    @JsonProperty("role")
    public void setRole(String role) {
        if (role != null) {
            this.role = Role.valueOf(role.toUpperCase());
        }
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
    public OffsetDateTime getPasswordUpdatedAt() {
        return passwordUpdatedAt;
    }

    public void setPasswordUpdatedAt(OffsetDateTime passwordUpdatedAt) {
        this.passwordUpdatedAt = passwordUpdatedAt;
    }
    
    public String getNomParent() {
        return nomParent;
    }
    
    public void setNomParent(String nomParent) {
        this.nomParent = nomParent;
    }
    
    public String getPrenomParent() {
        return prenomParent;
    }
    
    public void setPrenomParent(String prenomParent) {
        this.prenomParent = prenomParent;
    }
}
