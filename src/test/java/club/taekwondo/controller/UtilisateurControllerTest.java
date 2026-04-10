package club.taekwondo.controller;

import club.taekwondo.controller.jpa.UtilisateurController;
import club.taekwondo.dto.UtilisateurDTO;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.repository.jpa.MembreRepository;
import club.taekwondo.security.JwtRevocationService;
import club.taekwondo.security.JwtUtil;
import club.taekwondo.service.jpa.EmailService;
import club.taekwondo.service.jpa.ReinitialisationMotDePasseService;
import club.taekwondo.service.jpa.UtilisateurService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UtilisateurController.class)
@AutoConfigureMockMvc(addFilters = false)
class UtilisateurControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UtilisateurService utilisateurService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private MembreRepository membreRepository;

    @MockBean
    private EmailService emailService;

    @MockBean
    private ReinitialisationMotDePasseService reinitService;

    @MockBean
    private JwtRevocationService jwtRevocationService;

    @Test
    @WithMockUser(roles = "ADMIN")
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
        saved.setRole(null);

        when(utilisateurService.getUtilisateurEntityByEmail("dupuis@example.com"))
                .thenReturn(Optional.empty());
        // Le controller appelle createUtilisateur(dto, false) — signature 2 args
        when(utilisateurService.createUtilisateur(any(UtilisateurDTO.class), eq(false)))
                .thenReturn(saved);

        mockMvc.perform(post("/api/utilisateurs/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"Dupuis\",\"email\":\"dupuis@example.com\",\"password\":\"secret\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("dupuis@example.com"));
    }

    @Test
    void testLoginUtilisateur() throws Exception {
        UtilisateurDTO dto = new UtilisateurDTO();
        dto.setId(1L);
        dto.setEmail("test@example.com");
        dto.setRole("MEMBRE");

        Utilisateur userEntity = new Utilisateur();
        userEntity.setId(1L);
        userEntity.setEmail("test@example.com");
        userEntity.setPasswordTemporaire(false);

        when(utilisateurService.login("test@example.com", "secret"))
                .thenReturn(Optional.of(dto));
        // Controller appelle generateToken(email, role, utilisateurId, membreId) — 4 args
        when(jwtUtil.generateToken(eq("test@example.com"), eq("MEMBRE"), eq(1L), isNull()))
                .thenReturn("fake-jwt-token");
        when(membreRepository.findByCompteUtilisateur_Id(1L))
                .thenReturn(Optional.empty());
        when(utilisateurService.getUtilisateurEntityById(1L))
                .thenReturn(Optional.of(userEntity));

        mockMvc.perform(post("/api/utilisateurs/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@example.com\",\"password\":\"secret\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("fake-jwt-token"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.role").value("MEMBRE"));
    }

    @Test
    @WithMockUser
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

    @Test
    void testLogoutUtilisateur() throws Exception {
        mockMvc.perform(post("/api/utilisateurs/logout")
                        .header("Authorization", "Bearer fake-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Deconnexion prise en compte."));
    }
}
