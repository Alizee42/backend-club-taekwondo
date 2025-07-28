package club.taekwondo.entity.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "parametres_paiement")
public class ParametresPaiement {

    @Id
    private Long id; // Identifiant fixe pour les paramètres globaux (ex : 1)

    @Column(nullable = false)
    private double montantCotisation;

    @Column(nullable = false)
    private boolean virement;

    @Column(nullable = false)
    private boolean especes;

    @Column(nullable = false)
    private boolean stripe;

    @Column(nullable = false)
    private String modePaiementParDefaut;

    @Column(nullable = false)
    private int echeancesAutorisees;

    @Column(nullable = false)
    private String intervalleEcheance;

    // === GETTERS & SETTERS ===
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public double getMontantCotisation() {
        return montantCotisation;
    }

    public void setMontantCotisation(double montantCotisation) {
        this.montantCotisation = montantCotisation;
    }

    public boolean isVirement() {
        return virement;
    }

    public void setVirement(boolean virement) {
        this.virement = virement;
    }

    public boolean isEspeces() {
        return especes;
    }

    public void setEspeces(boolean especes) {
        this.especes = especes;
    }

    public boolean isStripe() {
        return stripe;
    }

    public void setStripe(boolean stripe) {
        this.stripe = stripe;
    }

    public String getModePaiementParDefaut() {
        return modePaiementParDefaut;
    }

    public void setModePaiementParDefaut(String modePaiementParDefaut) {
        this.modePaiementParDefaut = modePaiementParDefaut;
    }

    public int getEcheancesAutorisees() {
        return echeancesAutorisees;
    }

    public void setEcheancesAutorisees(int echeancesAutorisees) {
        this.echeancesAutorisees = echeancesAutorisees;
    }

    public String getIntervalleEcheance() {
        return intervalleEcheance;
    }

    public void setIntervalleEcheance(String intervalleEcheance) {
        this.intervalleEcheance = intervalleEcheance;
    }
}
