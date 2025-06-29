package club.taekwondo.entity.jpa;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class ParametresPaiement {

    @Id
    private Long id; // Ajoutez un identifiant unique pour l'entité

    private double montantCotisation;
    private boolean virement;
    private boolean especes;
    private boolean stripe;
    private String modePaiementParDefaut;
    private int echeancesAutorisees;
    private String intervalleEcheance;

    // Getters et setters
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