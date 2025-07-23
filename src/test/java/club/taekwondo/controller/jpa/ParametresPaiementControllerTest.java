package club.taekwondo.controller.jpa;

import club.taekwondo.dto.ParametresPaiementDTO;
import club.taekwondo.service.jpa.ParametresPaiementService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ParametresPaiementController.class)
@AutoConfigureMockMvc(addFilters = false)
class ParametresPaiementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ParametresPaiementService parametresPaiementService;

    @Test
    void testGetParametresPaiement() throws Exception {
        ParametresPaiementDTO dto = new ParametresPaiementDTO();
        dto.setMontantCotisation(100.0);
        dto.setVirement(true);
        dto.setEspeces(false);
        dto.setStripe(true);
        dto.setModePaiementParDefaut("CB");
        dto.setEcheancesAutorisees(3);
        dto.setIntervalleEcheance("mois");

        when(parametresPaiementService.getParametresPaiement()).thenReturn(dto);

        mockMvc.perform(get("/api/parametres-paiement"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montantCotisation").value(100.0))
                .andExpect(jsonPath("$.virement").value(true))
                .andExpect(jsonPath("$.modePaiementParDefaut").value("CB"));
    }

    @Test
    void testUpdateParametresPaiement() throws Exception {
        ParametresPaiementDTO dto = new ParametresPaiementDTO();
        dto.setMontantCotisation(200.0);
        dto.setVirement(false);
        dto.setEspeces(true);
        dto.setStripe(false);
        dto.setModePaiementParDefaut("VIREMENT");
        dto.setEcheancesAutorisees(2);
        dto.setIntervalleEcheance("trimestre");

        Mockito.doNothing().when(parametresPaiementService).updateParametresPaiement(dto);

        mockMvc.perform(post("/api/parametres-paiement")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montantCotisation").value(200.0))
                .andExpect(jsonPath("$.modePaiementParDefaut").value("VIREMENT"));
    }
}
