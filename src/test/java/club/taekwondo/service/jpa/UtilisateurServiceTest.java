package club.taekwondo.service.jpa;

import club.taekwondo.dto.UtilisateurDTO;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.enums.Role;
import club.taekwondo.repository.jpa.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UtilisateurServiceTest {

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UtilisateurService utilisateurService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCreateUtilisateur_Success() {
        // ARRANGE
        UtilisateurDTO dto = new UtilisateurDTO();
        dto.setNom("Dupont");
        dto.setPrenom("Jean");
        dto.setEmail("jean.dupont@example.com");
        dto.setPassword("secret");

        Utilisateur saved = new Utilisateur();
        saved.setId(1L);
        saved.setNom(dto.getNom());
        saved.setPrenom(dto.getPrenom());
        saved.setEmail(dto.getEmail());
        saved.setPassword("hashed");

        when(passwordEncoder.encode("secret")).thenReturn("hashed");
        when(utilisateurRepository.existsByEmailIgnoreCase("jean.dupont@example.com")).thenReturn(false);
        when(utilisateurRepository.save(any(Utilisateur.class))).thenReturn(saved);

        // ACT
        Utilisateur result = utilisateurService.createUtilisateur(dto);

        // ASSERT
        assertNotNull(result.getId());
        assertEquals("Dupont", result.getNom());
        assertEquals("Jean", result.getPrenom());
        assertEquals("jean.dupont@example.com", result.getEmail());
        assertEquals("hashed", result.getPassword());
        verify(utilisateurRepository, times(1)).save(any(Utilisateur.class));
    }

    @Test
    void testCreateUtilisateur_MissingNom_ShouldThrow() {
        UtilisateurDTO dto = new UtilisateurDTO();
        dto.setEmail("test@example.com");
        dto.setPassword("secret");

        Exception ex = assertThrows(IllegalArgumentException.class,
                () -> utilisateurService.createUtilisateur(dto));
        assertEquals("Le nom est requis.", ex.getMessage());
    }

    @Test
    void testGetUtilisateurEntityById_Found() {
        Utilisateur user = new Utilisateur();
        user.setId(1L);
        user.setNom("Martin");

        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(user));

        Optional<Utilisateur> result = utilisateurService.getUtilisateurEntityById(1L);

        assertTrue(result.isPresent());
        assertEquals("Martin", result.get().getNom());
    }

    @Test
    void testGetUtilisateurEntityById_NotFound() {
        when(utilisateurRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Utilisateur> result = utilisateurService.getUtilisateurEntityById(99L);

        assertTrue(result.isEmpty());
    }

    @Test
    void testUpdateUtilisateurFromDTO() {
        Utilisateur existing = new Utilisateur();
        existing.setId(1L);
        existing.setNom("Ancien");
        existing.setEmail("ancien@example.com");
        existing.setRole(Role.MEMBRE);

        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(utilisateurRepository.save(any(Utilisateur.class))).thenAnswer(inv -> inv.getArgument(0));

        UtilisateurDTO update = new UtilisateurDTO();
        update.setNom("Nouveau");
        update.setEmail("nouveau@example.com");
        update.setRole("ADMIN");

        utilisateurService.updateUtilisateurFromDTO(1L, update);

        assertEquals("Nouveau", existing.getNom());
        assertEquals("nouveau@example.com", existing.getEmail());
        assertEquals(Role.ADMIN, existing.getRole());
        verify(utilisateurRepository, times(1)).save(existing);
    }

    @Test
    void testDeleteUtilisateur() {
        Long id = 1L;
        doNothing().when(utilisateurRepository).deleteById(id);

        utilisateurService.deleteUtilisateur(id);

        verify(utilisateurRepository, times(1)).deleteById(id);
    }

    @Test
    void testLogin_Success() {
        Utilisateur user = new Utilisateur();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setPassword("hashed");
        // SUPER_ADMIN n'a pas besoin d'un club pour se connecter
        user.setRole(Role.SUPER_ADMIN);

        when(utilisateurRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "hashed")).thenReturn(true);

        Optional<UtilisateurDTO> result = utilisateurService.login("test@example.com", "secret");

        assertTrue(result.isPresent());
        assertEquals("test@example.com", result.get().getEmail());
    }

    @Test
    void testLogin_WrongPassword() {
        Utilisateur user = new Utilisateur();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setPassword("hashed");

        when(utilisateurRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        Optional<UtilisateurDTO> result = utilisateurService.login("test@example.com", "wrong");

        assertFalse(result.isPresent());
    }
}

