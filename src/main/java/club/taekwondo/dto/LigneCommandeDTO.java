package club.taekwondo.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class LigneCommandeDTO {

    private Long id;
    private Long commandeId;

    @NotNull(message = "L'identifiant du produit est obligatoire.")
    private Long produitId; // 🔹 Ajout ici

    @NotNull(message = "Le nom du produit est obligatoire.")
    private String produitNom;

    @NotNull(message = "La quantité est obligatoire.")
    @Min(value = 1, message = "La quantité doit être au moins 1.")
    private Integer quantite;

    @NotNull(message = "Le prix unitaire est obligatoire.")
    private Double prixUnitaire;

    @NotNull(message = "Le sous-total est obligatoire.")
    private Double sousTotal;

    private String taille;
    private String couleur;
    private String flocage;

    private Long beneficiaireId;
    private String beneficiairePrenom;
    private String beneficiaireNom;
    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCommandeId() {
        return commandeId;
    }

    public void setCommandeId(Long commandeId) {
        this.commandeId = commandeId;
    }

    public Long getProduitId() {
        return produitId;
    }

    public void setProduitId(Long produitId) {
        this.produitId = produitId;
    }

    public String getProduitNom() {
        return produitNom;
    }

    public void setProduitNom(String produitNom) {
        this.produitNom = produitNom;
    }

    public Integer getQuantite() {
        return quantite;
    }

    public void setQuantite(Integer quantite) {
        this.quantite = quantite;
    }

    public Double getPrixUnitaire() {
        return prixUnitaire;
    }

    public void setPrixUnitaire(Double prixUnitaire) {
        this.prixUnitaire = prixUnitaire;
    }

    public Double getSousTotal() {
        return sousTotal;
    }

    public void setSousTotal(Double sousTotal) {
        this.sousTotal = sousTotal;
    }

    public String getTaille() {
        return taille;
    }

    public void setTaille(String taille) {
        this.taille = taille;
    }

    public String getCouleur() {
        return couleur;
    }

    public void setCouleur(String couleur) {
        this.couleur = couleur;
    }

    public String getFlocage() {
        return flocage;
    }

    public void setFlocage(String flocage) {
        this.flocage = flocage;
    }

	public Long getBeneficiaireId() {
		return beneficiaireId;
	}

	public void setBeneficiaireId(Long beneficiaireId) {
		this.beneficiaireId = beneficiaireId;
	}

	public String getBeneficiairePrenom() {
		return beneficiairePrenom;
	}

	public void setBeneficiairePrenom(String beneficiairePrenom) {
		this.beneficiairePrenom = beneficiairePrenom;
	}

	public String getBeneficiaireNom() {
		return beneficiaireNom;
	}

	public void setBeneficiaireNom(String beneficiaireNom) {
		this.beneficiaireNom = beneficiaireNom;
	}
    
    
}

