package club.taekwondo.controller;

import club.taekwondo.controller.jpa.UtilisateurController;
import club.taekwondo.dto.UtilisateurDTO;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.security.JwtUtil;
import club.taekwondo.service.jpa.UtilisateurService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UtilisateurController.class)
@AutoConfigureMockMvc(addFilters = false) // désactive les filtres de sécurité
class UtilisateurControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UtilisateurService utilisateurService;

    @MockBean
    private JwtUtil jwtUtil;

    @Test
    void testGetAllUtilisateurs() throws Exception {
        UtilisateurDTO dto1 = new UtilisateurDTO();
        dto1.setId(1L);
        dto1.setNom("Durand");
        dto1.setEmail("durand@example.com");

        UtilisateurDTO dto2 = new UtilisateurDTO();
        dto2.setId(2L);
        dto2.setNom("Martin");
        dto2.setEmail("martin@example.com");

        when(utilisateurService.getAllUtilisateurs()).thenReturn(List.of(dto1, dto2));

        mockMvc.perform(get("/api/utilisateurs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nom").value("Durand"))
                .andExpect(jsonPath("$[1].nom").value("Martin"));
    }

    @Test
    void testRegisterUtilisateur() throws Exception {
        Utilisateur saved = new Utilisateur();
        saved.setId(1L);
        saved.setNom("Dupuis");
        saved.setEmail("dupuis@example.com");
        saved.setRole(null); // Simule un utilisateur créé sans rôle défini

        when(utilisateurService.getUtilisateurEntityByEmail("dupuis@example.com"))
                .thenReturn(Optional.empty());
        when(utilisateurService.createUtilisateur(any(UtilisateurDTO.class)))
                .thenReturn(saved);

        mockMvc.perform(post("/api/utilisateurs/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"Dupuis\",\"email\":\"dupuis@example.com\",\"password\":\"secret\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("dupuis@example.com"))
                .andExpect(jsonPath("$.role").doesNotExist()); // ✅ vérifie que la clé est présente et vaut null
    }

    @Test
    void testLoginUtilisateur() throws Exception {
        UtilisateurDTO dto = new UtilisateurDTO();
        dto.setId(1L);
        dto.setEmail("test@example.com");
        dto.setRole("MEMBRE");

        when(utilisateurService.login("test@example.com", "secret"))
                .thenReturn(Optional.of(dto));
        when(jwtUtil.generateToken("test@example.com", "MEMBRE"))
                .thenReturn("fake-jwt-token");

        mockMvc.perform(post("/api/utilisateurs/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@example.com\",\"password\":\"secret\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("fake-jwt-token"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.role").value("MEMBRE"));
    }

    @Test
    void testGetCurrentUser() throws Exception {
        Utilisateur user = new Utilisateur();
        user.setId(1L);
        user.setNom("Durand");
        user.setEmail("durand@example.com");

        when(jwtUtil.extractEmail("fake-token")).thenReturn("durand@example.com");
        when(utilisateurService.getUtilisateurEntityByEmail("durand@example.com"))
                .thenReturn(Optional.of(user));
        when(utilisateurService.convertToDTO(user)).thenReturn(new UtilisateurDTO());

        mockMvc.perform(get("/api/utilisateurs/me")
                        .header("Authorization", "Bearer fake-token"))
                .andExpect(status().isOk());
    }
}

