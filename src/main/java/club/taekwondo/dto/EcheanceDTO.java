package club.taekwondo.dto;

import java.time.LocalDate;

public class EcheanceDTO {

	private Long id;
	private LocalDate dateEcheance;
	private Double montant;
	private String statut;
	private Integer numero; 

	// Getters & Setters
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public LocalDate getDateEcheance() {
		return dateEcheance;
	}

	public void setDateEcheance(LocalDate dateEcheance) {
		this.dateEcheance = dateEcheance;
	}

	public Double getMontant() {
		return montant;
	}
	
	public void setMontant(Double montant) {
		this.montant = montant;
	}

	public String getStatut() {
		return statut;
	}

	public void setStatut(String statut) {
		this.statut = statut;
	}

	public Integer getNumero() {
		return numero;
	}

	public void setNumero(Integer numero) {
		this.numero = numero;
	}
	
	@Override
	public String toString() {
	    return "EcheanceDTO{" +
	            "id=" + id +
	            ", numero=" + numero +
	            ", dateEcheance=" + dateEcheance +
	            ", montant=" + montant +
	            ", statut='" + statut + '\'' +
	            '}';
	}
}

