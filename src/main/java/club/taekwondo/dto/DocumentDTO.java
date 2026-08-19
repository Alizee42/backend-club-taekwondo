package club.taekwondo.dto;

import java.time.LocalDateTime;

public class DocumentDTO {
    private Long id;
    private String typeDocument;
    private String nomDocument;
    private String cheminFichier;
    private LocalDateTime dateDepot;
    private String status;
    private String motifRefus;

    /** Nouveau : commentaire côté front (vient de Document.description) */
    private String commentaire;

    /** Nouveaux : identifiants simples pour éviter la récursion JSON */
    private Long utilisateurId;
    private Long membreId;
    private Long enfantId;     // alias lisible par le front (même valeur que membreId)
    private MembreDTO enfant;  // objet enfant (id, prenom, nom, numeroLicence)

    /** Optionnel : on garde pour compat si tu l’utilisais déjà */
    private UtilisateurDTO utilisateur;

    // ===== Getters / Setters =====
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTypeDocument() { return typeDocument; }
    public void setTypeDocument(String typeDocument) { this.typeDocument = typeDocument; }

    public String getNomDocument() { return nomDocument; }
    public void setNomDocument(String nomDocument) { this.nomDocument = nomDocument; }

    public String getCheminFichier() { return cheminFichier; }
    public void setCheminFichier(String cheminFichier) { this.cheminFichier = cheminFichier; }

    public LocalDateTime getDateDepot() { return dateDepot; }
    public void setDateDepot(LocalDateTime dateDepot) { this.dateDepot = dateDepot; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMotifRefus() { return motifRefus; }
    public void setMotifRefus(String motifRefus) { this.motifRefus = motifRefus; }

    public String getCommentaire() { return commentaire; }
    public void setCommentaire(String commentaire) { this.commentaire = commentaire; }

    public Long getUtilisateurId() { return utilisateurId; }
    public void setUtilisateurId(Long utilisateurId) { this.utilisateurId = utilisateurId; }

    public Long getMembreId() { return membreId; }
    public void setMembreId(Long membreId) { this.membreId = membreId; }

    public UtilisateurDTO getUtilisateur() { return utilisateur; }
    public void setUtilisateur(UtilisateurDTO utilisateur) { this.utilisateur = utilisateur; }
	public Long getEnfantId() {
		return enfantId;
	}
	public void setEnfantId(Long enfantId) {
		this.enfantId = enfantId;
	}
	public MembreDTO getEnfant() {
		return enfant;
	}
	public void setEnfant(MembreDTO enfant) {
		this.enfant = enfant;
	}
    
}
