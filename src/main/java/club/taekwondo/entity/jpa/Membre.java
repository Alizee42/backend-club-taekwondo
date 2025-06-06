package club.taekwondo.entity.jpa;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "membre")
@Access(AccessType.FIELD)
public class Membre extends Utilisateur {

    @Column(nullable = false, unique = true)
    private String numeroLicence;
    private String ceinture;



    // Constructeur sans argument
    public Membre() {
    	super();
    }

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

}

