package club.taekwondo.entity.jpa;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Access(AccessType.FIELD)
@Table(name = "document")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String typeDocument;
    private String nomDocument;
    private String dateDepot;
    private String description;

    // Clé étrangère pour lier Document à Membre
    @ManyToOne
    @JoinColumn(name = "membre_id", referencedColumnName = "id")
    private Membre membre;

    // Constructeur par défaut
    public Document() {
    }

    // Constructeur avec paramètres
    public Document(Long id, String typeDocument, String nomDocument, String dateDepot, String description, Membre membre) {
        this.id = id;
        this.typeDocument = typeDocument;
        this.nomDocument = nomDocument;
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

    public String getDateDepot() {
        return dateDepot;
    }

    public void setDateDepot(String dateDepot) {
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
