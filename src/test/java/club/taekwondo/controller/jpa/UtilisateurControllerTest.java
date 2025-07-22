package club.taekwondo.controller.jpa;

import club.taekwondo.dto.LoginDTO;
import club.taekwondo.dto.UtilisateurDTO;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.enums.Role;
import club.taekwondo.security.JwtUtil;
import club.taekwondo.service.jpa.UtilisateurService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UtilisateurController.class)
@AutoConfigureMockMvc(addFilters = false) // Désactive la sécurité pour les tests
class UtilisateurControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UtilisateurService utilisateurService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testCreateUtilisateur_Success() throws Exception {
        UtilisateurDTO dto = new UtilisateurDTO();
        dto.setNom("Dupont");
        dto.setPrenom("Jean");
        dto.setEmail("jean.dupont@email.com");
        dto.setPassword("motdepasse");
        dto.setRoles(Set.of(Role.MEMBRE));

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(1L);
        utilisateur.setEmail(dto.getEmail());
        utilisateur.setRoles(dto.getRoles());

        when(utilisateurService.createUtilisateur(any(UtilisateurDTO.class))).thenReturn(utilisateur);

        mockMvc.perform(post("/api/utilisateurs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.email").value("jean.dupont@email.com"))
                .andExpect(jsonPath("$.roles[0]").value("MEMBRE"));
    }

    @Test
    void testCreateUtilisateur_EmailDejaUtilise() throws Exception {
        UtilisateurDTO dto = new UtilisateurDTO();
        dto.setNom("Dupont");
        dto.setPrenom("Jean");
        dto.setEmail("jean.dupont@email.com");
        dto.setPassword("motdepasse");
        dto.setRoles(Set.of(Role.MEMBRE));

        when(utilisateurService.createUtilisateur(any(UtilisateurDTO.class)))
                .thenThrow(new IllegalArgumentException("Un utilisateur avec cet email existe déjà."));

        mockMvc.perform(post("/api/utilisateurs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Un utilisateur avec cet email existe déjà."));
    }

    @Test
    void testLogin_Success() throws Exception {
        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setEmail("jean.dupont@email.com");
        loginDTO.setPassword("motdepasse");

        UtilisateurDTO utilisateurDTO = new UtilisateurDTO();
        utilisateurDTO.setEmail("jean.dupont@email.com");
        utilisateurDTO.setNom("Dupont");
        utilisateurDTO.setPrenom("Jean");
        utilisateurDTO.setRoles(Set.of(Role.MEMBRE));

        when(utilisateurService.login("jean.dupont@email.com", "motdepasse")).thenReturn(Optional.of(utilisateurDTO));
        when(jwtUtil.generateToken(Mockito.anyString(), Mockito.anyString())).thenReturn("fake-jwt-token");

        mockMvc.perform(post("/api/utilisateurs/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("fake-jwt-token"))
                .andExpect(jsonPath("$.roles[0]").value("MEMBRE"))
                .andExpect(jsonPath("$.email").value("jean.dupont@email.com"));
    }

    @Test
    void testLogin_Echec() throws Exception {
        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setEmail("jean.dupont@email.com");
        loginDTO.setPassword("mauvaismotdepasse");

        when(utilisateurService.login("jean.dupont@email.com", "mauvaismotdepasse")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/utilisateurs/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Email ou mot de passe incorrect."));
    }

    @Test
    void testGetUtilisateurById_Success() throws Exception {
        UtilisateurDTO utilisateurDTO = new UtilisateurDTO();
        utilisateurDTO.setId(1L);
        utilisateurDTO.setNom("Dupont");
        utilisateurDTO.setPrenom("Jean");
        utilisateurDTO.setEmail("jean.dupont@email.com");
        utilisateurDTO.setRoles(Set.of(Role.MEMBRE));

        when(utilisateurService.getUtilisateurById(1L)).thenReturn(Optional.of(utilisateurDTO));

        mockMvc.perform(get("/api/utilisateurs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.email").value("jean.dupont@email.com"));
    }

    @Test
    void testGetUtilisateurById_NotFound() throws Exception {
        when(utilisateurService.getUtilisateurById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/utilisateurs/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Utilisateur non trouvé avec l'ID : 99"));
    }

    @Test
    void testDeleteUtilisateur() throws Exception {
        mockMvc.perform(delete("/api/utilisateurs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Utilisateur supprimé avec succès."));
    }
}