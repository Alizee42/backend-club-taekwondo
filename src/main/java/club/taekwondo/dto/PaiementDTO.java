package club.taekwondo.dto;

import java.time.LocalDate;
import java.util.List;

public class PaiementDTO {

	private Long id;
	private String type;
	private Double montant;
	private LocalDate datePaiement;
	private String statut;
	private String modePaiement;
	private Long utilisateurId;
	private List<EcheanceDTO> echeances;

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

	public Double getMontant() {
		return montant;
	}

	public void setMontant(Double montant) {
		this.montant = montant;
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

}
