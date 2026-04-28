package club.taekwondo.dto;

import java.time.LocalDate;

public class CampagneCommandeDTO {

    private Long id;
    private String titre;
    private String description;
    private LocalDate dateOuverture;
    private LocalDate dateFermeture;
    private boolean actif;
    private Long clubId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getDateOuverture() { return dateOuverture; }
    public void setDateOuverture(LocalDate dateOuverture) { this.dateOuverture = dateOuverture; }

    public LocalDate getDateFermeture() { return dateFermeture; }
    public void setDateFermeture(LocalDate dateFermeture) { this.dateFermeture = dateFermeture; }

    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }

    public Long getClubId() { return clubId; }
    public void setClubId(Long clubId) { this.clubId = clubId; }
}
