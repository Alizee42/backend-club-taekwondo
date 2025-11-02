package club.taekwondo.dto;

public class RequiredDocumentDTO {
    private Long id;
    private Long clubId;
    private String code;   // ex: CERTIFICAT_MEDICAL
    private String label;  // libellé affiché
    private Boolean required; // requis (true) ou optionnel
    private Boolean active;   // actif/inactif
    private Integer orderIndex; // pour l'ordre d'affichage

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getClubId() { return clubId; }
    public void setClubId(Long clubId) { this.clubId = clubId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public Boolean getRequired() { return required; }
    public void setRequired(Boolean required) { this.required = required; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public Integer getOrderIndex() { return orderIndex; }
    public void setOrderIndex(Integer orderIndex) { this.orderIndex = orderIndex; }
}
