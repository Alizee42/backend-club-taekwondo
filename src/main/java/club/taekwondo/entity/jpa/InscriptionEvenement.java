package club.taekwondo.entity.jpa;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import club.taekwondo.enums.StatutInscription;

@Entity
@Access(AccessType.FIELD)
@Table(
    name = "inscription_evenement",
    uniqueConstraints = @UniqueConstraint(columnNames = {"membre_id", "evenement_id"}) // ✅ empêche les doublons pour un même enfant
)
public class InscriptionEvenement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ✅ On lie maintenant l’inscription à un Membre (et non plus à un Utilisateur)
    @ManyToOne
    @JoinColumn(name = "membre_id", referencedColumnName = "id", nullable = false)
    private Membre membre;

    @ManyToOne
    @JoinColumn(name = "evenement_id", referencedColumnName = "id", nullable = false)
    private Evenement evenement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutInscription statut = StatutInscription.EN_ATTENTE;

    @Column(name = "date_inscription", nullable = false)
    private LocalDateTime dateInscription;

    private Boolean presence;
    private String commentaire;

    // 🔹 Constructeur sans argument
    public InscriptionEvenement() {
        this.dateInscription = LocalDateTime.now();
    }

    // 🔹 Constructeur complet
    public InscriptionEvenement(Long id, Membre membre, Evenement evenement,
                                StatutInscription statut, LocalDateTime dateInscription,
                                Boolean presence, String commentaire) {
        this.id = id;
        this.membre = membre;
        this.evenement = evenement;
        this.statut = statut != null ? statut : StatutInscription.EN_ATTENTE;
        this.dateInscription = dateInscription != null ? dateInscription : LocalDateTime.now();
        this.presence = presence;
        this.commentaire = commentaire;
    }

    // --- Getters & Setters ---
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

    public StatutInscription getStatut() {
        return statut;
    }
    public void setStatut(StatutInscription statut) {
        this.statut = statut;
    }

    public LocalDateTime getDateInscription() {
        return dateInscription;
    }
    public void setDateInscription(LocalDateTime dateInscription) {
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
