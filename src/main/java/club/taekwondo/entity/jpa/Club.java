package club.taekwondo.entity.jpa;

import jakarta.persistence.*;

@Entity
@Table(name = "clubs")
public class Club {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private String adresse;
    private String telephone;
    private String email;
    private String logo;
    private String rib;

    @Column(name = "stripe_account_id")
    private String stripeAccountId;

    @Column(name = "stripe_charges_enabled", nullable = false)
    private boolean stripeChargesEnabled = false;

    // Getters & Setters
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
