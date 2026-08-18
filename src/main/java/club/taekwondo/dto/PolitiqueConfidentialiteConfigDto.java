package club.taekwondo.dto;

public class PolitiqueConfidentialiteConfigDto {

    private Long clubId;
    private String nomAssociation;
    private String adresse;
    private String emailContact;
    private String emailRgpd;
    private String representantLegal;

    public Long getClubId() { return clubId; }
    public void setClubId(Long v) { this.clubId = v; }
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
