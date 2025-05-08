package club.taekwondo.entity.jpa;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Access(AccessType.FIELD)
@Table(name = "document")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "type_document", length = 50)
    private String typeDocument;

    @Column(name = "nom_document", length = 100)
    private String nomDocument;

    @Column(name = "chemin_fichier", length = 255)
    private String cheminFichier; // Chemin ou nom du fichier réel sur le disque

    @Column(name = "date_depot", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime dateDepot;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Clé étrangère pour lier Document à Membre
    @ManyToOne
    @JoinColumn(name = "membre_id", referencedColumnName = "id")
    private Membre membre;

    // Constructeur par défaut
    public Document() {
        this.dateDepot = LocalDateTime.now(); // Initialiser avec la date et l'heure actuelles
    }

    // Constructeur avec paramètres
    public Document(Long id, String typeDocument, String nomDocument, String cheminFichier, LocalDateTime dateDepot, String description, Membre membre) {
        this.id = id;
        this.typeDocument = typeDocument;
        this.nomDocument = nomDocument;
        this.cheminFichier = cheminFichier;
        this.dateDepot = dateDepot;
        this.description = description;
        this.membre = membre;
    }

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTypeDocument() {
        return typeDocument;
    }

    public void setTypeDocument(String typeDocument) {
        this.typeDocument = typeDocument;
    }

    public String getNomDocument() {
        return nomDocument;
    }

    public void setNomDocument(String nomDocument) {
        this.nomDocument = nomDocument;
    }

    public String getCheminFichier() {
        return cheminFichier;
    }

    public void setCheminFichier(String cheminFichier) {
        this.cheminFichier = cheminFichier;
    }

    public LocalDateTime getDateDepot() {
        return dateDepot;
    }

    public void setDateDepot(LocalDateTime dateDepot) {
        this.dateDepot = dateDepot;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Membre getMembre() {
        return membre;
    }

    public void setMembre(Membre membre) {
        this.membre = membre;
    }
}