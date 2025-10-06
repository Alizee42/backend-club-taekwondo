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
}
