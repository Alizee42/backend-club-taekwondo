package club.taekwondo.entity.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;

import java.time.LocalDateTime;
@Access(AccessType.FIELD)
@Document(collection = "galerie")
public class Galerie {

    @Id
    private String id;

    private String titre;
    private String imageUrl;
    private String description;
    private LocalDateTime datePublication;

    // === Constructeurs ===

    public Galerie() {
        // Constructeur vide requis par Spring
    }

    public Galerie(String titre, String imageUrl, String description, LocalDateTime datePublication) {
        this.titre = titre;
        this.imageUrl = imageUrl;
        this.description = description;
        this.datePublication = datePublication;
    }

    // === Getters et Setters ===

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getDatePublication() {
        return datePublication;
    }

    public void setDatePublication(LocalDateTime datePublication) {
        this.datePublication = datePublication;
    }

    // === toString (optionnel mais pratique pour debug) ===

    @Override
    public String toString() {
        return "Galerie{" +
                "id='" + id + '\'' +
                ", titre='" + titre + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", description='" + description + '\'' +
                ", datePublication=" + datePublication +
                '}';
    }
}
