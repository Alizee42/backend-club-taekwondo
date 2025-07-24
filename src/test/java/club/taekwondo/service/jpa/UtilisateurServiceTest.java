package club.taekwondo.service.jpa;

import club.taekwondo.dto.UtilisateurDTO;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.repository.jpa.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class UtilisateurServiceTest {

    private UtilisateurRepository utilisateurRepository;
    private PasswordEncoder passwordEncoder;
    private UtilisateurService utilisateurService;

    @BeforeEach
    public void setup() {
        utilisateurRepository = mock(UtilisateurRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        utilisateurService = new UtilisateurService(utilisateurRepository, passwordEncoder);
    }

    @Test
    public void testCreateUtilisateur_RoleParDefautMembre() {
        // Given
        UtilisateurDTO dto = new UtilisateurDTO();
        dto.setNom("Dupont");
        dto.setPrenom("Jean");
        dto.setEmail("jean.dupont@test.com");
        dto.setPassword("motdepasse");

        when(utilisateurRepository.findByEmail("jean.dupont@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_password");

        // When
        utilisateurService.createUtilisateur(dto);

        // Then
        ArgumentCaptor<Utilisateur> utilisateurCaptor = ArgumentCaptor.forClass(Utilisateur.class);
        verify(utilisateurRepository, times(1)).save(utilisateurCaptor.capture());

        Utilisateur saved = utilisateurCaptor.getValue();
        assertEquals("jean.dupont@test.com", saved.getEmail());
        assertEquals("MEMBRE", saved.getRole()); // vérifie que le rôle est bien par défaut
        assertEquals("encoded_password", saved.getPassword());
    }

    @Test
    public void testCreateUtilisateur_EmailExistant_Exception() {
        // Given
        UtilisateurDTO dto = new UtilisateurDTO();
        dto.setNom("Dupont");
        dto.setEmail("existant@test.com");
        dto.setPassword("motdepasse");

        when(utilisateurRepository.findByEmail("existant@test.com")).thenReturn(Optional.of(new Utilisateur()));

        // When + Then
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            utilisateurService.createUtilisateur(dto);
        });

        assertEquals("Un utilisateur avec cet email existe déjà.", exception.getMessage());
    }

    @Test
    public void testCreateUtilisateur_ChampManquant_Exception() {
        // Given
        UtilisateurDTO dto = new UtilisateurDTO(); // aucun champ rempli

        // When + Then
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            utilisateurService.createUtilisateur(dto);
        });

        assertEquals("Le nom est requis.", exception.getMessage());
    }

    @Test
    public void testLogin_Succes() {
        // Given
        String email = "test@test.com";
        String password = "secret";
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setEmail(email);
        utilisateur.setPassword("encoded_password");
        utilisateur.setRole("ADMIN");

        when(utilisateurRepository.findByEmail(email)).thenReturn(Optional.of(utilisateur));
        when(passwordEncoder.matches(password, "encoded_password")).thenReturn(true);

        // When
        Optional<UtilisateurDTO> result = utilisateurService.login(email, password);

        // Then
        assertTrue(result.isPresent());
        assertEquals(email, result.get().getEmail());
        assertEquals("ADMIN", result.get().getRole());
    }

    @Test
    public void testLogin_MotDePasseIncorrect() {
        // Given
        String email = "test@test.com";
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setEmail(email);
        utilisateur.setPassword("encoded_password");

        when(utilisateurRepository.findByEmail(email)).thenReturn(Optional.of(utilisateur));
        when(passwordEncoder.matches(anyString(), eq("encoded_password"))).thenReturn(false);

        // When
        Optional<UtilisateurDTO> result = utilisateurService.login(email, "wrong_password");

        // Then
        assertFalse(result.isPresent());
    }
}
