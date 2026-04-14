package club.taekwondo.entity.jpa;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Entity
@Table(
    name = "paiement",
    indexes = {
        @Index(name = "idx_paiement_utilisateur", columnList = "utilisateur_id"),
        @Index(name = "idx_paiement_membre", columnList = "membre_id"),
        @Index(name = "idx_paiement_intent", columnList = "payment_intent_id")
    }
)
public class Paiement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String type;

    @Column(name = "date_paiement")
    private LocalDate datePaiement;

    private String statut;

    @Column(name = "mode_paiement")
    private String modePaiement;

    @Column(name = "payment_intent_id", unique = true, length = 100)
    private String paymentIntentId;

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

    @Column(name = "charge_id", length = 100)
    private String chargeId;

    @Column(name = "receipt_url", columnDefinition = "TEXT")
    private String receiptUrl;

    @Column(name = "stripe_status", length = 50)
    private String stripeStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membre_id", nullable = false)
    private Membre membre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = true)
    private Club club;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commande_id", nullable = true)
    private Commande commande;

    @OneToMany(mappedBy = "paiement", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("numero ASC, dateEcheance ASC")
    @JsonManagedReference
    private List<Echeance> echeances = new ArrayList<>();

    public Paiement() {}

    @PrePersist
    public void prePersist() {
        if (statut == null || statut.isBlank()) {
            statut = "en attente";
        }
        if (this.club == null) {
            try {
                if (this.commande != null && this.commande.getClub() != null) {
                    this.club = this.commande.getClub();
                } else if (this.membre != null && this.membre.getClub() != null) {
                    this.club = this.membre.getClub();
                } else if (this.utilisateur != null && this.utilisateur.getClub() != null) {
                    this.club = this.utilisateur.getClub();
                }
            } catch (Exception ignored) {
            }
        }
    }

    @PreUpdate
    public void preUpdate() {
        if (this.club == null) {
            try {
                if (this.commande != null && this.commande.getClub() != null) {
                    this.club = this.commande.getClub();
                } else if (this.membre != null && this.membre.getClub() != null) {
                    this.club = this.membre.getClub();
                } else if (this.utilisateur != null && this.utilisateur.getClub() != null) {
                    this.club = this.utilisateur.getClub();
                }
            } catch (Exception ignored) {
            }
        }
    }

    private String normalizeMode(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.toLowerCase(Locale.ROOT)
                .replace("é", "e").replace("è", "e").replace("ê", "e")
                .replace("à", "a").replace("ç", "c")
                .trim();

        if (normalized.equals("cb")
                || normalized.equals("carte")
                || normalized.equals("carte bancaire")
                || normalized.equals("cartebancaire")
                || normalized.equals("stripe")) {
            return "CB";
        }
        if (normalized.equals("virement")) {
            return "VIREMENT";
        }
        if (normalized.equals("cheque")) {
            return "CHEQUE";
        }
        if (normalized.equals("club")) {
            return "CLUB";
        }
        if (normalized.equals("especes") || normalized.equals("espece")) {
            return "ESPECES";
        }

        return value.trim().toUpperCase(Locale.ROOT);
    }

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

    public String getPaymentIntentId() { return paymentIntentId; }
    public void setPaymentIntentId(String paymentIntentId) { this.paymentIntentId = paymentIntentId; }

    public Utilisateur getUtilisateur() { return utilisateur; }
    public void setUtilisateur(Utilisateur utilisateur) { this.utilisateur = utilisateur; }

    public Membre getMembre() { return membre; }
    public void setMembre(Membre membre) { this.membre = membre; }

    public Commande getCommande() { return commande; }
    public void setCommande(Commande commande) { this.commande = commande; }

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

    public String getChargeId() { return chargeId; }
    public void setChargeId(String chargeId) { this.chargeId = chargeId; }

    public String getReceiptUrl() { return receiptUrl; }
    public void setReceiptUrl(String receiptUrl) { this.receiptUrl = receiptUrl; }

    public String getStripeStatus() { return stripeStatus; }
    public void setStripeStatus(String stripeStatus) { this.stripeStatus = stripeStatus; }

    public void addEcheance(Echeance echeance) {
        this.echeances.add(echeance);
        echeance.setPaiement(this);
    }

    public void removeEcheance(Echeance echeance) {
        this.echeances.remove(echeance);
        echeance.setPaiement(null);
    }

    public Club getClub() { return club; }
    public void setClub(Club club) { this.club = club; }
}
