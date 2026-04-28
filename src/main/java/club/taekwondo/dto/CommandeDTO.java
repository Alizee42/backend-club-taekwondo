package club.taekwondo.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class CommandeDTO {

    private Long id;

    private Long clubId;

    private LocalDate dateCommande;

    @NotNull(message = "Le montant total est obligatoire")
    @PositiveOrZero(message = "Le montant total doit être positif ou nul")
    private BigDecimal montantTotal;

    @NotBlank(message = "Le mode de paiement est obligatoire")
    private String modePaiement;

    private LocalDate datePaiement;

    @NotBlank(message = "Le statut est obligatoire")
    private String statut;


    @NotNull(message = "L'utilisateur est obligatoire")
    private Long utilisateurId;

    private UtilisateurCommandeDTO utilisateur;

    private Long campagneId;
    private String campagneTitre;

    private Long beneficiaireId;
    private String beneficiairePrenom;
    private String beneficiaireNom;
    
    @NotNull(message = "La commande doit contenir au moins une ligne")
    @Size(min = 1, message = "La commande doit contenir au moins une ligne")
    private List<LigneCommandeDTO> lignesCommande;

    // --- Getters & Setters ---
    public Long getId() {
        return id;
    }

    public Long getClubId() {
        return clubId;
    }

    public void setClubId(Long clubId) {
        this.clubId = clubId;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getDateCommande() {
        return dateCommande;
    }

    public void setDateCommande(LocalDate dateCommande) {
        this.dateCommande = dateCommande;
    }

    public BigDecimal getMontantTotal() {
        return montantTotal;
    }

    public void setMontantTotal(BigDecimal montantTotal) {
        this.montantTotal = montantTotal;
    }

    public String getModePaiement() {
        return modePaiement;
    }

    public void setModePaiement(String modePaiement) {
        this.modePaiement = modePaiement;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public Long getUtilisateurId() {
        return utilisateurId;
    }

    public void setUtilisateurId(Long utilisateurId) {
        this.utilisateurId = utilisateurId;
    }

    public UtilisateurCommandeDTO getUtilisateur() {
        return utilisateur;
    }

    public void setUtilisateur(UtilisateurCommandeDTO utilisateur) {
        this.utilisateur = utilisateur;
    }

    
    public Long getCampagneId() { return campagneId; }
    public void setCampagneId(Long campagneId) { this.campagneId = campagneId; }

    public String getCampagneTitre() { return campagneTitre; }
    public void setCampagneTitre(String campagneTitre) { this.campagneTitre = campagneTitre; }

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

	public List<LigneCommandeDTO> getLignesCommande() {
        return lignesCommande;
    }

    public void setLignesCommande(List<LigneCommandeDTO> lignesCommande) {
        this.lignesCommande = lignesCommande;
    }

    public LocalDate getDatePaiement() {
        return datePaiement;
    }

    public void setDatePaiement(LocalDate datePaiement) {
        this.datePaiement = datePaiement;
    }
}