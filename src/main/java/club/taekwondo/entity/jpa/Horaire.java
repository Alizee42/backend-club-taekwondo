package club.taekwondo.entity.jpa;

import jakarta.persistence.*;

@Entity
public class Horaire {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String jour;
    private String heureDebut;
    private String heureFin;
    private String groupe;
    private String adresse;
    private String codePostal;
    private String ville;

    @ManyToOne
    @JoinColumn(name = "club_id")
    private Club club;

    // Getters et setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getJour() { return jour; }
    public void setJour(String jour) { this.jour = jour; }
    public String getHeureDebut() { return heureDebut; }
    public void setHeureDebut(String heureDebut) { this.heureDebut = heureDebut; }
    public String getHeureFin() { return heureFin; }
    public void setHeureFin(String heureFin) { this.heureFin = heureFin; }
    public String getGroupe() { return groupe; }
    public void setGroupe(String groupe) { this.groupe = groupe; }
    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }
    public String getCodePostal() { return codePostal; }
    public void setCodePostal(String codePostal) { this.codePostal = codePostal; }
    public String getVille() { return ville; }
    public void setVille(String ville) { this.ville = ville; }
    public Club getClub() { return club; }
    public void setClub(Club club) { this.club = club; }
}
