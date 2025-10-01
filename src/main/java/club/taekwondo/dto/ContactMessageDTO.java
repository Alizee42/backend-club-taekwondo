package club.taekwondo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ContactMessageDTO {
    @NotBlank(message = "Le nom est requis")
    @Size(max = 120, message = "Nom trop long")
    private String name;

    @NotBlank(message = "L'email est requis")
    @Email(message = "Email invalide")
    private String email;

    @NotBlank(message = "L'objet est requis")
    @Size(max = 180, message = "Objet trop long")
    private String objet;

    @NotBlank(message = "Le message est requis")
    @Size(min = 10, max = 4000, message = "Le message doit faire entre 10 et 4000 caractères")
    private String message;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getObjet() { return objet; }
    public void setObjet(String objet) { this.objet = objet; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
