package club.taekwondo.entity.jpa;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Access(AccessType.FIELD)
@Table(name = "avis")
public class Avis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "date_pub", nullable = false)
    private LocalDate datePub;

    @Column(name = "contenu", nullable = false, columnDefinition = "TEXT")
    private String contenu;

    @Column(name = "approuve", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean approuve = false;

    @Column(name = "pseudo_visiteur", length = 100)
    private String pseudoVisiteur;

    @Column(name = "note", columnDefinition = "TINYINT")
    private Integer note; // Note sur 5 étoiles

    @Column(name = "type_avis", length = 50)
    private String typeAvis; // Type d'avis : "cours", "événement", etc.
    
    @Column(name = "photo", nullable = true)
    private String photo;
    
    @ManyToOne(optional = true)
    @JoinColumn(name = "utilisateur_id", nullable = true)
    private Utilisateur utilisateur;


    // Constructeurs
    public Avis() {}

    public Avis(LocalDate datePub, String contenu, Boolean approuve, String pseudoVisiteur, Integer note, String typeAvis) {
        this.datePub = datePub;
        this.contenu = contenu;
        this.approuve = approuve;
        this.pseudoVisiteur = pseudoVisiteur;
        this.note = note;
        this.typeAvis = typeAvis;
    }

    // Getters et Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public LocalDate getDatePub() {
        return datePub;
    }

    public void setDatePub(LocalDate datePub) {
        this.datePub = datePub;
    }

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public Boolean getApprouve() {
        return approuve;
    }

    public void setApprouve(Boolean approuve) {
        this.approuve = approuve;
    }

    public String getPseudoVisiteur() {
        return pseudoVisiteur;
    }

    public void setPseudoVisiteur(String pseudoVisiteur) {
        this.pseudoVisiteur = pseudoVisiteur;
    }

    public Integer getNote() {
        return note;
    }

    public void setNote(Integer note) {
        this.note = note;
    }

    public String getTypeAvis() {
        return typeAvis;
    }

    public void setTypeAvis(String typeAvis) {
        this.typeAvis = typeAvis;
    }
    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }
    public Utilisateur getUtilisateur() {
        return utilisateur;
    }

    public void setUtilisateur(Utilisateur utilisateur) {
        this.utilisateur = utilisateur;
    }
}