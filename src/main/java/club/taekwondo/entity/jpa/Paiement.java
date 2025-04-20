package club.taekwondo.entity.jpa;

import java.time.LocalDate;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Access(AccessType.FIELD)
@Table(name = "paiement")
public class Paiement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double montant;

    @Column(name = "date_paiement")
    private LocalDate datePaiement;

    private String statut;

    // Clé étrangère vers Membre
    @ManyToOne
    @JoinColumn(name = "membre_id", referencedColumnName = "id")
    private Membre membre;

    // Constructeur sans argument
    public Paiement() {
    }

    // Constructeur avec arguments
    public Paiement(Long id, Double montant, LocalDate datePaiement, String statut, Membre membre) {
        this.id = id;
        this.montant = montant;
        this.datePaiement = datePaiement;
        this.statut = statut;
        this.membre = membre;
    }

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Double getMontant() {
        return montant;
    }

    public void setMontant(Double montant) {
        this.montant = montant;
    }

    public LocalDate getDatePaiement() {
        return datePaiement;
    }

    public void setDatePaiement(LocalDate datePaiement) {
        this.datePaiement = datePaiement;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public Membre getMembre() {
        return membre;
    }

    public void setMembre(Membre membre) {
        this.membre = membre;
    }
}

