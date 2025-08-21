package club.taekwondo.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de réponse pour l'affichage et le suivi des paiements.
 *
 * Conventions:
 * - type         : "UNIQUE" | "ECHELONNE" | "COTISATION"
 * - modePaiement : "CB" | "VIREMENT" | "ESPECES"
 * - statut       : "payé" | "en attente" | "en retard" | "annulé"
 *
 * Dates renvoyées en String ISO (yyyy-MM-dd) pour simplicité côté Angular.
 */
public class PaiementDTO {

    private Long id;

    /** "UNIQUE" | "ECHELONNE" | "COTISATION" */
    private String type;

    /** Date du paiement initial (ISO yyyy-MM-dd) */
    private String datePaiement;

    /** "payé" | "en attente" | "en retard" | "annulé" */
    private String statut;

    /** Mode principal (si échéances, le détail peut être au niveau de l’échéance) */
    private String modePaiement; // "CB" | "VIREMENT" | "ESPECES"

    /** Montants */
    private Double montantTotal;   // total
    private Double montantPaye;    // déjà payé
    private Double montantRestant; // >= 0

    /** Utilisateur (parent/adulte) payeur */
    private Long utilisateurId;
    private String utilisateurNom;
    private String utilisateurPrenom;
    private String utilisateurEmail;

    /** Enfant / Membre concerné */
    private Long membreId;
    private String membreNom;
    private String membrePrenom;
    private String enfantNomComplet; // confort d'affichage

    /** Détail des échéances si type = ECHELONNE */
    private List<EcheanceDTO> echeances;

    /** Infos d'annulation (optionnel) */
    private String motifAnnulation;
    private LocalDateTime dateAnnulation;
    private String adminResponsable;

    // ===== Getters / Setters =====
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDatePaiement() { return datePaiement; }
    public void setDatePaiement(String datePaiement) { this.datePaiement = datePaiement; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public String getModePaiement() { return modePaiement; }
    public void setModePaiement(String modePaiement) { this.modePaiement = modePaiement; }

    public Double getMontantTotal() { return montantTotal; }
    public void setMontantTotal(Double montantTotal) { this.montantTotal = montantTotal; }

    public Double getMontantPaye() { return montantPaye; }
    public void setMontantPaye(Double montantPaye) { this.montantPaye = montantPaye; }

    public Double getMontantRestant() { return montantRestant; }
    public void setMontantRestant(Double montantRestant) { this.montantRestant = montantRestant; }

    public Long getUtilisateurId() { return utilisateurId; }
    public void setUtilisateurId(Long utilisateurId) { this.utilisateurId = utilisateurId; }

    public String getUtilisateurNom() { return utilisateurNom; }
    public void setUtilisateurNom(String utilisateurNom) { this.utilisateurNom = utilisateurNom; }

    public String getUtilisateurPrenom() { return utilisateurPrenom; }
    public void setUtilisateurPrenom(String utilisateurPrenom) { this.utilisateurPrenom = utilisateurPrenom; }

    public String getUtilisateurEmail() { return utilisateurEmail; }
    public void setUtilisateurEmail(String utilisateurEmail) { this.utilisateurEmail = utilisateurEmail; }

    public Long getMembreId() { return membreId; }
    public void setMembreId(Long membreId) { this.membreId = membreId; }

    public String getMembreNom() { return membreNom; }
    public void setMembreNom(String membreNom) { this.membreNom = membreNom; }

    public String getMembrePrenom() { return membrePrenom; }
    public void setMembrePrenom(String membrePrenom) { this.membrePrenom = membrePrenom; }

    public String getEnfantNomComplet() { return enfantNomComplet; }
    public void setEnfantNomComplet(String enfantNomComplet) { this.enfantNomComplet = enfantNomComplet; }

    public List<EcheanceDTO> getEcheances() { return echeances; }
    public void setEcheances(List<EcheanceDTO> echeances) { this.echeances = echeances; }

    public String getMotifAnnulation() { return motifAnnulation; }
    public void setMotifAnnulation(String motifAnnulation) { this.motifAnnulation = motifAnnulation; }

    public LocalDateTime getDateAnnulation() { return dateAnnulation; }
    public void setDateAnnulation(LocalDateTime dateAnnulation) { this.dateAnnulation = dateAnnulation; }

    public String getAdminResponsable() { return adminResponsable; }
    public void setAdminResponsable(String adminResponsable) { this.adminResponsable = adminResponsable; }

    // ===== Échéance (réponse) =====
    public static class EcheanceDTO {
        private Long id;
        private Integer numero;        // 1..N
        private String dateEcheance;   // ISO yyyy-MM-dd
        private Double montant;
        private String statut;         // "en attente" | "payé" | "en retard"
        private String modePaiement;   // "CB" | "VIREMENT" | "ESPECES" (si géré par échéance)
        private String reference;      // référence externe (optionnel)

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public Integer getNumero() { return numero; }
        public void setNumero(Integer numero) { this.numero = numero; }

        public String getDateEcheance() { return dateEcheance; }
        public void setDateEcheance(String dateEcheance) { this.dateEcheance = dateEcheance; }

        public Double getMontant() { return montant; }
        public void setMontant(Double montant) { this.montant = montant; }

        public String getStatut() { return statut; }
        public void setStatut(String statut) { this.statut = statut; }

        public String getModePaiement() { return modePaiement; }
        public void setModePaiement(String modePaiement) { this.modePaiement = modePaiement; }

        public String getReference() { return reference; }
        public void setReference(String reference) { this.reference = reference; }
    }
}


