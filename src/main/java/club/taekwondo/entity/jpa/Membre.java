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

    // 🔹 Lien vers le parent si le membre est un enfant
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;

    // 🔹 Lien vers le compte utilisateur si le membre est un adulte
    @OneToOne
    @JoinColumn(name = "compte_utilisateur_id", unique = true)
    private Utilisateur compteUtilisateur;

    // --- Constructeurs ---
    public Membre() {}

    public Membre(String nom, String prenom, LocalDate dateNaissance, String ceinture, String numeroLicence,
                  Utilisateur utilisateur, Utilisateur compteUtilisateur) {
        this.nom = nom;
        this.prenom = prenom;
        this.dateNaissance = dateNaissance;
        this.ceinture = ceinture;
        this.numeroLicence = numeroLicence;
        this.utilisateur = utilisateur;
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

    public Utilisateur getUtilisateur() {
        return utilisateur;
    }

    public void setUtilisateur(Utilisateur utilisateur) {
        this.utilisateur = utilisateur;
    }

    public Utilisateur getCompteUtilisateur() {
        return compteUtilisateur;
    }

    public void setCompteUtilisateur(Utilisateur compteUtilisateur) {
        this.compteUtilisateur = compteUtilisateur;
    }
}

