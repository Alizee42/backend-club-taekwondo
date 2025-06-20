package club.taekwondo.dto;

import java.time.LocalDate;

public class MembreRetardDTO {
    private String nom;
    private double totalRestant;
    private LocalDate echeanceDate; // Date de l'échéance en retard
    private double echeanceMontant; // Montant de l'échéance en retard

    // Constructeur
    public MembreRetardDTO(String nom, double totalRestant, LocalDate echeanceDate, double echeanceMontant) {
        this.nom = nom;
        this.totalRestant = totalRestant;
        this.echeanceDate = echeanceDate;
        this.echeanceMontant = echeanceMontant;
    }

    // Getters et Setters
    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public double getTotalRestant() {
        return totalRestant;
    }

    public void setTotalRestant(double totalRestant) {
        this.totalRestant = totalRestant;
    }

    public LocalDate getEcheanceDate() {
        return echeanceDate;
    }

    public void setEcheanceDate(LocalDate echeanceDate) {
        this.echeanceDate = echeanceDate;
    }

    public double getEcheanceMontant() {
        return echeanceMontant;
    }

    public void setEcheanceMontant(double echeanceMontant) {
        this.echeanceMontant = echeanceMontant;
    }
}
