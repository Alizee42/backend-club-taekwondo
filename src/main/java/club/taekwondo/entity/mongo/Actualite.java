package club.taekwondo.entity.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;

import java.time.LocalDateTime;
@Access(AccessType.FIELD)
@Document(collection = "actualite")
public class Actualite {

    @Id
    private String id;

    private String titre;
    private String contenu;
    private LocalDateTime datePublication;
    private String typeActu;

    public Actualite() {}

    public Actualite(String titre, String contenu, LocalDateTime datePublication, String typeActu) {
        this.titre = titre;
        this.contenu = contenu;
        this.datePublication = datePublication;
        this.typeActu = typeActu;
    }

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
}
