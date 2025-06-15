package club.taekwondo.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CommandeDTO {

    private Long id;
    private LocalDate dateCommande;
    private BigDecimal montantTotal;
    private Long utilisateurId;
	public Long getId() {
		return id;
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
	public Long getUtilisateurId() {
		return utilisateurId;
	}
	public void setUtilisateurId(Long utilisateurId) {
		this.utilisateurId = utilisateurId;
	}

    
}
