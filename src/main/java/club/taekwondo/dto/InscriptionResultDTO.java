package club.taekwondo.dto;

import java.util.List;

public class InscriptionResultDTO {

    private Long id;
    private String email;
    private String role;
    private boolean emailSent;
    private List<MembreDTO> membres;

    public InscriptionResultDTO() {
    }

    public InscriptionResultDTO(Long id, String email, String role, boolean emailSent, List<MembreDTO> membres) {
        this.id = id;
        this.email = email;
        this.role = role;
        this.emailSent = emailSent;
        this.membres = membres;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isEmailSent() {
        return emailSent;
    }

    public void setEmailSent(boolean emailSent) {
        this.emailSent = emailSent;
    }

    public List<MembreDTO> getMembres() {
        return membres;
    }

    public void setMembres(List<MembreDTO> membres) {
        this.membres = membres;
    }
}
