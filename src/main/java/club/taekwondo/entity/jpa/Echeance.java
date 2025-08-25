package club.taekwondo.entity.jpa;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(
    name = "echeance",
    indexes = { @Index(name = "idx_echeance_paiement", columnList = "paiement_id") }
)
public class Echeance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date_echeance", nullable = false)
    private LocalDate dateEcheance;

    @Column(nullable = false)
    private Double montant; // <— Double pour rester cohérent

    /** payé | en attente | en retard | annulé */
    @Column(nullable = false)
    private String statut;

    @Column(name = "numero")
    private Integer numero;

    // mode de paiement spécifique à l'échéance (cb/stripe, virement, espèces)
    @Column(name = "mode_paiement")
    private String modePaiement;

    // date réelle d'enregistrement du paiement de cette échéance
    @Column(name = "date_paiement_reel")
    private LocalDate datePaiementReel;

    // référence technique (id Stripe, n° virement, note espèces)
    @Column(name = "reference")
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paiement_id")
    @JsonBackReference
    private Paiement paiement;

    /* ---------- Hooks ---------- */
    @PrePersist
    public void prePersist() {
        if (statut == null || statut.isBlank()) {
            statut = "en attente";
        }
    }

    /* ---------- Getters / Setters ---------- */
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

    public String getModePaiement() { return modePaiement; }
    public void setModePaiement(String modePaiement) {
        // Normalisation légère (optionnelle)
        if (modePaiement == null) { this.modePaiement = null; return; }
        String m = modePaiement.toLowerCase()
            .replace("é", "e").replace("è", "e").replace("ê", "e")
            .replace("à", "a").replace("ç", "c")
            .trim();
        if (m.equals("cb") || m.equals("carte") || m.equals("carte bancaire") || m.equals("cartebancaire") || m.equals("stripe")) {
            this.modePaiement = "cb";
        } else if (m.equals("virement")) {
            this.modePaiement = "virement";
        } else if (m.equals("especes") || m.equals("espece")) {
            this.modePaiement = "espèces";
        } else {
            this.modePaiement = modePaiement;
        }
    }

    public LocalDate getDatePaiementReel() { return datePaiementReel; }
    public void setDatePaiementReel(LocalDate datePaiementReel) { this.datePaiementReel = datePaiementReel; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public Paiement getPaiement() { return paiement; }
    public void setPaiement(Paiement paiement) { this.paiement = paiement; }
}
