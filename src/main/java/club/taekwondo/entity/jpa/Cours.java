package club.taekwondo.entity.jpa;


import java.time.LocalDateTime;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Access(AccessType.FIELD)
@Table(name = "cours")
public class Cours {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nomCours;
    private String description;
    private LocalDateTime date;
    private Integer duree; // durée en minutes

    // Clé étrangère vers Professeur
    @ManyToOne
    @JoinColumn(name = "professeur_id", referencedColumnName = "id")
    private Professeur professeur;

    // Constructeur par défaut
    public Cours() {}

    // Constructeur avec paramètres
    public Cours(Long id, String nomCours, String description, LocalDateTime date, Integer duree, Professeur professeur) {
        this.id = id;
        this.nomCours = nomCours;
        this.description = description;
        this.date = date;
        this.duree = duree;
        this.professeur = professeur;
    }

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNomCours() {
        return nomCours;
    }

    public void setNomCours(String nomCours) {
        this.nomCours = nomCours;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public Integer getDuree() {
        return duree;
    }

    public void setDuree(Integer duree) {
        this.duree = duree;
    }

    public Professeur getProfesseur() {
        return professeur;
    }

    public void setProfesseur(Professeur professeur) {
        this.professeur = professeur;
    }
}
