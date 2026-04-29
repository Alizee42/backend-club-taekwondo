package club.taekwondo.dto;

import java.util.List;

public class CartCheckoutRequestDTO {

    private String modePaiement;
    private List<LigneCommandeDTO> lignes;

    public String getModePaiement() { return modePaiement; }
    public void setModePaiement(String modePaiement) { this.modePaiement = modePaiement; }

    public List<LigneCommandeDTO> getLignes() { return lignes; }
    public void setLignes(List<LigneCommandeDTO> lignes) { this.lignes = lignes; }
}
