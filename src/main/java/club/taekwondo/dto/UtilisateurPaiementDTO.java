package club.taekwondo.dto;

import java.util.List;


public class UtilisateurPaiementDTO {
    private Long id;
    private String nom;
    private String prenom;
    private String email;
    private String role;
    private List<PaiementDTO> paiements;
    private Double montantRestant;
    private String statut;


    // --- Getters & Setters ---
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public List<PaiementDTO> getPaiements() {
        return paiements;
    }

    public void setPaiements(List<PaiementDTO> paiements) {
        this.paiements = paiements;
    }

	public Double getMontantRestant() {
		return montantRestant;
	}

	public void setMontantRestant(Double montantRestant) {
		this.montantRestant = montantRestant;
	}

	public String getStatut() {
		return statut;
	}

	public void setStatut(String statut) {
		this.statut = statut;
	}
    
}

