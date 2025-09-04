package club.taekwondo.dto;

import java.util.List;

public class CartCheckoutRequest {
    private String modePaiement; // "stripe" | "cb" | "virement" | ...
    private Long membreId;       // optionnel : achat pour un enfant
    private List<CartItemDTO> items;

    // --- getters/setters ---
    public String getModePaiement() { return modePaiement; }
    public void setModePaiement(String modePaiement) { this.modePaiement = modePaiement; }

    public Long getMembreId() { return membreId; }
    public void setMembreId(Long membreId) { this.membreId = membreId; }

    public List<CartItemDTO> getItems() { return items; }
    public void setItems(List<CartItemDTO> items) { this.items = items; }
}
