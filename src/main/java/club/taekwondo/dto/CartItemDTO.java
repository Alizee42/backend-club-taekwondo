package club.taekwondo.dto;

public class CartItemDTO {
    private Long produitId;
    private Integer quantite;
    private String taille;
    private String couleur;
    private Boolean flocageActif;
    private String flocage;

    // Optionnels : permettent de ne pas dépendre du prix en BDD
    private Double prixUnitaire; // si fourni par le front
    private Double prixTotal;    // si déjà calculé côté front

    // ⬇️ NOUVEAU : bénéficiaire (enfant) pour CETTE ligne
    private Long beneficiaireId;

    // --- getters/setters ---
    public Long getProduitId() { return produitId; }
    public void setProduitId(Long produitId) { this.produitId = produitId; }

    public Integer getQuantite() { return quantite; }
    public void setQuantite(Integer quantite) { this.quantite = quantite; }

    public String getTaille() { return taille; }
    public void setTaille(String taille) { this.taille = taille; }

    public String getCouleur() { return couleur; }
    public void setCouleur(String couleur) { this.couleur = couleur; }

    public Boolean getFlocageActif() { return flocageActif; }
    public void setFlocageActif(Boolean flocageActif) { this.flocageActif = flocageActif; }

    public String getFlocage() { return flocage; }
    public void setFlocage(String flocage) { this.flocage = flocage; }

    public Double getPrixUnitaire() { return prixUnitaire; }
    public void setPrixUnitaire(Double prixUnitaire) { this.prixUnitaire = prixUnitaire; }

    public Double getPrixTotal() { return prixTotal; }
    public void setPrixTotal(Double prixTotal) { this.prixTotal = prixTotal; }

    public Long getBeneficiaireId() { return beneficiaireId; }
    public void setBeneficiaireId(Long beneficiaireId) { this.beneficiaireId = beneficiaireId; }
}
