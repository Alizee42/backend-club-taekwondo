package club.taekwondo.entity.jpa;

import java.time.LocalDate;

import jakarta.persistence.*;

@Entity
@Access(AccessType.FIELD)
@Table(name = "paiement")
public class Paiement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String type; // Cotisation ou Achat

    private Double montant;

    @Column(name = "date_paiement", nullable = false)
    private LocalDate datePaiement;

    private String statut; // payé / en attente / annulé

    @Column(name = "mode_paiement", nullable = false)
    private String modePaiement; // Espèces / Carte / Virement, etc.

    // Clé étrangère vers Membre
    @ManyToOne
    @JoinColumn(name = "membre_id", referencedColumnName = "id", nullable = false)
    private Membre membre;

    // Clé étrangère vers Commande (peut être null si c’est un paiement de cotisation)
    @ManyToOne
    @JoinColumn(name = "commande_id")
    private Commande commande;

    // Constructeur sans argument
    public Paiement() {
    }

    // Constructeur avec tous les arguments
    public Paiement(Long id, String type, Double montant, LocalDate datePaiement, String statut, String modePaiement, Membre membre, Commande commande) {
        this.id = id;
        this.type = type;
        this.montant = montant;
        this.datePaiement = datePaiement;
        this.statut = statut;
        this.modePaiement = modePaiement;
        this.membre = membre;
        this.commande = commande;
    }

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

    public Commande getCommande() {
        return commande;
    }

    public void setCommande(Commande commande) {
        this.commande = commande;
    }
}
