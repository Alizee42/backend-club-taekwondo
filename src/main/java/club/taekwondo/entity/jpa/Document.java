package club.taekwondo.entity.jpa;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Access(AccessType.FIELD)
@Table(name = "document")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "type_document", length = 50, nullable = false)
    private String typeDocument;

    @Column(name = "nom_document", length = 100, nullable = false)
    private String nomDocument;

    @Column(name = "chemin_fichier", length = 255, nullable = false)
    private String cheminFichier;

    @Column(name = "date_depot", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime dateDepot;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "status", length = 20, nullable = false)
    private String status;

    // Relation existante: Document -> Utilisateur (obligatoire)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    @JsonBackReference // Empêche la sérialisation récursive
    private Utilisateur utilisateur;

    // ✅ NOUVEAU: Document -> Membre (optionnel)
    // On met @JsonIgnore côté Document pour éviter toute boucle JSON
    // (tu peux remplacer par @JsonBackReference/@JsonManagedReference si tu exposes la liste dans Membre)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membre_id") // nullable par défaut
    @JsonIgnore
    private Membre membre;

    public Document() {
        this.dateDepot = LocalDateTime.now();
        this.status = "en attente"; // on conserve ta convention actuelle
    }

    // Getters / Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTypeDocument() { return typeDocument; }
    public void setTypeDocument(String typeDocument) { this.typeDocument = typeDocument; }

    public String getNomDocument() { return nomDocument; }
    public void setNomDocument(String nomDocument) { this.nomDocument = nomDocument; }

    public String getCheminFichier() { return cheminFichier; }
    public void setCheminFichier(String cheminFichier) { this.cheminFichier = cheminFichier; }

    public LocalDateTime getDateDepot() { return dateDepot; }
    public void setDateDepot(LocalDateTime dateDepot) { this.dateDepot = dateDepot; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Utilisateur getUtilisateur() { return utilisateur; }
    public void setUtilisateur(Utilisateur utilisateur) { this.utilisateur = utilisateur; }

    public Membre getMembre() { return membre; }
    public void setMembre(Membre membre) { this.membre = membre; }
}
