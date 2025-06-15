package club.taekwondo.entity.jpa;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;

@Entity
public class Paiement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String type; 

    private Double montant; 
    @Column(name = "date_paiement", nullable = false)
    private LocalDate datePaiement;

    private String statut; 

    @Column(name = "mode_paiement", nullable = false)
    private String modePaiement; 

    @ManyToOne
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;
    
    @OneToMany(mappedBy = "paiement", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<Echeance> echeances;
    
    @Column(name = "montant_total")
    private Double montantTotal;

    @Column(name = "montant_restant")
    private Double montantRestant;

    @Column(name = "echeances_totales")
    private Integer echeancesTotales;

    @Column(name = "echeances_restantes")
    private Integer echeancesRestantes;
    // Constructeur par défaut
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

    public Utilisateur getUtilisateur() {
        return utilisateur;
    }

    public void setUtilisateur(Utilisateur utilisateur) {
        this.utilisateur = utilisateur;
    }

    public Double getMontantTotal() {
        return montantTotal;
    }

    public void setMontantTotal(Double montantTotal) {
        this.montantTotal = montantTotal;
    }

    public Double getMontantRestant() {
        return montantRestant;
    }

    public void setMontantRestant(Double montantRestant) {
        this.montantRestant = montantRestant;
    }

    public Integer getEcheancesTotales() {
        return echeancesTotales;
    }

    public void setEcheancesTotales(Integer echeancesTotales) {
        this.echeancesTotales = echeancesTotales;
    }

    public Integer getEcheancesRestantes() {
        return echeancesRestantes;
    }

    public void setEcheancesRestantes(Integer echeancesRestantes) {
        this.echeancesRestantes = echeancesRestantes;
    }
    public List<Echeance> getEcheances() {
        return echeances;
    }

    public void setEcheances(List<Echeance> echeances) {
        this.echeances = echeances;
    }
}

