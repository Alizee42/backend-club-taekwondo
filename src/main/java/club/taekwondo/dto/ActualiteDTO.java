package club.taekwondo.dto;

import java.time.LocalDateTime;

public class ActualiteDTO {

    private String id;
    private String titre;
    private String contenu;
    private LocalDateTime datePublication;
    private String typeActu;
    private boolean isFeatured;
    private String imageUrl;

    // Constructeurs
    public ActualiteDTO() {}

    public ActualiteDTO(String id, String titre, String contenu, LocalDateTime datePublication, String typeActu, boolean isFeatured, String imageUrl) {
        this.id = id;
        this.titre = titre;
        this.contenu = contenu;
        this.datePublication = datePublication;
        this.typeActu = typeActu;
        this.isFeatured = isFeatured;
        this.imageUrl = imageUrl;
    }

    // Getters et Setters
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

    public void setFeatured(boolean featured) {
        isFeatured = featured;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
