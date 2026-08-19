package club.taekwondo.dto;

import java.util.List;

/**
 * DTO de requête pour la création/édition d'un paiement.
 *
 * Compatible avec :
 * - JSON (/api/paiements/ajouter-manuel, /api/paiements/ajouter-complet)
 * - Multipart (/api/paiements/ajouter-complet) : le controller reconstruit ce DTO
 *
 * Conventions attendues par le service :
 * - typePaiement : "UNIQUE" | "ECHELONNE"
 * - modePaiement : "CB" | "VIREMENT" | "ESPECES"
 * - datePaiement : "yyyy-MM-dd"
 * - statut échéance : "en attente" | "payé"
 */
public class PaiementRequestDTO {

    // ===== Cible existante =====
    /** Identifiant d'un parent/adulte existant (payeur) */
    private Long utilisateurId;

    /** Identifiant d'un membre (enfant) ciblé. */
    private Long membreId;

    /** Optionnel : plusieurs enfants d'un coup (une création sera faite par enfant côté service). */
    private List<Long> membreIds;

    /** Optionnel : commande boutique réglée par ce paiement (absent pour une cotisation) */
    private Long commandeId;

    // ===== Création à la volée (si utilisateurId absent) =====
    /** Nom/prénom/email pour créer un payeur rapidement (parent/adulte) */
    private String utilisateurNom;
    private String utilisateurPrenom;
    private String utilisateurEmail;

    /** Optionnel : création d'un enfant en même temps (géré côté service) */
    private NewMembreInput newMembre;

    public static class NewMembreInput {
        private String prenom;
        private String nom;
        private String dateNaissance; // "yyyy-MM-dd" (optionnel)

        public String getPrenom() { return prenom; }
        public void setPrenom(String prenom) { this.prenom = prenom; }

        public String getNom() { return nom; }
        public void setNom(String nom) { this.nom = nom; }

        public String getDateNaissance() { return dateNaissance; }
        public void setDateNaissance(String dateNaissance) { this.dateNaissance = dateNaissance; }
    }

    // ===== Données de paiement =====
    private Double montantTotal;     // total à payer
    private String modePaiement;     // "CB" | "VIREMENT" | "ESPECES"
    private String typePaiement;     // "UNIQUE" | "ECHELONNE"
    private String datePaiement;     // "yyyy-MM-dd"
    private Integer nombreEcheances; // si echeances null -> fallback autosplit

    /** Échéances détaillées si typePaiement = ECHELONNE */
    private List<EcheanceInput> echeances;

    /** Commentaire libre (optionnel) */
    private String commentaire;

    // ===== Classe interne pour l'entrée d'échéance =====
    public static class EcheanceInput {
        private Integer numero;        // 1..N
        private String dateEcheance;   // "yyyy-MM-dd"
        private Double montant;
        private String statut;         // "en attente" | "payé"

        public Integer getNumero() { return numero; }
        public void setNumero(Integer numero) { this.numero = numero; }

        public String getDateEcheance() { return dateEcheance; }
        public void setDateEcheance(String dateEcheance) { this.dateEcheance = dateEcheance; }

        public Double getMontant() { return montant; }
        public void setMontant(Double montant) { this.montant = montant; }

        public String getStatut() { return statut; }
        public void setStatut(String statut) { this.statut = statut; }
    }

    // ===== Getters / Setters =====
    public Long getUtilisateurId() { return utilisateurId; }
    public void setUtilisateurId(Long utilisateurId) { this.utilisateurId = utilisateurId; }

    public Long getMembreId() { return membreId; }
    public void setMembreId(Long membreId) { this.membreId = membreId; }

    public List<Long> getMembreIds() { return membreIds; }
    public void setMembreIds(List<Long> membreIds) { this.membreIds = membreIds; }

    public Long getCommandeId() { return commandeId; }
    public void setCommandeId(Long commandeId) { this.commandeId = commandeId; }

    public String getUtilisateurNom() { return utilisateurNom; }
    public void setUtilisateurNom(String utilisateurNom) { this.utilisateurNom = utilisateurNom; }

    public String getUtilisateurPrenom() { return utilisateurPrenom; }
    public void setUtilisateurPrenom(String utilisateurPrenom) { this.utilisateurPrenom = utilisateurPrenom; }

    public String getUtilisateurEmail() { return utilisateurEmail; }
    public void setUtilisateurEmail(String utilisateurEmail) { this.utilisateurEmail = utilisateurEmail; }

    public NewMembreInput getNewMembre() { return newMembre; }
    public void setNewMembre(NewMembreInput newMembre) { this.newMembre = newMembre; }

    public Double getMontantTotal() { return montantTotal; }
    public void setMontantTotal(Double montantTotal) { this.montantTotal = montantTotal; }

    public String getModePaiement() { return modePaiement; }
    public void setModePaiement(String modePaiement) { this.modePaiement = modePaiement; }

    public String getTypePaiement() { return typePaiement; }
    public void setTypePaiement(String typePaiement) { this.typePaiement = typePaiement; }

    public String getDatePaiement() { return datePaiement; }
    public void setDatePaiement(String datePaiement) { this.datePaiement = datePaiement; }

    public Integer getNombreEcheances() { return nombreEcheances; }
    public void setNombreEcheances(Integer nombreEcheances) { this.nombreEcheances = nombreEcheances; }

    public List<EcheanceInput> getEcheances() { return echeances; }
    public void setEcheances(List<EcheanceInput> echeances) { this.echeances = echeances; }

    public String getCommentaire() { return commentaire; }
    public void setCommentaire(String commentaire) { this.commentaire = commentaire; }
}


