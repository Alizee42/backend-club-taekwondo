package club.taekwondo.entity.jpa;

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
@Table(name = "inscription_evenement")
public class InscriptionEvenement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Clé étrangère vers Membre
    @ManyToOne
    @JoinColumn(name = "membre_id", referencedColumnName = "id")
    private Membre membre;

    // Clé étrangère vers Evenement
    @ManyToOne
    @JoinColumn(name = "evenement_id", referencedColumnName = "id")
    private Evenement evenement;

    private String statut; // Statut de l'inscription

    // Constructeur sans argument
    public InscriptionEvenement() {
    }

    // Constructeur avec tous les arguments
    public InscriptionEvenement(Long id, Membre membre, Evenement evenement, String statut) {
        this.id = id;
        this.membre = membre;
        this.evenement = evenement;
        this.statut = statut;
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
}

