package club.taekwondo.entity.mongo;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "actualite")
public class Actualite {

    @Id
    private String id;

    private String titre;
    private String contenu;
    private LocalDateTime datePublication;
    private String typeActu;

    @JsonProperty("isFeatured") // Mappe "isFeatured" dans le JSON au champ Java
    private boolean isFeatured;

    private String imageUrl;

    public Actualite() {}

    public Actualite(String titre, String contenu, LocalDateTime datePublication, String typeActu, boolean isFeatured, String imageUrl) {
        this.titre = titre;
        this.contenu = contenu;
        this.datePublication = datePublication;
        this.typeActu = typeActu;
        this.isFeatured = isFeatured;
        this.imageUrl = imageUrl;
    }

    // Getters et setters

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

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public LocalDateTime getDatePublication() {
        return datePublication;
    }

    public void setDatePublication(LocalDateTime datePublication) {
        this.datePublication = datePublication;
    }

    public String getTypeActu() {
        return typeActu;
    }

    public void setTypeActu(String typeActu) {
        this.typeActu = typeActu;
    }

    public boolean isFeatured() {
        return isFeatured;
    }

    public void setFeatured(boolean isFeatured) {
        this.isFeatured = isFeatured;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}