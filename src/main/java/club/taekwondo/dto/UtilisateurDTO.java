package club.taekwondo.dto;

import java.time.LocalDate;
import java.util.Set;
import club.taekwondo.enums.Role;
import com.fasterxml.jackson.annotation.JsonProperty;

public class UtilisateurDTO {

    private Long id;
    private String nom;
    private String prenom;
    private LocalDate dateNaissance;
    private String adresse;
    private String email;
    private String telephone;

    /**
     * Rôles de l'utilisateur : PARENT, MEMBRE, ADMIN
     */
    private Set<Role> roles;

    /**
     * Mot de passe non exposé en lecture (écriture uniquement)
     */
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

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}