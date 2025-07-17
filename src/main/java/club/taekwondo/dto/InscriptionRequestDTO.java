package club.taekwondo.dto;

import java.util.List;

public class InscriptionRequestDTO {

    private UtilisateurDTO parent;

    private List<MembreDTO> enfants;

    // --- Getters & Setters ---

    public UtilisateurDTO getParent() {
        return parent;
    }

    public void setParent(UtilisateurDTO parent) {
        this.parent = parent;
    }

    public List<MembreDTO> getEnfants() {
        return enfants;
    }

    public void setEnfants(List<MembreDTO> enfants) {
        this.enfants = enfants;
    }
}
