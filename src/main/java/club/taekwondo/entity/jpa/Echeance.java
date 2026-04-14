package club.taekwondo.entity.jpa;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.Locale;

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
    private Double montant;

    @Column(nullable = false)
    private String statut;

    @Column(name = "numero")
    private Integer numero;

    @Column(name = "mode_paiement")
    private String modePaiement;

    @Column(name = "date_paiement_reel")
    private LocalDate datePaiementReel;

    @Column(name = "reference")
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paiement_id")
    @JsonBackReference
    private Paiement paiement;

    @PrePersist
    public void prePersist() {
        if (statut == null || statut.isBlank()) {
            statut = "en attente";
        }
    }

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
        if (modePaiement == null) {
            this.modePaiement = null;
            return;
        }

        String normalized = modePaiement.toLowerCase(Locale.ROOT)
                .replace("é", "e").replace("è", "e").replace("ê", "e")
                .replace("à", "a").replace("ç", "c")
                .trim();

        if (normalized.equals("cb")
                || normalized.equals("carte")
                || normalized.equals("carte bancaire")
                || normalized.equals("cartebancaire")
                || normalized.equals("stripe")) {
            this.modePaiement = "CB";
        } else if (normalized.equals("virement")) {
            this.modePaiement = "VIREMENT";
        } else if (normalized.equals("cheque")) {
            this.modePaiement = "CHEQUE";
        } else if (normalized.equals("club")) {
            this.modePaiement = "CLUB";
        } else if (normalized.equals("especes") || normalized.equals("espece")) {
            this.modePaiement = "ESPECES";
        } else {
            this.modePaiement = modePaiement.trim().toUpperCase(Locale.ROOT);
        }
    }

    public LocalDate getDatePaiementReel() { return datePaiementReel; }
    public void setDatePaiementReel(LocalDate datePaiementReel) { this.datePaiementReel = datePaiementReel; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public Paiement getPaiement() { return paiement; }
    public void setPaiement(Paiement paiement) { this.paiement = paiement; }
}
