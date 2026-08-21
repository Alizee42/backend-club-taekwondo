package club.taekwondo.controller.jpa;

import club.taekwondo.dto.LoginDTO;
import club.taekwondo.dto.ReinitialisationMotDePasseDTO;
import club.taekwondo.dto.UtilisateurDTO;
import club.taekwondo.entity.jpa.Membre;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.enums.Role;
import club.taekwondo.repository.jpa.MembreRepository;
import club.taekwondo.security.JwtRevocationService;
import club.taekwondo.security.JwtUtil;
import club.taekwondo.service.jpa.EmailService;
import club.taekwondo.service.jpa.ReinitialisationMotDePasseService;
import club.taekwondo.service.jpa.UtilisateurService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.TestingAuthenticationToken;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UtilisateurControllerTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UtilisateurService utilisateurService;

    @Mock
    private MembreRepository membreRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private ReinitialisationMotDePasseService reinitService;

    @Mock
    private JwtRevocationService jwtRevocationService;

    private UtilisateurController controller;

    @BeforeEach
    void setUp() {
        controller = new UtilisateurController(jwtUtil, utilisateurService, membreRepository, emailService, reinitService, jwtRevocationService);
    }

    private Utilisateur entity(Long id, String email, Role role) {
        Utilisateur u = new Utilisateur();
        u.setId(id);
        u.setEmail(email);
        u.setRole(role);
        return u;
    }

    private UtilisateurDTO dto(Long id, String email, Role role) {
        UtilisateurDTO d = new UtilisateurDTO();
        d.setId(id);
        d.setEmail(email);
        d.setRole(role != null ? role.name() : null);
        return d;
    }

    // ---- createUtilisateur ----

    @Test
    void createUtilisateur_succes_retourneCreated() {
        Utilisateur created = entity(1L, "nouveau@test.com", Role.ADMIN);
        when(utilisateurService.createUtilisateur(any(UtilisateurDTO.class), eq(true))).thenReturn(created);
        ReinitialisationMotDePasseDTO demande = new ReinitialisationMotDePasseDTO();
        demande.setToken("tok123");
        when(reinitService.creerDemande(1L)).thenReturn(demande);

        ResponseEntity<?> response = controller.createUtilisateur(new UtilisateurDTO());

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    void createUtilisateur_erreur_retourneBadRequest() {
        when(utilisateurService.createUtilisateur(any(UtilisateurDTO.class), eq(true)))
                .thenThrow(new RuntimeException("erreur"));

        ResponseEntity<?> response = controller.createUtilisateur(new UtilisateurDTO());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void createUtilisateur_emailEchoueMaisCreationReussit_retourneCreated() {
        Utilisateur created = entity(1L, "nouveau@test.com", Role.ADMIN);
        when(utilisateurService.createUtilisateur(any(UtilisateurDTO.class), eq(true))).thenReturn(created);
        when(reinitService.creerDemande(1L)).thenThrow(new RuntimeException("email indisponible"));

        ResponseEntity<?> response = controller.createUtilisateur(new UtilisateurDTO());

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    // ---- updateUtilisateur ----

    @Test
    void updateUtilisateur_succes_retourneOk() {
        when(utilisateurService.getUtilisateurEntityById(1L)).thenReturn(Optional.of(entity(1L, "u@test.com", Role.ADMIN)));

        ResponseEntity<?> response = controller.updateUtilisateur(1L, new UtilisateurDTO());

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void updateUtilisateur_absentApresMaj_retourneNotFound() {
        when(utilisateurService.getUtilisateurEntityById(1L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.updateUtilisateur(1L, new UtilisateurDTO());

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void updateUtilisateur_erreur_retourneBadRequest() {
        org.mockito.Mockito.doThrow(new RuntimeException("erreur"))
                .when(utilisateurService).updateUtilisateurFromDTO(anyLong(), any(UtilisateurDTO.class));

        ResponseEntity<?> response = controller.updateUtilisateur(1L, new UtilisateurDTO());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    // ---- deleteUtilisateur ----

    @Test
    void deleteUtilisateur_succes_retourneOk() {
        ResponseEntity<?> response = controller.deleteUtilisateur(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void deleteUtilisateur_erreur_retourneBadRequest() {
        org.mockito.Mockito.doThrow(new RuntimeException("erreur")).when(utilisateurService).deleteUtilisateur(1L);

        ResponseEntity<?> response = controller.deleteUtilisateur(1L);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    // ---- listUtilisateurs ----

    @Test
    void listUtilisateurs_superAdmin_retourneTous() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("super@test.com", null, "ROLE_SUPER_ADMIN"));
        try {
            when(utilisateurService.getAllUtilisateurs()).thenReturn(List.of(dto(1L, "a@test.com", Role.ADMIN)));

            ResponseEntity<List<UtilisateurDTO>> response = controller.listUtilisateurs(null, null, null);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(1, response.getBody().size());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void listUtilisateurs_adminForceSonPropreClub() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("admin@test.com", null, "ROLE_ADMIN"));
        try {
            Utilisateur admin = entity(1L, "admin@test.com", Role.ADMIN);
            club.taekwondo.entity.jpa.Club club = new club.taekwondo.entity.jpa.Club();
            club.setId(10L);
            admin.setClub(club);
            when(utilisateurService.getUtilisateurEntityByEmail("admin@test.com")).thenReturn(Optional.of(admin));

            UtilisateurDTO membreDuClub = dto(2L, "membre@test.com", Role.MEMBRE);
            membreDuClub.setClubId(10L);
            when(utilisateurService.getAllUtilisateurs()).thenReturn(List.of(membreDuClub));

            ResponseEntity<List<UtilisateurDTO>> response = controller.listUtilisateurs(null, null, null);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(1, response.getBody().size());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void listUtilisateurs_adminDemandeAutreClub_retourneForbidden() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("admin@test.com", null, "ROLE_ADMIN"));
        try {
            Utilisateur admin = entity(1L, "admin@test.com", Role.ADMIN);
            club.taekwondo.entity.jpa.Club club = new club.taekwondo.entity.jpa.Club();
            club.setId(10L);
            admin.setClub(club);
            when(utilisateurService.getUtilisateurEntityByEmail("admin@test.com")).thenReturn(Optional.of(admin));

            ResponseEntity<List<UtilisateurDTO>> response = controller.listUtilisateurs(null, 99L, null);

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void listUtilisateurs_filtreParRoleEtRecherche() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("super@test.com", null, "ROLE_SUPER_ADMIN"));
        try {
            UtilisateurDTO admin = dto(1L, "jean.dupont@test.com", Role.ADMIN);
            admin.setNom("Dupont");
            UtilisateurDTO membre = dto(2L, "alice@test.com", Role.MEMBRE);
            membre.setNom("Martin");
            when(utilisateurService.getAllUtilisateurs()).thenReturn(List.of(admin, membre));

            ResponseEntity<List<UtilisateurDTO>> response = controller.listUtilisateurs("ADMIN", null, "dupont");

            assertEquals(1, response.getBody().size());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    // ---- register ----

    @Test
    void register_payloadNul_retourneBadRequest() {
        ResponseEntity<?> response = controller.register(null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void register_sansEmail_retourneBadRequest() {
        ResponseEntity<?> response = controller.register(new UtilisateurDTO());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void register_emailDejaUtilise_retourneBadRequest() {
        UtilisateurDTO input = dto(null, "existe@test.com", Role.MEMBRE);
        when(utilisateurService.getUtilisateurEntityByEmail("existe@test.com"))
                .thenReturn(Optional.of(entity(1L, "existe@test.com", Role.MEMBRE)));

        ResponseEntity<?> response = controller.register(input);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void register_succes_retourneCreated() {
        UtilisateurDTO input = dto(null, "nouveau@test.com", null);
        when(utilisateurService.getUtilisateurEntityByEmail("nouveau@test.com")).thenReturn(Optional.empty());
        Utilisateur created = entity(5L, "nouveau@test.com", Role.MEMBRE);
        when(utilisateurService.createUtilisateur(any(UtilisateurDTO.class), eq(false))).thenReturn(created);

        ResponseEntity<?> response = controller.register(input);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    void register_erreurValidation_retourneBadRequest() {
        UtilisateurDTO input = dto(null, "nouveau@test.com", Role.MEMBRE);
        when(utilisateurService.getUtilisateurEntityByEmail("nouveau@test.com")).thenReturn(Optional.empty());
        when(utilisateurService.createUtilisateur(any(UtilisateurDTO.class), eq(false)))
                .thenThrow(new IllegalArgumentException("invalide"));

        ResponseEntity<?> response = controller.register(input);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void register_erreurInattendue_retourneInternalServerError() {
        UtilisateurDTO input = dto(null, "nouveau@test.com", Role.MEMBRE);
        when(utilisateurService.getUtilisateurEntityByEmail("nouveau@test.com")).thenReturn(Optional.empty());
        when(utilisateurService.createUtilisateur(any(UtilisateurDTO.class), eq(false)))
                .thenThrow(new RuntimeException("boom"));

        ResponseEntity<?> response = controller.register(input);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    // ---- login ----

    @Test
    void login_sansCredentials_retourneBadRequest() {
        ResponseEntity<?> response = controller.login(new LoginDTO());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void login_credentialsInvalides_retourneUnauthorized() {
        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setEmail("u@test.com");
        loginDTO.setPassword("wrong");
        when(utilisateurService.login("u@test.com", "wrong")).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.login(loginDTO);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void login_succes_retourneToken() {
        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setEmail("u@test.com");
        loginDTO.setPassword("secret");
        UtilisateurDTO userDto = dto(1L, "u@test.com", Role.MEMBRE);
        when(utilisateurService.login("u@test.com", "secret")).thenReturn(Optional.of(userDto));
        when(membreRepository.findByCompteUtilisateur_Id(1L)).thenReturn(Optional.empty());
        when(utilisateurService.getUtilisateurEntityById(1L)).thenReturn(Optional.of(entity(1L, "u@test.com", Role.MEMBRE)));
        when(jwtUtil.generateToken(eq("u@test.com"), eq("MEMBRE"), eq(1L), any())).thenReturn("jwt-token");

        ResponseEntity<?> response = controller.login(loginDTO);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void login_avecMembreAssocie_inclutMembreId() {
        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setEmail("parent@test.com");
        loginDTO.setPassword("secret");
        UtilisateurDTO userDto = dto(1L, "parent@test.com", Role.PARENT);
        when(utilisateurService.login("parent@test.com", "secret")).thenReturn(Optional.of(userDto));
        Membre membre = new Membre();
        membre.setId(8L);
        when(membreRepository.findByCompteUtilisateur_Id(1L)).thenReturn(Optional.of(membre));
        when(utilisateurService.getUtilisateurEntityById(1L)).thenReturn(Optional.of(entity(1L, "parent@test.com", Role.PARENT)));
        when(jwtUtil.generateToken(anyString(), anyString(), anyLong(), eq(8L))).thenReturn("jwt-token");

        ResponseEntity<?> response = controller.login(loginDTO);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void login_erreurInattendue_retourneInternalServerError() {
        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setEmail("u@test.com");
        loginDTO.setPassword("secret");
        when(utilisateurService.login("u@test.com", "secret")).thenThrow(new RuntimeException("boom"));

        ResponseEntity<?> response = controller.login(loginDTO);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    // ---- logout ----

    @Test
    void logout_sansHeader_retourneBadRequest() {
        ResponseEntity<?> response = controller.logout(null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void logout_headerInvalide_retourneBadRequest() {
        ResponseEntity<?> response = controller.logout("InvalidHeader");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void logout_tokenVide_retourneBadRequest() {
        ResponseEntity<?> response = controller.logout("Bearer ");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void logout_succes_retourneOk() {
        ResponseEntity<?> response = controller.logout("Bearer valid-token");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void logout_erreurRevocation_retourneBadRequest() {
        org.mockito.Mockito.doThrow(new RuntimeException("erreur")).when(jwtRevocationService).revokeToken("valid-token");

        ResponseEntity<?> response = controller.logout("Bearer valid-token");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    // ---- getCurrentUser ----

    @Test
    void getCurrentUser_sansHeader_retourneUnauthorized() {
        ResponseEntity<?> response = controller.getCurrentUser(null);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void getCurrentUser_tokenInvalide_retourneUnauthorized() {
        when(jwtUtil.extractEmail("badtoken")).thenReturn(null);

        ResponseEntity<?> response = controller.getCurrentUser("Bearer badtoken");

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void getCurrentUser_utilisateurIntrouvable_retourneNotFound() {
        when(jwtUtil.extractEmail("tok")).thenReturn("u@test.com");
        when(utilisateurService.getUtilisateurEntityByEmail("u@test.com")).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.getCurrentUser("Bearer tok");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void getCurrentUser_succes_retourneOk() {
        when(jwtUtil.extractEmail("tok")).thenReturn("u@test.com");
        Utilisateur u = entity(1L, "u@test.com", Role.MEMBRE);
        when(utilisateurService.getUtilisateurEntityByEmail("u@test.com")).thenReturn(Optional.of(u));
        when(utilisateurService.convertToDTO(u)).thenReturn(dto(1L, "u@test.com", Role.MEMBRE));

        ResponseEntity<?> response = controller.getCurrentUser("Bearer tok");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getCurrentUser_erreurInattendue_retourneInternalServerError() {
        when(jwtUtil.extractEmail("tok")).thenThrow(new RuntimeException("boom"));

        ResponseEntity<?> response = controller.getCurrentUser("Bearer tok");

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    // ---- updateCurrentUser ----

    @Test
    void updateCurrentUser_sansHeader_retourneUnauthorized() {
        ResponseEntity<?> response = controller.updateCurrentUser(null, new UtilisateurDTO());

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void updateCurrentUser_tokenInvalide_retourneUnauthorized() {
        when(jwtUtil.extractEmail("badtoken")).thenReturn(null);

        ResponseEntity<?> response = controller.updateCurrentUser("Bearer badtoken", new UtilisateurDTO());

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void updateCurrentUser_utilisateurIntrouvable_retourneNotFound() {
        when(jwtUtil.extractEmail("tok")).thenReturn("u@test.com");
        when(utilisateurService.getUtilisateurEntityByEmail("u@test.com")).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.updateCurrentUser("Bearer tok", new UtilisateurDTO());

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void updateCurrentUser_succes_retourneOk() {
        when(jwtUtil.extractEmail("tok")).thenReturn("u@test.com");
        Utilisateur u = entity(1L, "u@test.com", Role.MEMBRE);
        when(utilisateurService.getUtilisateurEntityByEmail("u@test.com")).thenReturn(Optional.of(u));
        when(utilisateurService.getUtilisateurEntityById(1L)).thenReturn(Optional.of(u));
        when(utilisateurService.convertToDTO(u)).thenReturn(dto(1L, "u@test.com", Role.MEMBRE));

        ResponseEntity<?> response = controller.updateCurrentUser("Bearer tok", new UtilisateurDTO());

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void updateCurrentUser_erreur_retourneBadRequest() {
        when(jwtUtil.extractEmail("tok")).thenThrow(new RuntimeException("boom"));

        ResponseEntity<?> response = controller.updateCurrentUser("Bearer tok", new UtilisateurDTO());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    // ---- updateCurrentUserPassword ----

    @Test
    void updateCurrentUserPassword_sansHeader_retourneUnauthorized() {
        ResponseEntity<?> response = controller.updateCurrentUserPassword(null, Map.of());

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void updateCurrentUserPassword_utilisateurIntrouvable_retourneNotFound() {
        when(jwtUtil.extractEmail("tok")).thenReturn("u@test.com");
        when(utilisateurService.getUtilisateurEntityByEmail("u@test.com")).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.updateCurrentUserPassword("Bearer tok", Map.of());

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void updateCurrentUserPassword_champsManquants_retourneBadRequest() {
        when(jwtUtil.extractEmail("tok")).thenReturn("u@test.com");
        when(utilisateurService.getUtilisateurEntityByEmail("u@test.com")).thenReturn(Optional.of(entity(1L, "u@test.com", Role.MEMBRE)));

        ResponseEntity<?> response = controller.updateCurrentUserPassword("Bearer tok", Map.of());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void updateCurrentUserPassword_motDePasseActuelIncorrect_retourneBadRequest() {
        when(jwtUtil.extractEmail("tok")).thenReturn("u@test.com");
        when(utilisateurService.getUtilisateurEntityByEmail("u@test.com")).thenReturn(Optional.of(entity(1L, "u@test.com", Role.MEMBRE)));
        when(utilisateurService.changerMotDePassePersonnel(1L, "wrong", "newpass")).thenReturn(false);

        ResponseEntity<?> response = controller.updateCurrentUserPassword("Bearer tok",
                Map.of("currentPassword", "wrong", "newPassword", "newpass"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void updateCurrentUserPassword_succes_retourneOk() {
        when(jwtUtil.extractEmail("tok")).thenReturn("u@test.com");
        when(utilisateurService.getUtilisateurEntityByEmail("u@test.com")).thenReturn(Optional.of(entity(1L, "u@test.com", Role.MEMBRE)));
        when(utilisateurService.changerMotDePassePersonnel(1L, "old", "newpass")).thenReturn(true);

        ResponseEntity<?> response = controller.updateCurrentUserPassword("Bearer tok",
                Map.of("currentPassword", "old", "newPassword", "newpass"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void updateCurrentUserPassword_erreurInattendue_retourneBadRequest() {
        when(jwtUtil.extractEmail("tok")).thenThrow(new RuntimeException("boom"));

        ResponseEntity<?> response = controller.updateCurrentUserPassword("Bearer tok", Map.of());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}
