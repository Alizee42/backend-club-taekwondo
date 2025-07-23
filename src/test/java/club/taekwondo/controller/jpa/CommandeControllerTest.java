package club.taekwondo.controller.jpa;

import club.taekwondo.dto.CommandeDTO;
import club.taekwondo.dto.CommandeUpdateDTO;
import club.taekwondo.service.jpa.CommandeService;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CommandeController.class)
@AutoConfigureMockMvc(addFilters = false)
class CommandeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CommandeService commandeService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testGetAllCommandes() throws Exception {
        CommandeDTO dto = new CommandeDTO();
        dto.setId(1L);
        dto.setMontantTotal(BigDecimal.valueOf(100));
        dto.setStatut("EN_ATTENTE");

        when(commandeService.getAllCommandes()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/commandes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].statut").value("EN_ATTENTE"));
    }

    @Test
    void testGetCommandeById_Found() throws Exception {
        CommandeDTO dto = new CommandeDTO();
        dto.setId(1L);
        dto.setStatut("PAYEE");

        when(commandeService.getCommandeById(1L)).thenReturn(Optional.of(dto));

        mockMvc.perform(get("/api/commandes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("PAYEE"));
    }

    @Test
    void testGetCommandeById_NotFound() throws Exception {
        when(commandeService.getCommandeById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/commandes/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCreateCommande() throws Exception {
        CommandeDTO dto = new CommandeDTO();
        dto.setMontantTotal(BigDecimal.valueOf(150));
        dto.setStatut("EN_ATTENTE");

        when(commandeService.createCommande(any(CommandeDTO.class))).thenReturn(dto);

        mockMvc.perform(post("/api/commandes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE"));
    }

    @Test
    void testMettreAJourCommande() throws Exception {
        CommandeUpdateDTO updateDTO = new CommandeUpdateDTO();
        updateDTO.setStatut("PAYEE");
        updateDTO.setModePaiement("CB");
        updateDTO.setDatePaiement(LocalDate.now());

        Mockito.doNothing().when(commandeService).mettreAJourCommande(eq(1L), any(CommandeUpdateDTO.class));

        mockMvc.perform(put("/api/commandes/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk());
    }

    @Test
    void testDeleteCommande_Success() throws Exception {
        Mockito.doNothing().when(commandeService).deleteCommande(1L);

        mockMvc.perform(delete("/api/commandes/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void testDeleteCommande_NotFound() throws Exception {
        Mockito.doThrow(new IllegalArgumentException("Commande non trouvée"))
                .when(commandeService).deleteCommande(99L);

        mockMvc.perform(delete("/api/commandes/99"))
                .andExpect(status().isNotFound());
    }
}