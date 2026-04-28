package club.taekwondo.entity.jpa;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "campagne_commande")
public class CampagneCommande {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titre;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "date_ouverture", nullable = false)
    private LocalDate dateOuverture;

    @Column(name = "date_fermeture", nullable = false)
    private LocalDate dateFermeture;

    @Column(nullable = false)
    private boolean actif = true;

    @ManyToOne
    @JoinColumn(name = "club_id")
    private Club club;

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

    public Club getClub() { return club; }
    public void setClub(Club club) { this.club = club; }
}
