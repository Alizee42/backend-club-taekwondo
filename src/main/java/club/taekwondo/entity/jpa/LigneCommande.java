package club.taekwondo.entity.jpa;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;

@Entity
@Access(AccessType.FIELD)
@Table(name = "ligne_commande")
public class LigneCommande {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🔹 Relation vers Commande (plusieurs lignes appartiennent à une commande)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commande_id", nullable = false)
    @JsonBackReference
    private Commande commande;

    // 🔹 Relation vers Produit
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produit_id", nullable = false)
    private Produit produit;

    @Column(nullable = false)
    private Integer quantite;

    @Column(nullable = false)
    private Double prixUnitaire;

    @Column(nullable = false)
    private Double sousTotal;

    // Champs additionnels pour personnalisation
    @Column(nullable = true)
    private String taille;

    @Column(nullable = true)
    private String couleur;

    @Column(nullable = true)
    private String flocage;

    // 🔹 Bénéficiaire (enfant) optionnel
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "beneficiaire_id") // nullable autorisé
    private Membre beneficiaire;

    // =========================
    // Constructeurs
    // =========================
    public LigneCommande() {}

    public LigneCommande(Long id, Commande commande, Produit produit,
                         Integer quantite, Double prixUnitaire, Double sousTotal,
                         String taille, String couleur, String flocage) {
        this.id = id;
        this.commande = commande;
        this.produit = produit;
        this.quantite = quantite;
        this.prixUnitaire = prixUnitaire;
        this.sousTotal = sousTotal;
        this.taille = taille;
        this.couleur = couleur;
        this.flocage = flocage;
    }

    // =========================
    // Getters et Setters
    // =========================
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Commande getCommande() {
        return commande;
    }

    public void setCommande(Commande commande) {
        this.commande = commande;
    }

    public Produit getProduit() {
        return produit;
    }

    public void setProduit(Produit produit) {
        this.produit = produit;
    }

    public Integer getQuantite() {
        return quantite;
    }

    public void setQuantite(Integer quantite) {
        this.quantite = quantite;
    }

    public Double getPrixUnitaire() {
        return prixUnitaire;
    }

    public void setPrixUnitaire(Double prixUnitaire) {
        this.prixUnitaire = prixUnitaire;
    }

    public Double getSousTotal() {
        return sousTotal;
    }

    public void setSousTotal(Double sousTotal) {
        this.sousTotal = sousTotal;
    }

    public String getTaille() {
        return taille;
    }

    public void setTaille(String taille) {
        this.taille = taille;
    }

    public String getCouleur() {
        return couleur;
    }

    public void setCouleur(String couleur) {
        this.couleur = couleur;
    }

    public String getFlocage() {
        return flocage;
    }

    public void setFlocage(String flocage) {
        this.flocage = flocage;
    }

    public Membre getBeneficiaire() {
        return beneficiaire;
    }

    public void setBeneficiaire(Membre beneficiaire) {
        this.beneficiaire = beneficiaire;
    }
}
