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

    @Column(name = "ceinture")
    private String ceinture;

    // ✅ nullable=true pour autoriser plusieurs NULL malgré l'unicité
    @Column(name = "numero_licence", unique = true, nullable = true, length = 255)
    private String numeroLicence;

    private LocalDate dateNaissance;

    @Column(nullable = false)
    private boolean estAdulte;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Utilisateur parent;

    @OneToOne
    @JoinColumn(name = "compte_utilisateur_id", unique = true)
    private Utilisateur compteUtilisateur;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id")
    private Club club;

    public Membre() {}

    public Membre(String nom, String prenom, LocalDate dateNaissance, String ceinture, String numeroLicence,
                  boolean estAdulte, Utilisateur parent, Utilisateur compteUtilisateur) {
        this.setNom(nom);
        this.setPrenom(prenom);
        this.setDateNaissance(dateNaissance);
        this.setCeinture(ceinture);
        this.setNumeroLicence(numeroLicence);
        this.estAdulte = estAdulte;
        this.parent = parent;
        this.compteUtilisateur = compteUtilisateur;
    }

    // --- Normalisation automatiques avant INSERT/UPDATE ---
    @PrePersist
    @PreUpdate
    private void normalize() {
        if (nom != null) nom = nom.trim();
        if (prenom != null) prenom = prenom.trim();
        if (ceinture != null) ceinture = ceinture.trim();

        if (numeroLicence != null) {
            numeroLicence = numeroLicence.trim();
            if (numeroLicence.isEmpty()) {
                // ⚠️ point clé : on convertit '' en NULL pour ne pas violer l'unicité
                numeroLicence = null;
            }
        }
    }

    // --- Getters & Setters ---
    public Club getClub() { return club; }
    public void setClub(Club club) { this.club = club; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom != null ? nom.trim() : null; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom != null ? prenom.trim() : null; }

    public String getCeinture() { return ceinture; }
    public void setCeinture(String ceinture) { this.ceinture = ceinture != null ? ceinture.trim() : null; }

    public String getNumeroLicence() { return numeroLicence; }
    public void setNumeroLicence(String numeroLicence) {
        if (numeroLicence == null) {
            this.numeroLicence = null;
        } else {
            String v = numeroLicence.trim();
            this.numeroLicence = v.isEmpty() ? null : v;
        }
    }

    public LocalDate getDateNaissance() { return dateNaissance; }
    public void setDateNaissance(LocalDate dateNaissance) { this.dateNaissance = dateNaissance; }

    public boolean isEstAdulte() { return estAdulte; }
    public void setEstAdulte(boolean estAdulte) { this.estAdulte = estAdulte; }

    public Utilisateur getParent() { return parent; }
    public void setParent(Utilisateur parent) { this.parent = parent; }

    public Utilisateur getCompteUtilisateur() { return compteUtilisateur; }
    public void setCompteUtilisateur(Utilisateur compteUtilisateur) { this.compteUtilisateur = compteUtilisateur; }
}
