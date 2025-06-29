package club.taekwondo.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class PaiementDTO {

	private Long id;
	private String type;
	private LocalDate datePaiement;
	private String statut;
	private String modePaiement;
	private Long utilisateurId;
	private List<EcheanceDTO> echeances;
	private Double montantTotal;
	private Double montantRestant;
	private Double montantPaye; 
	private String utilisateurNom;
	private String utilisateurPrenom;
	private String utilisateurEmail;
	private String motifAnnulation;
	private LocalDateTime dateAnnulation;
	private String adminResponsable;


	// Getters et Setters
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public LocalDate getDatePaiement() {
		return datePaiement;
	}

	public void setDatePaiement(LocalDate datePaiement) {
		this.datePaiement = datePaiement;
	}

	public String getStatut() {
		return statut;
	}

	public void setStatut(String statut) {
		this.statut = statut;
	}

	public String getModePaiement() {
		return modePaiement;
	}

	public void setModePaiement(String modePaiement) {
		this.modePaiement = modePaiement;
	}

	public Long getUtilisateurId() {
		return utilisateurId;
	}

	public void setUtilisateurId(Long utilisateurId) {
		this.utilisateurId = utilisateurId;
	}

	public List<EcheanceDTO> getEcheances() {
		return echeances;
	}

	public void setEcheances(List<EcheanceDTO> echeances) {
		this.echeances = echeances;
	}

	public Double getMontantTotal() {
		return montantTotal;
	}

	public void setMontantTotal(Double montantTotal) {
		this.montantTotal = montantTotal;
	}

	public Double getMontantRestant() {
		return montantRestant;
	}

	public void setMontantRestant(Double montantRestant) {
		this.montantRestant = montantRestant;
	}

	public Double getMontantPaye() {
		return montantPaye;
	}

	public void setMontantPaye(Double montantPaye) {
		this.montantPaye = montantPaye;
	}

	public String getUtilisateurNom() {
		return utilisateurNom;
	}

	public void setUtilisateurNom(String utilisateurNom) {
		this.utilisateurNom = utilisateurNom;
	}

	public String getUtilisateurPrenom() {
		return utilisateurPrenom;
	}

	public void setUtilisateurPrenom(String utilisateurPrenom) {
		this.utilisateurPrenom = utilisateurPrenom;
	}

	public String getUtilisateurEmail() {
		return utilisateurEmail;
	}

	public void setUtilisateurEmail(String utilisateurEmail) {
		this.utilisateurEmail = utilisateurEmail;
	}

	public String getMotifAnnulation() {
		return motifAnnulation;
	}

	public void setMotifAnnulation(String motifAnnulation) {
		this.motifAnnulation = motifAnnulation;
	}

	public LocalDateTime getDateAnnulation() {
		return dateAnnulation;
	}

	public void setDateAnnulation(LocalDateTime dateAnnulation) {
		this.dateAnnulation = dateAnnulation;
	}

	public String getAdminResponsable() {
		return adminResponsable;
	}

	public void setAdminResponsable(String adminResponsable) {
		this.adminResponsable = adminResponsable;
	}
	
}

