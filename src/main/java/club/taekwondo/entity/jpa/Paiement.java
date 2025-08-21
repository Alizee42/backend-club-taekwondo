package club.taekwondo.entity.jpa;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "paiement",
    indexes = {
        @Index(name = "idx_paiement_utilisateur", columnList = "utilisateur_id"),
        @Index(name = "idx_paiement_membre", columnList = "membre_id")
    }
)
public class Paiement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Forme: unique | échelonné | cotisation */
    @Column(nullable = false)
    private String type;

    /** Date de création / réalisation (facultative) */
    @Column(name = "date_paiement")
    private LocalDate datePaiement;

    /** Statut global: payé | en attente | en retard | annulé | inconnu */
    private String statut;

    /**
     * Moyen par défaut (utile surtout pour les paiements UNIQUES).
     * Pour un ÉCHÉLONNÉ, le moyen réel est porté par chaque échéance.
     * Ex: cb | virement | espèces
     */
    @Column(name = "mode_paiement")
    private String modePaiement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membre_id", nullable = false)
    private Membre membre;

    @OneToMany(mappedBy = "paiement", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("numero ASC, dateEcheance ASC")
    @JsonManagedReference
    private List<Echeance> echeances = new ArrayList<>();

    @Column(name = "montant_total")
    private Double montantTotal;

    @Column(name = "montant_restant")
    private Double montantRestant;

    @Column(name = "montant_paye")
    private Double montantPaye;

    @Column(name = "echeances_totales")
    private Integer echeancesTotales;

    @Column(name = "echeances_restantes")
    private Integer echeancesRestantes;

    @Column(name = "motif_annulation")
    private String motifAnnulation;

    @Column(name = "date_annulation")
    private LocalDateTime dateAnnulation;

    @Column(name = "admin_responsable")
    private String adminResponsable;

    public Paiement() {}

    /* ---------------- Hooks ---------------- */

    @PrePersist
    public void prePersist() {
        if (statut == null || statut.isBlank()) {
            statut = "en attente";
        }
    }

    /* ------------- Normalisation simple du mode ------------- */

    private String normalizeMode(String v) {
        if (v == null) return null;
        String m = v.toLowerCase()
                .replace("é", "e").replace("è", "e").replace("ê", "e")
                .replace("à", "a").replace("ç", "c")
                .trim();
        if (m.equals("cb") || m.equals("carte") || m.equals("carte bancaire") || m.equals("cartebancaire") || m.equals("stripe")) {
            return "cb";
        }
        if (m.equals("virement")) return "virement";
        if (m.equals("especes") || m.equals("espece")) return "espèces";
        return v; // conserver tel quel sinon
    }

    /* ---------------- Getters / Setters ---------------- */

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public LocalDate getDatePaiement() { return datePaiement; }
    public void setDatePaiement(LocalDate datePaiement) { this.datePaiement = datePaiement; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public String getModePaiement() { return modePaiement; }
    public void setModePaiement(String modePaiement) { this.modePaiement = normalizeMode(modePaiement); }

    public Utilisateur getUtilisateur() { return utilisateur; }
    public void setUtilisateur(Utilisateur utilisateur) { this.utilisateur = utilisateur; }

    public Membre getMembre() { return membre; }
    public void setMembre(Membre membre) { this.membre = membre; }

    public List<Echeance> getEcheances() { return echeances; }
    public void setEcheances(List<Echeance> echeances) { this.echeances = echeances; }

    public Double getMontantTotal() { return montantTotal; }
    public void setMontantTotal(Double montantTotal) { this.montantTotal = montantTotal; }

    public Double getMontantRestant() { return montantRestant; }
    public void setMontantRestant(Double montantRestant) { this.montantRestant = montantRestant; }

    public Double getMontantPaye() { return montantPaye; }
    public void setMontantPaye(Double montantPaye) { this.montantPaye = montantPaye; }

    public Integer getEcheancesTotales() { return echeancesTotales; }
    public void setEcheancesTotales(Integer echeancesTotales) { this.echeancesTotales = echeancesTotales; }

    public Integer getEcheancesRestantes() { return echeancesRestantes; }
    public void setEcheancesRestantes(Integer echeancesRestantes) { this.echeancesRestantes = echeancesRestantes; }

    public String getMotifAnnulation() { return motifAnnulation; }
    public void setMotifAnnulation(String motifAnnulation) { this.motifAnnulation = motifAnnulation; }

    public LocalDateTime getDateAnnulation() { return dateAnnulation; }
    public void setDateAnnulation(LocalDateTime dateAnnulation) { this.dateAnnulation = dateAnnulation; }

    public String getAdminResponsable() { return adminResponsable; }
    public void setAdminResponsable(String adminResponsable) { this.adminResponsable = adminResponsable; }

    /* ---------------- Helpers (facultatif) ---------------- */

    public void addEcheance(Echeance e) {
        this.echeances.add(e);
        e.setPaiement(this);
    }

    public void removeEcheance(Echeance e) {
        this.echeances.remove(e);
        e.setPaiement(null);
    }
}
