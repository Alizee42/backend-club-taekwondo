package club.taekwondo.dto;

import java.util.List;

public class BonCommandeRequestDTO {

    private Long campagneId;
    private String modePaiement;
    private List<LigneCommandeDTO> lignesCommande;

    public Long getCampagneId() { return campagneId; }
    public void setCampagneId(Long campagneId) { this.campagneId = campagneId; }

    public String getModePaiement() { return modePaiement; }
    public void setModePaiement(String modePaiement) { this.modePaiement = modePaiement; }

    public List<LigneCommandeDTO> getLignesCommande() { return lignesCommande; }
    public void setLignesCommande(List<LigneCommandeDTO> lignesCommande) { this.lignesCommande = lignesCommande; }
}
