package club.taekwondo.entity.jpa;

import jakarta.persistence.*;
import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
public class Echeance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate dateEcheance;

    private double montant;

    /** payé | en attente | en retard | annulé */
    private String statut;

    @Column(name = "numero")
    private Integer numero;

    // 🔹 NOUVEAU : mode de paiement spécifique à l'échéance (cb/stripe, virement, espèces)
    @Column(name = "mode_paiement")
    private String modePaiement;

    // 🔹 NOUVEAU : date réelle d'enregistrement du paiement de cette échéance
    @Column(name = "date_paiement_reel")
    private LocalDate datePaiementReel;

    // 🔹 NOUVEAU : référence technique (id Stripe, n° virement, note espèces)
    @Column(name = "reference")
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paiement_id")
    @JsonBackReference
    private Paiement paiement;

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getDateEcheance() { return dateEcheance; }
    public void setDateEcheance(LocalDate dateEcheance) { this.dateEcheance = dateEcheance; }

    public double getMontant() { return montant; }
    public void setMontant(double montant) { this.montant = montant; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public Integer getNumero() { return numero; }
    public void setNumero(Integer numero) { this.numero = numero; }

    public String getModePaiement() { return modePaiement; }
    public void setModePaiement(String modePaiement) { this.modePaiement = modePaiement; }

    public LocalDate getDatePaiementReel() { return datePaiementReel; }
    public void setDatePaiementReel(LocalDate datePaiementReel) { this.datePaiementReel = datePaiementReel; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public Paiement getPaiement() { return paiement; }
    public void setPaiement(Paiement paiement) { this.paiement = paiement; }
}