package club.taekwondo.entity.jpa;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Access(AccessType.FIELD)
@Table(name = "professeur")
public class Professeur extends Membre {

    private String specialite;
    private String description;

    // Constructeur vide
    public Professeur() {}

    // Getters et Setters
    public String getSpecialite() {
        return specialite;
    }

    public void setSpecialite(String specialite) {
        this.specialite = specialite;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
