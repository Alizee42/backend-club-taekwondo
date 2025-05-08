package club.taekwondo.entity.jpa;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Access(AccessType.FIELD)
@Table(name = "evenement")
public class Evenement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titre; // Titre de l'événement

    @Column(nullable = false)
    private LocalDateTime dateDebut;

    @Column(nullable = false)
    private LocalDateTime dateFin;

    @Column(nullable = false)
    private String lieu;

    @Column(nullable = false)
    private Integer capacite; // Capacité maximale de l'événement

    @Column(nullable = true)
    private String description;

    // Relation avec Utilisateur (organisateur)
    @ManyToOne
    @JoinColumn(name = "organisateur_id", nullable = false)
    private Utilisateur organisateur;

    // Constructeur sans argument
    public Evenement() {
    }

    // Constructeur avec tous les arguments
    public Evenement(Long id, String titre, LocalDateTime dateDebut, LocalDateTime dateFin, String lieu, Integer capacite, String description, Utilisateur organisateur) {
        this.id = id;
        this.titre = titre;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.lieu = lieu;
        this.capacite = capacite;
        this.description = description;
        this.organisateur = organisateur;
    }

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public LocalDateTime getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDateTime dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDateTime getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDateTime dateFin) {
        this.dateFin = dateFin;
    }

    public String getLieu() {
        return lieu;
    }

    public void setLieu(String lieu) {
        this.lieu = lieu;
    }

    public Integer getCapacite() {
        return capacite;
    }

    public void setCapacite(Integer capacite) {
        this.capacite = capacite;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Utilisateur getOrganisateur() {
        return organisateur;
    }

    public void setOrganisateur(Utilisateur organisateur) {
        this.organisateur = organisateur;
    }
}