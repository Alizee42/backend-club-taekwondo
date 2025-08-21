package club.taekwondo.dto;

import java.time.LocalDate;

public class EcheanceDTO {

    private Long id;
    private LocalDate dateEcheance;
    private Double montant;
    private String statut;
    private Integer numero;

    // Infos parent/enfant (déjà présentes chez toi)
    private String nom;
    private String prenom;
    private String enfantPrenom;
    private String enfantNom;

    // 🔹 Nouveaux champs pour la gestion du paiement au niveau de l'échéance
    /** "cb"/"stripe", "virement", "espèces" (ou vide si non renseigné) */
    private String modePaiement;

    /** Date réelle d'enregistrement du paiement de cette échéance */
    private LocalDate datePaiementReel;

    /** Référence technique : id Stripe, n° virement, ou note pour espèces */
    private String reference;

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getDateEcheance() { return dateEcheance; }
    public void setDateEcheance(LocalDate dateEcheance) { this.dateEcheance = dateEcheance; }

    public Double getMontant() { return montant; }
    public void setMontant(Double montant) { this.montant = montant; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public Integer getNumero() { return numero; }
    public void setNumero(Integer numero) { this.numero = numero; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getEnfantPrenom() { return enfantPrenom; }
    public void setEnfantPrenom(String enfantPrenom) { this.enfantPrenom = enfantPrenom; }

    public String getEnfantNom() { return enfantNom; }
    public void setEnfantNom(String enfantNom) { this.enfantNom = enfantNom; }

    public String getModePaiement() { return modePaiement; }
    public void setModePaiement(String modePaiement) { this.modePaiement = modePaiement; }

    public LocalDate getDatePaiementReel() { return datePaiementReel; }
    public void setDatePaiementReel(LocalDate datePaiementReel) { this.datePaiementReel = datePaiementReel; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    @Override
    public String toString() {
        return "EcheanceDTO{" +
                "id=" + id +
                ", numero=" + numero +
                ", dateEcheance=" + dateEcheance +
                ", montant=" + montant +
                ", statut='" + statut + '\'' +
                ", modePaiement='" + modePaiement + '\'' +
                ", datePaiementReel=" + datePaiementReel +
                ", reference='" + reference + '\'' +
                '}';
    }
}

