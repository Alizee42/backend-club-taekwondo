package club.taekwondo.controller.jpa;

import club.taekwondo.dto.ProduitDTO;
import club.taekwondo.service.jpa.ProduitService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProduitController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProduitControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProduitService produitService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testGetAllProduits() throws Exception {
        ProduitDTO dto = new ProduitDTO();
        dto.setId(1L);
        dto.setNom("Dobok");
        dto.setDescription("Tenue officielle");
        dto.setPrix(BigDecimal.valueOf(30));
        dto.setStock(10);
        dto.setCategorie("Equipement");
        dto.setImageUrl("img.jpg");

        when(produitService.getAllProduits()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/produits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nom").value("Dobok"));
    }

    @Test
    void testGetProduitById_Found() throws Exception {
        ProduitDTO dto = new ProduitDTO();
        dto.setId(1L);
        dto.setNom("Ceinture");
        dto.setDescription("Ceinture noire");
        dto.setPrix(BigDecimal.valueOf(15));
        dto.setStock(5);
        dto.setCategorie("Equipement");
        dto.setImageUrl("img2.jpg");

        when(produitService.getProduitById(1L)).thenReturn(Optional.of(dto));

        mockMvc.perform(get("/api/produits/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom").value("Ceinture"));
    }

    @Test
    void testGetProduitById_NotFound() throws Exception {
        when(produitService.getProduitById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/produits/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCreateProduit() throws Exception {
        ProduitDTO dto = new ProduitDTO();
        dto.setNom("Plastron");
        dto.setDescription("Protection");
        dto.setPrix(BigDecimal.valueOf(50));
        dto.setStock(5);
        dto.setCategorie("Protection");
        dto.setImageUrl("img3.jpg");

        when(produitService.createProduit(any(ProduitDTO.class))).thenReturn(dto);

        mockMvc.perform(post("/api/produits")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nom").value("Plastron"));
    }

    @Test
    void testUpdateProduit_Success() throws Exception {
        ProduitDTO dto = new ProduitDTO();
        dto.setNom("Dobok Premium");
        dto.setDescription("Tenue officielle premium");
        dto.setPrix(BigDecimal.valueOf(40));
        dto.setStock(8);
        dto.setCategorie("Equipement");
        dto.setImageUrl("img4.jpg");

        when(produitService.updateProduit(eq(1L), any(ProduitDTO.class))).thenReturn(dto);

        mockMvc.perform(put("/api/produits/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom").value("Dobok Premium"));
    }

    @Test
    void testUpdateProduit_NotFound() throws Exception {
        ProduitDTO dto = new ProduitDTO();
        dto.setNom("Inexistant");

        Mockito.when(produitService.updateProduit(eq(99L), any(ProduitDTO.class)))
                .thenThrow(new IllegalArgumentException("Le produit avec l'ID 99 n'existe pas."));

        mockMvc.perform(put("/api/produits/99")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteProduit_Success() throws Exception {
        Mockito.doNothing().when(produitService).deleteProduit(1L);

        mockMvc.perform(delete("/api/produits/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void testDeleteProduit_NotFound() throws Exception {
        Mockito.doThrow(new IllegalArgumentException("Le produit avec l'ID 99 n'existe pas."))
                .when(produitService).deleteProduit(99L);

        mockMvc.perform(delete("/api/produits/99"))
                .andExpect(status().isNotFound());
    }
}