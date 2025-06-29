package club.taekwondo.dto;

import java.time.LocalDateTime;

public class AnnulationRequestDTO {

    private String motif;
    private LocalDateTime dateAnnulation;
    private String adminResponsable;

    public AnnulationRequestDTO() {
    }

    public AnnulationRequestDTO(String motif, LocalDateTime dateAnnulation, String adminResponsable) {
        this.motif = motif;
        this.dateAnnulation = dateAnnulation;
        this.adminResponsable = adminResponsable;
    }

    public String getMotif() {
        return motif;
    }

    public void setMotif(String motif) {
        this.motif = motif;
    }

    public LocalDateTime getDateAnnulation() {
        return dateAnnulation;
    }

    public void setDateAnnulation(LocalDateTime dateAnnulation) {
        this.dateAnnulation = dateAnnulation;
    }

    public String getAdminResponsable() {
        return adminResponsable;
    }

    public void setAdminResponsable(String adminResponsable) {
        this.adminResponsable = adminResponsable;
    }
}
