package club.taekwondo.controller.jpa;

import club.taekwondo.dto.ReinitialisationMotDePasseDTO;
import club.taekwondo.service.jpa.ReinitialisationMotDePasseService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReinitialisationMotDePasseController.class)
@AutoConfigureMockMvc(addFilters = false) // Désactive les filtres de sécurité (CSRF, Auth, etc.)
public class ReinitialisationMotDePasseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReinitialisationMotDePasseService service;

    @Test
    void testDemanderReinitialisation_success() throws Exception {
        mockMvc.perform(post("/api/reinitialisation/demander")
                        .param("email", "test@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Demande de réinitialisation envoyée."));
    }

    @Test
    void testDemanderReinitialisation_fail() throws Exception {
        Mockito.doThrow(new IllegalArgumentException("Utilisateur introuvable"))
                .when(service).demanderReinitialisation("inconnu@example.com");

        mockMvc.perform(post("/api/reinitialisation/demander")
                        .param("email", "inconnu@example.com"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Utilisateur introuvable"));
    }

    @Test
    void testVerifierToken_valide() throws Exception {
        ReinitialisationMotDePasseDTO dto = new ReinitialisationMotDePasseDTO();
        dto.setId(1L);
        dto.setToken("valid-token");
        dto.setUtilisateurId(5L);
        dto.setDateExpiration(LocalDateTime.now().plusMinutes(30));
        dto.setUtilise(false);

        Mockito.when(service.getByToken("valid-token")).thenReturn(Optional.of(dto));

        mockMvc.perform(get("/api/reinitialisation/verifier")
                        .param("token", "valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("valid-token"));
    }

    @Test
    void testVerifierToken_invalide() throws Exception {
        Mockito.when(service.getByToken("invalid-token")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/reinitialisation/verifier")
                        .param("token", "invalid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Lien invalide ou expiré."));
    }

    @Test
    void testValiderToken_succes() throws Exception {
        Mockito.when(service.validerToken("abc123")).thenReturn(true);

        mockMvc.perform(post("/api/reinitialisation/valider")
                        .param("token", "abc123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Token validé."));
    }

    @Test
    void testValiderToken_echec() throws Exception {
        Mockito.when(service.validerToken("expired")).thenReturn(false);

        mockMvc.perform(post("/api/reinitialisation/valider")
                        .param("token", "expired"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Token invalide ou expiré."));
    }
}
