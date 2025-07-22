package club.taekwondo.controller.jpa;

import club.taekwondo.dto.*;
import club.taekwondo.entity.jpa.Paiement;
import club.taekwondo.service.jpa.PaiementService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaiementController.class)
@AutoConfigureMockMvc(addFilters = false)
class PaiementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaiementService paiementService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testGetAll() throws Exception {
        PaiementDTO paiement1 = new PaiementDTO();
        paiement1.setId(1L);
        PaiementDTO paiement2 = new PaiementDTO();
        paiement2.setId(2L);

        when(paiementService.getAllWithEcheances()).thenReturn(List.of(paiement1, paiement2));

        mockMvc.perform(get("/api/paiements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

   @Test
void testAjouterPaiementManuel_Success() throws Exception {
    PaiementDTO dto = new PaiementDTO();
    dto.setType("unique");
    dto.setMontantTotal(100.0);
    dto.setModePaiement("CB");
    dto.setUtilisateurId(1L);
    dto.setStatut("en attente"); // <-- AJOUTE cette ligne

    Paiement paiement = new Paiement();
    paiement.setId(1L);

    when(paiementService.ajouterPaiementManuel(any(PaiementDTO.class))).thenReturn(paiement);
    when(paiementService.toPaiementDTO(any(Paiement.class))).thenReturn(dto);

    mockMvc.perform(post("/api/paiements/ajouter-manuel")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type").value("unique"));
}

    @Test
    void testAjouterPaiementManuel_BadRequest() throws Exception {
        PaiementDTO dto = new PaiementDTO();
        dto.setType("unique");
        // DTO incomplet : il manque les champs obligatoires

        // Ne pas mocker le service ici, la validation intervient avant
        mockMvc.perform(post("/api/paiements/ajouter-manuel")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testAnnulerPaiement_Success() throws Exception {
        AnnulationRequestDTO request = new AnnulationRequestDTO();
        request.setMotif("Erreur");
        PaiementDTO updated = new PaiementDTO();
        updated.setId(1L);

        when(paiementService.annulerPaiement(eq(1L), any(AnnulationRequestDTO.class))).thenReturn(updated);

        mockMvc.perform(put("/api/paiements/1/annuler")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void testAnnulerPaiement_NotFound() throws Exception {
        AnnulationRequestDTO request = new AnnulationRequestDTO();
        request.setMotif("Erreur");

        when(paiementService.annulerPaiement(eq(99L), any(AnnulationRequestDTO.class)))
                .thenThrow(new RuntimeException("Paiement non trouvé"));

        mockMvc.perform(put("/api/paiements/99/annuler")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeletePaiement_Success() throws Exception {
        mockMvc.perform(delete("/api/paiements/1"))
                .andExpect(status().isOk());
    }

    @Test
    void testDeletePaiement_NotFound() throws Exception {
        Mockito.doThrow(new RuntimeException("Paiement non trouvé")).when(paiementService).delete(99L);

        mockMvc.perform(delete("/api/paiements/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetDashboardStats() throws Exception {
        DashboardStatsDTO stats = new DashboardStatsDTO(0,0,0,0,List.of(),List.of());
        when(paiementService.buildDashboardStats()).thenReturn(stats);

        mockMvc.perform(get("/api/paiements/dashboard"))
                .andExpect(status().isOk());
    }
}