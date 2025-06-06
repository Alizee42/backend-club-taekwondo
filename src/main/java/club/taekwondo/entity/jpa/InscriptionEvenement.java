package club.taekwondo.entity.jpa;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Access(AccessType.FIELD)
@Table(name = "inscription_evenement")
public class InscriptionEvenement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Clé étrangère vers Membre
    @ManyToOne
    @JoinColumn(name = "utilisateur_id", referencedColumnName = "id", nullable = false)
    private Membre membre;

    // Clé étrangère vers Evenement
    @ManyToOne
    @JoinColumn(name = "evenement_id", referencedColumnName = "id", nullable = false)
    private Evenement evenement;

    @Column(nullable = false, columnDefinition = "ENUM('en_attente', 'validée', 'annulée') DEFAULT 'en_attente'")
    private String statut; // Statut de l'inscription

    @Column(name = "date_inscription", nullable = false)
    private LocalDate dateInscription; // Date de l'inscription

    @Column(nullable = true)
    private Boolean presence; // Indique si le membre était présent ou non

    @Column(nullable = true)
    private String commentaire; // Champ texte pour ajouter des commentaires

    // Constructeur sans argument
    public InscriptionEvenement() {
        // Initialiser la date d'inscription à la date actuelle
        this.dateInscription = LocalDate.now();
    }

    // Constructeur avec tous les arguments
    public InscriptionEvenement(Long id, Membre membre, Evenement evenement, String statut, LocalDate dateInscription, Boolean presence, String commentaire) {
        this.id = id;
        this.membre = membre;
        this.evenement = evenement;
        this.statut = statut;
        this.dateInscription = dateInscription;
        this.presence = presence;
        this.commentaire = commentaire;
    }

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Membre getMembre() {
        return membre;
    }

    public void setMembre(Membre membre) {
        this.membre = membre;
    }

    public Evenement getEvenement() {
        return evenement;
    }

    public void setEvenement(Evenement evenement) {
        this.evenement = evenement;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public LocalDate getDateInscription() {
        return dateInscription;
    }

    public void setDateInscription(LocalDate dateInscription) {
        this.dateInscription = dateInscription;
    }

    public Boolean getPresence() {
        return presence;
    }

    public void setPresence(Boolean presence) {
        this.presence = presence;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public void setCommentaire(String commentaire) {
        this.commentaire = commentaire;
    }
}