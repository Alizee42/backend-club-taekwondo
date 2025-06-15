package club.taekwondo.entity.jpa;

import java.time.LocalDateTime;
import java.time.LocalTime;

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

    private LocalTime horaire; // Horaire du cours
    private String niveau; // Niveau requis pour le cours
    private Integer capacite; // Nombre maximum de participants

    // Clé étrangère vers Utilisateur (professeur)
    @ManyToOne
    @JoinColumn(name = "utilisateur_id", referencedColumnName = "id")
    private Utilisateur professeur; //

    // Constructeur par défaut
    public Cours() {}

    // Constructeur avec paramètres
    public Cours(Long id, String nomCours, String description, LocalDateTime date, Integer duree, LocalTime horaire, String niveau, Integer capacite, Utilisateur professeur) {
        this.id = id;
        this.nomCours = nomCours;
        this.description = description;
        this.date = date;
        this.duree = duree;
        this.horaire = horaire;
        this.niveau = niveau;
        this.capacite = capacite;
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

    public LocalTime getHoraire() {
        return horaire;
    }

    public void setHoraire(LocalTime horaire) {
        this.horaire = horaire;
    }

    public String getNiveau() {
        return niveau;
    }

    public void setNiveau(String niveau) {
        this.niveau = niveau;
    }

    public Integer getCapacite() {
        return capacite;
    }

    public void setCapacite(Integer capacite) {
        this.capacite = capacite;
    }

    public Utilisateur getProfesseur() {
        return professeur;
    }

    public void setProfesseur(Utilisateur professeur) {
        this.professeur = professeur;
    }
}