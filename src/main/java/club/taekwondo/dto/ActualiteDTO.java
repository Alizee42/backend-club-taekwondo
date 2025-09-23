package club.taekwondo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL) // ✅ ne sérialise pas les champs null
public class ActualiteDTO {

    private String id;
    private String titre;
    private String contenu;
    private LocalDateTime datePublication;

    private String typeActu; // devient optionnel

    @JsonProperty("isFeatured")
    private boolean isFeatured;

    private String imageUrl;

    // Constructeurs
    public ActualiteDTO() {}

    public ActualiteDTO(
        String id,
        String titre,
        String contenu,
        LocalDateTime datePublication,
        String typeActu,
        boolean isFeatured,
        String imageUrl
    ) {
        this.id = id;
        this.titre = titre;
        this.contenu = contenu;
        this.datePublication = datePublication;
        this.typeActu = typeActu;
        this.isFeatured = isFeatured;
        this.imageUrl = imageUrl;
    }

    // Getters et Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }

    public LocalDateTime getDatePublication() { return datePublication; }
    public void setDatePublication(LocalDateTime datePublication) { this.datePublication = datePublication; }

    public String getTypeActu() { return typeActu; }
    public void setTypeActu(String typeActu) { this.typeActu = typeActu; }

    public boolean isFeatured() { return isFeatured; }
    public void setFeatured(boolean featured) { isFeatured = featured; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}

