package club.taekwondo.entity.jpa;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class Paiement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String type = "Cotisation"; // Valeur par défaut

    private Double montant;

    @Column(name = "date_paiement", nullable = false)
    private LocalDate datePaiement;

    private String statut; // payé / en attente / annulé

    @Column(name = "mode_paiement", nullable = false)
    private String modePaiement; // Espèces / Carte / Virement, etc.

    @ManyToOne
    @JoinColumn(name = "membre_id", nullable = false)
    @JsonBackReference // Empêche la récursion infinie
    private Membre membre;

    @ManyToOne
    @JoinColumn(name = "utilisateur_id", nullable = false) // Relation avec Utilisateur
    private Utilisateur utilisateur;

    
    public Paiement() {}

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public String getModePaiement() {
        return modePaiement;
    }

    public void setModePaiement(String modePaiement) {
        this.modePaiement = modePaiement;
    }

    public Membre getMembre() {
        return membre;
    }

    public void setMembre(Membre membre) {
        this.membre = membre;
    }
    public Utilisateur getUtilisateur() {
        return utilisateur;
    }

    public void setUtilisateur(Utilisateur utilisateur) {
        this.utilisateur = utilisateur;
    }
}