package club.taekwondo.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class PaiementRequestDTO {

    @NotNull(message = "Le montant est obligatoire.")
    @Min(value = 1, message = "Le montant doit être supérieur à zéro.")
    private Double amount;

    @NotBlank(message = "La devise est obligatoire.")
    private String currency;

    @NotBlank(message = "Le mode de paiement est obligatoire.")
    private String modePaiement;

    @NotBlank(message = "Le type de paiement est obligatoire.")
    private String typePaiement;

    @Min(value = 1, message = "Il doit y avoir au moins une échéance.")
    private int nombreEcheances;

    private Long utilisateurId; // optionnel, pas besoin de validation ici car extrait du token

    // --- Getters & Setters ---

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getModePaiement() {
        return modePaiement;
    }

    public void setModePaiement(String modePaiement) {
        this.modePaiement = modePaiement;
    }

    public String getTypePaiement() {
        return typePaiement;
    }

    public void setTypePaiement(String typePaiement) {
        this.typePaiement = typePaiement;
    }

    public int getNombreEcheances() {
        return nombreEcheances;
    }

    public void setNombreEcheances(int nombreEcheances) {
        this.nombreEcheances = nombreEcheances;
    }

    public Long getUtilisateurId() {
        return utilisateurId;
    }

    public void setUtilisateurId(Long utilisateurId) {
        this.utilisateurId = utilisateurId;
    }
}
