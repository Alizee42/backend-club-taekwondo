package club.taekwondo.entity.jpa;

import jakarta.persistence.*;
import java.time.LocalDate;
import club.taekwondo.enums.StatutInscription;

@Entity
@Access(AccessType.FIELD)
@Table(
    name = "inscription_evenement",
    uniqueConstraints = @UniqueConstraint(columnNames = {"utilisateur_id", "evenement_id"}) // empêche les doublons
)
public class InscriptionEvenement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "utilisateur_id", referencedColumnName = "id", nullable = false)
    private Utilisateur utilisateur;

    @ManyToOne
    @JoinColumn(name = "evenement_id", referencedColumnName = "id", nullable = false)
    private Evenement evenement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutInscription statut = StatutInscription.EN_ATTENTE;

    @Column(name = "date_inscription", nullable = false)
    private LocalDate dateInscription;

    @Column(nullable = true)
    private Boolean presence;

    @Column(nullable = true)
    private String commentaire;

    // 🔹 Constructeur sans argument
    public InscriptionEvenement() {
        this.dateInscription = LocalDate.now();
    }

    // 🔹 Constructeur complet
    public InscriptionEvenement(Long id, Utilisateur utilisateur, Evenement evenement,
                                 StatutInscription statut, LocalDate dateInscription,
                                 Boolean presence, String commentaire) {
        this.id = id;
        this.utilisateur = utilisateur;
        this.evenement = evenement;
        this.statut = statut;
        this.dateInscription = dateInscription;
        this.presence = presence;
        this.commentaire = commentaire;
    }

    // 🔹 Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Utilisateur getUtilisateur() {
        return utilisateur;
    }

    public void setUtilisateur(Utilisateur utilisateur) {
        this.utilisateur = utilisateur;
    }

    public Evenement getEvenement() {
        return evenement;
    }

    public void setEvenement(Evenement evenement) {
        this.evenement = evenement;
    }

    public StatutInscription getStatut() {
        return statut;
    }

    public void setStatut(StatutInscription statut) {
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

