package club.taekwondo.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class CommandeDTO {

    private Long id;

    private LocalDate dateCommande;

    @NotNull(message = "Le montant total est obligatoire")
    @PositiveOrZero(message = "Le montant total doit être positif ou nul")
    private BigDecimal montantTotal;

    @NotBlank(message = "Le mode de paiement est obligatoire")
    private String modePaiement;

    private LocalDate datePaiement;

    @NotBlank(message = "Le statut est obligatoire")
    private String statut;

    private Boolean disponibleAuClub;

    @NotNull(message = "L'utilisateur est obligatoire")
    private Long utilisateurId;

    private UtilisateurCommandeDTO utilisateur;

    @NotNull(message = "La commande doit contenir au moins une ligne")
    @Size(min = 1, message = "La commande doit contenir au moins une ligne")
    private List<LigneCommandeDTO> lignesCommande;

    // --- Getters & Setters ---
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

    public List<LigneCommandeDTO> getLignesCommande() {
        return lignesCommande;
    }

    public void setLignesCommande(List<LigneCommandeDTO> lignesCommande) {
        this.lignesCommande = lignesCommande;
    }

    public Boolean getDisponibleAuClub() {
        return disponibleAuClub;
    }

    public void setDisponibleAuClub(Boolean disponibleAuClub) {
        this.disponibleAuClub = disponibleAuClub;
    }

    public LocalDate getDatePaiement() {
        return datePaiement;
    }

    public void setDatePaiement(LocalDate datePaiement) {
        this.datePaiement = datePaiement;
    }
}