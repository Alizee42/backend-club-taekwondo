package club.taekwondo.entity.jpa;

import java.time.LocalDate;

import jakarta.persistence.*;

@Entity
@Access(AccessType.FIELD)
@Table(name = "membre")
public class Membre extends Utilisateur {

    @Column(nullable = false, unique = true)
    private String numeroLicence;

    private String ceinture;
    private LocalDate dateNaissance;
    private String adresse;
    private String statutSante;

    // Constructeur sans argument
    public Membre() {}

    // Getters et Setters
    public String getNumeroLicence() {
        return numeroLicence;
    }

    public void setNumeroLicence(String numeroLicence) {
        this.numeroLicence = numeroLicence;
    }

    public String getCeinture() {
        return ceinture;
    }

    public void setCeinture(String ceinture) {
        this.ceinture = ceinture;
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

    public String getStatutSante() {
        return statutSante;
    }

    public void setStatutSante(String statutSante) {
        this.statutSante = statutSante;
    }
}