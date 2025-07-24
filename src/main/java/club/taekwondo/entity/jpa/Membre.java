package club.taekwondo.entity.jpa;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "membre")
@Access(AccessType.FIELD)
public class Membre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    private String ceinture;

    @Column(unique = true)
    private String numeroLicence;

    private LocalDate dateNaissance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Utilisateur parent;

    @OneToOne
    @JoinColumn(name = "compte_utilisateur_id", unique = true)
    private Utilisateur compteUtilisateur;

    public Membre() {}

    public Membre(String nom, String prenom, LocalDate dateNaissance, String ceinture, String numeroLicence,
                  Utilisateur parent, Utilisateur compteUtilisateur) {
        this.nom = nom;
        this.prenom = prenom;
        this.dateNaissance = dateNaissance;
        this.ceinture = ceinture;
        this.numeroLicence = numeroLicence;
        this.parent = parent;
        this.compteUtilisateur = compteUtilisateur;
    }
    // --- Getters & Setters ---
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getCeinture() {
        return ceinture;
    }

    public void setCeinture(String ceinture) {
        this.ceinture = ceinture;
    }

    public String getNumeroLicence() {
        return numeroLicence;
    }

    public void setNumeroLicence(String numeroLicence) {
        this.numeroLicence = numeroLicence;
    }

    public LocalDate getDateNaissance() {
        return dateNaissance;
    }

    public void setDateNaissance(LocalDate dateNaissance) {
        this.dateNaissance = dateNaissance;
    }

    public Utilisateur getParent() {
        return parent;
    }

    public void setParent(Utilisateur parent) {
        this.parent = parent;
    }

    public Utilisateur getCompteUtilisateur() {
        return compteUtilisateur;
    }

    public void setCompteUtilisateur(Utilisateur compteUtilisateur) {
        this.compteUtilisateur = compteUtilisateur;
    }
}
