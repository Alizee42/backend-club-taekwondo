package club.taekwondo.entity.jpa;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Access(AccessType.FIELD)
@Table(name = "commande")
public class Commande {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date_commande", nullable = false)
    private LocalDate dateCommande;

    @Column(nullable = false, length = 50)
    private String statut;

    @Column(name = "montant_total", precision = 10, scale = 2)
    private BigDecimal montantTotal; // Utilisation de BigDecimal pour les montants financiers

    // Clé étrangère vers Utilisateur
    @ManyToOne
    @JoinColumn(name = "utilisateur_id", referencedColumnName = "id")
    private Utilisateur utilisateur;

    // Constructeur par défaut
    public Commande() {}

    // Constructeur avec paramètres
    public Commande(Long id, LocalDate dateCommande, String statut, BigDecimal montantTotal, Utilisateur utilisateur) {
        this.id = id;
        this.dateCommande = dateCommande;
        this.statut = statut;
        this.montantTotal = montantTotal;
        this.utilisateur = utilisateur;
    }

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getDateCommande() {
        return dateCommande;
    }

    public void setDateCommande(LocalDate dateCommande) {
        this.dateCommande = dateCommande;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public BigDecimal getMontantTotal() {
        return montantTotal;
    }

    public void setMontantTotal(BigDecimal montantTotal) {
        this.montantTotal = montantTotal;
    }

    public Utilisateur getUtilisateur() {
        return utilisateur;
    }

    public void setUtilisateur(Utilisateur utilisateur) {
        this.utilisateur = utilisateur;
    }
}