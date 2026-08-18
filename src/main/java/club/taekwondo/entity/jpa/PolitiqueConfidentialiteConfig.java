package club.taekwondo.entity.jpa;

import jakarta.persistence.*;

@Entity
@Table(name = "politique_confidentialite_config")
public class PolitiqueConfidentialiteConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false, unique = true)
    private Club club;

    // Responsable du traitement — le reste du texte RGPD (finalités, durées,
    // droits, cookies, mineurs...) est commun à tous les clubs, non éditable ici.
    private String nomAssociation;
    private String adresse;
    private String emailContact;
    private String emailRgpd;
    private String representantLegal;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Club getClub() { return club; }
    public void setClub(Club club) { this.club = club; }
    public String getNomAssociation() { return nomAssociation; }
    public void setNomAssociation(String v) { this.nomAssociation = v; }
    public String getAdresse() { return adresse; }
    public void setAdresse(String v) { this.adresse = v; }
    public String getEmailContact() { return emailContact; }
    public void setEmailContact(String v) { this.emailContact = v; }
    public String getEmailRgpd() { return emailRgpd; }
    public void setEmailRgpd(String v) { this.emailRgpd = v; }
    public String getRepresentantLegal() { return representantLegal; }
    public void setRepresentantLegal(String v) { this.representantLegal = v; }
}
