package club.taekwondo.entity.jpa;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Entity
@Access(AccessType.FIELD)
@Table(name = "commande")
public class Commande {

    @ManyToOne
    private Club club;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date_commande", nullable = false)
    private LocalDate dateCommande;

    @Column(nullable = false, length = 50)
    private String statut;

    @Column(name = "montant_total", precision = 10, scale = 2)
    private BigDecimal montantTotal; 
    
    @Column(name = "mode_paiement")
    private String modePaiement;
    
    @Column(name = "date_paiement")
    private LocalDate datePaiement;

    // 🔹 Relation avec Utilisateur (parent/membre avec compte)
    @ManyToOne
    @JoinColumn(name = "utilisateur_id", referencedColumnName = "id")
    private Utilisateur utilisateur;

    @ManyToOne
    @JoinColumn(name = "campagne_id")
    private CampagneCommande campagne;

    // 🔹 Relation avec les lignes de commande
    @OneToMany(mappedBy = "commande", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LigneCommande> lignes;

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

    public Club getClub() {
        return club;
    }

    public void setClub(Club club) {
        this.club = club;
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

    public String getModePaiement() {
        return modePaiement;
    }

    public void setModePaiement(String modePaiement) {
        this.modePaiement = modePaiement;
    }

    public LocalDate getDatePaiement() {
        return datePaiement;
    }

    public void setDatePaiement(LocalDate datePaiement) {
        this.datePaiement = datePaiement;
    }

    public CampagneCommande getCampagne() {
        return campagne;
    }

    public void setCampagne(CampagneCommande campagne) {
        this.campagne = campagne;
    }

    public List<LigneCommande> getLignes() {
        return lignes;
    }

    public void setLignes(List<LigneCommande> lignes) {
        this.lignes = lignes;
    }
}
