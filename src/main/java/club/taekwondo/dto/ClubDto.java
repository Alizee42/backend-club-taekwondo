package club.taekwondo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class ClubDto {
    private Long id;

    @NotBlank(message = "Le nom du club est obligatoire")
    private String name;

    @NotBlank(message = "L'adresse du club est obligatoire")
    private String adresse;

    @NotBlank(message = "Le telephone du club est obligatoire")
    private String telephone;

    @NotBlank(message = "L'email du club est obligatoire")
    @Email(message = "L'email du club est invalide")
    private String email;

    private String logo;
    private String rib;
    private String stripeAccountId;
    private boolean stripeChargesEnabled;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }
    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getLogo() { return logo; }
    public void setLogo(String logo) { this.logo = logo; }
    public String getRib() { return rib; }
    public void setRib(String rib) { this.rib = rib; }
    public String getStripeAccountId() { return stripeAccountId; }
    public void setStripeAccountId(String stripeAccountId) { this.stripeAccountId = stripeAccountId; }
    public boolean isStripeChargesEnabled() { return stripeChargesEnabled; }
    public void setStripeChargesEnabled(boolean stripeChargesEnabled) { this.stripeChargesEnabled = stripeChargesEnabled; }
}
