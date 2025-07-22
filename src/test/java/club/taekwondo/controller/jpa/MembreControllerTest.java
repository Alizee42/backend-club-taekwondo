package club.taekwondo.controller.jpa;

import club.taekwondo.dto.MembreDTO;
import club.taekwondo.service.jpa.MembreService;
import club.taekwondo.service.jpa.UtilisateurService;
import club.taekwondo.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MembreController.class)
@AutoConfigureMockMvc(addFilters = false)
class MembreControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MembreService membreService;

    @MockBean
    private UtilisateurService utilisateurService;

    @MockBean
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testGetAllMembres() throws Exception {
        MembreDTO membre1 = new MembreDTO();
        membre1.setId(1L);
        membre1.setNom("Dupont");
        membre1.setPrenom("Jean");

        MembreDTO membre2 = new MembreDTO();
        membre2.setId(2L);
        membre2.setNom("Durand");
        membre2.setPrenom("Paul");

        when(membreService.getAllMembres()).thenReturn(List.of(membre1, membre2));

        mockMvc.perform(get("/api/membres"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nom").value("Dupont"))
                .andExpect(jsonPath("$[1].nom").value("Durand"));
    }

    @Test
    void testGetMembreById_Exist() throws Exception {
        MembreDTO membre = new MembreDTO();
        membre.setId(1L);
        membre.setNom("Dupont");

        when(membreService.getMembreById(1L)).thenReturn(Optional.of(membre));

        mockMvc.perform(get("/api/membres/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.nom").value("Dupont"));
    }

    @Test
    void testGetMembreById_NotExist() throws Exception {
        when(membreService.getMembreById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/membres/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Membre non trouvé avec l'ID : 99"));
    }

    @Test
    void testCreateMembre_Success() throws Exception {
        MembreDTO dto = new MembreDTO();
        dto.setNom("Dupont");
        dto.setPrenom("Jean");
        dto.setDateNaissance(LocalDate.of(2000, 1, 1));
        dto.setCeinture("Noire");
        dto.setNumeroLicence("12345");
        dto.setUtilisateurId(10L);

        MembreDTO saved = new MembreDTO();
        saved.setId(1L);
        saved.setNom("Dupont");
        saved.setPrenom("Jean");
        saved.setDateNaissance(LocalDate.of(2000, 1, 1));
        saved.setCeinture("Noire");
        saved.setNumeroLicence("12345");
        saved.setUtilisateurId(10L);

        when(membreService.createMembre(any(MembreDTO.class))).thenReturn(saved);

        mockMvc.perform(post("/api/membres")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.nom").value("Dupont"))
                .andExpect(jsonPath("$.ceinture").value("Noire"));
    }

    @Test
    void testCreateMembre_UtilisateurNonTrouve() throws Exception {
        MembreDTO dto = new MembreDTO();
        dto.setNom("Dupont");
        dto.setPrenom("Jean");
        dto.setUtilisateurId(99L);

        when(membreService.createMembre(any(MembreDTO.class)))
                .thenThrow(new RuntimeException("Utilisateur non trouvé avec l'ID : 99"));

        mockMvc.perform(post("/api/membres")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Utilisateur non trouvé avec l'ID : 99"));
    }

    @Test
    void testGetMembreConnecte_MembreFound() throws Exception {
        String token = "Bearer valid.jwt.token";
        String email = "membre@email.com";
        MembreDTO membre = new MembreDTO();
        membre.setId(1L);
        membre.setNom("Dupont");
        membre.setPrenom("Jean");

        when(jwtUtil.extractEmail("valid.jwt.token")).thenReturn(email);
        when(membreService.getMembreByEmail(email)).thenReturn(Optional.of(membre));

        mockMvc.perform(get("/api/membres/me")
                .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.nom").value("Dupont"));
    }

    @Test
    void testGetMembreConnecte_UtilisateurFound() throws Exception {
        String token = "Bearer valid.jwt.token";
        String email = "utilisateur@email.com";
        when(jwtUtil.extractEmail("valid.jwt.token")).thenReturn(email);
        when(membreService.getMembreByEmail(email)).thenReturn(Optional.empty());

        club.taekwondo.dto.UtilisateurDTO utilisateur = new club.taekwondo.dto.UtilisateurDTO();
        utilisateur.setId(2L);
        utilisateur.setNom("Martin");
        utilisateur.setPrenom("Paul");

        when(utilisateurService.getUtilisateurByEmail(email)).thenReturn(Optional.of(utilisateur));

        mockMvc.perform(get("/api/membres/me")
                .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.nom").value("Martin"));
    }

    @Test
    void testUpdateMembre_NotExist() throws Exception {
        MembreDTO dto = new MembreDTO();
        dto.setNom("NouveauNom");

        when(membreService.updateMembre(Mockito.eq(99L), any(MembreDTO.class)))
                .thenThrow(new RuntimeException("Membre non trouvé avec l'ID : 99"));

        mockMvc.perform(put("/api/membres/99")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Membre non trouvé avec l'ID : 99"));
    }

    @Test
    void testUpdateMembre_Success() throws Exception {
        MembreDTO dto = new MembreDTO();
        dto.setNom("NouveauNom");
        dto.setPrenom("NouveauPrenom");
        dto.setCeinture("Bleue");

        MembreDTO updated = new MembreDTO();
        updated.setId(1L);
        updated.setNom("NouveauNom");
        updated.setPrenom("NouveauPrenom");
        updated.setCeinture("Bleue");

        when(membreService.getMembreById(1L)).thenReturn(Optional.of(updated));
        when(membreService.updateMembre(Mockito.eq(1L), any(MembreDTO.class))).thenReturn(updated);

        mockMvc.perform(put("/api/membres/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom").value("NouveauNom"))
                .andExpect(jsonPath("$.ceinture").value("Bleue"));
    }

       @Test
    void testDeleteMembre_Success() throws Exception {
        MembreDTO membre = new MembreDTO();
        membre.setId(1L);
        membre.setNom("Dupont");
        // Simule que le membre existe
        when(membreService.getMembreById(1L)).thenReturn(Optional.of(membre));
    
        mockMvc.perform(delete("/api/membres/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Membre supprimé avec succès."));
    }

    @Test
    void testDeleteMembre_NotExist() throws Exception {
        Mockito.doThrow(new RuntimeException("Membre non trouvé avec l'ID : 99"))
                .when(membreService).deleteMembre(99L);

        mockMvc.perform(delete("/api/membres/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Membre non trouvé avec l'ID : 99"));
    }
}