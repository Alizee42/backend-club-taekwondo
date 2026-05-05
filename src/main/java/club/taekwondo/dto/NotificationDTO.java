package club.taekwondo.dto;

import java.time.LocalDateTime;

public class NotificationDTO {

    private Long id;
    private String titre;
    private String message;
    private String type;
    private boolean lu;
    private LocalDateTime date; // Renommer pour correspondre au frontend
    private Long utilisateurId;
    private String lienAction;

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }
    
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isLu() {
        return lu;
    }

    public void setLu(boolean lu) {
        this.lu = lu;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public Long getUtilisateurId() {
        return utilisateurId;
    }

    public void setUtilisateurId(Long utilisateurId) {
        this.utilisateurId = utilisateurId;
    }

    public String getLienAction() {
        return lienAction;
    }

    public void setLienAction(String lienAction) {
        this.lienAction = lienAction;
    }
}
