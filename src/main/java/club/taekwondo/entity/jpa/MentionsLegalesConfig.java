package club.taekwondo.entity.jpa;

import jakarta.persistence.*;

@Entity
@Table(name = "mentions_legales_config")
public class MentionsLegalesConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false, unique = true)
    private Club club;

    // Éditeur du site
    private String nomAssociation;
    private String statutJuridique;
    private String adresse;
    private String numeroRna;
    private String numeroSiren;
    private String representantLegal;
    private String email;
    private String telephone;

    // Hébergeur
    private String hebergeurNom;
    private String hebergeurAdresse;
    private String hebergeurSiteWeb;

    // Médiation — vide = "aucun médiateur conventionné"
    private String mediateurNom;
    private String mediateurContact;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Club getClub() { return club; }
    public void setClub(Club club) { this.club = club; }
    public String getNomAssociation() { return nomAssociation; }
    public void setNomAssociation(String v) { this.nomAssociation = v; }
    public String getStatutJuridique() { return statutJuridique; }
    public void setStatutJuridique(String v) { this.statutJuridique = v; }
    public String getAdresse() { return adresse; }
    public void setAdresse(String v) { this.adresse = v; }
    public String getNumeroRna() { return numeroRna; }
    public void setNumeroRna(String v) { this.numeroRna = v; }
    public String getNumeroSiren() { return numeroSiren; }
    public void setNumeroSiren(String v) { this.numeroSiren = v; }
    public String getRepresentantLegal() { return representantLegal; }
    public void setRepresentantLegal(String v) { this.representantLegal = v; }
    public String getEmail() { return email; }
    public void setEmail(String v) { this.email = v; }
    public String getTelephone() { return telephone; }
    public void setTelephone(String v) { this.telephone = v; }
    public String getHebergeurNom() { return hebergeurNom; }
    public void setHebergeurNom(String v) { this.hebergeurNom = v; }
    public String getHebergeurAdresse() { return hebergeurAdresse; }
    public void setHebergeurAdresse(String v) { this.hebergeurAdresse = v; }
    public String getHebergeurSiteWeb() { return hebergeurSiteWeb; }
    public void setHebergeurSiteWeb(String v) { this.hebergeurSiteWeb = v; }
    public String getMediateurNom() { return mediateurNom; }
    public void setMediateurNom(String v) { this.mediateurNom = v; }
    public String getMediateurContact() { return mediateurContact; }
    public void setMediateurContact(String v) { this.mediateurContact = v; }
}
