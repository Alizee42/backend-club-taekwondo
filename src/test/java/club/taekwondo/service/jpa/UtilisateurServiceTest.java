package club.taekwondo.service.jpa;

import club.taekwondo.dto.UtilisateurDTO;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.enums.Role;
import club.taekwondo.repository.jpa.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UtilisateurServiceTest {

    private UtilisateurRepository utilisateurRepository;
    private PasswordEncoder passwordEncoder;
    private UtilisateurService utilisateurService;

    @BeforeEach
    void setUp() {
        utilisateurRepository = mock(UtilisateurRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        utilisateurService = new UtilisateurService(utilisateurRepository, passwordEncoder);
    }

    @Test
    void testCreateUtilisateur_Success() {
        UtilisateurDTO dto = new UtilisateurDTO();
        dto.setNom("Dupont");
        dto.setPrenom("Jean");
        dto.setEmail("jean.dupont@email.com");
        dto.setPassword("motdepasse");
        dto.setRoles(Set.of(Role.MEMBRE));

        when(utilisateurRepository.findByEmail(dto.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode("motdepasse")).thenReturn("motdepasseEncodee");
        when(utilisateurRepository.save(any(Utilisateur.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Utilisateur utilisateurCree = utilisateurService.createUtilisateur(dto);

        assertEquals("Dupont", utilisateurCree.getNom());
        assertEquals("Jean", utilisateurCree.getPrenom());
        assertEquals("jean.dupont@email.com", utilisateurCree.getEmail());
        assertTrue(utilisateurCree.getRoles().contains(Role.MEMBRE));
        assertEquals("motdepasseEncodee", utilisateurCree.getPassword());
    }

    @Test
    void testCreateUtilisateur_EmailDejaExistant() {
        UtilisateurDTO dto = new UtilisateurDTO();
        dto.setNom("Dupont");
        dto.setPrenom("Jean");
        dto.setEmail("jean.dupont@email.com");
        dto.setPassword("motdepasse");
        dto.setRoles(Set.of(Role.MEMBRE));

        when(utilisateurRepository.findByEmail(dto.getEmail())).thenReturn(Optional.of(new Utilisateur()));

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            utilisateurService.createUtilisateur(dto);
        });

        assertEquals("Un utilisateur avec cet email existe déjà.", exception.getMessage());
    }

    @Test
    void testCreateUtilisateur_MotDePasseManquant() {
        UtilisateurDTO dto = new UtilisateurDTO();
        dto.setNom("Dupont");
        dto.setPrenom("Jean");
        dto.setEmail("jean.dupont@email.com");
        dto.setPassword(""); // Mot de passe vide
        dto.setRoles(Set.of(Role.MEMBRE));

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            utilisateurService.createUtilisateur(dto);
        });

        assertEquals("Le mot de passe est requis.", exception.getMessage());
    }
    @Test
void testLogin_Success() {
    Utilisateur utilisateur = new Utilisateur();
    utilisateur.setEmail("jean.dupont@email.com");
    utilisateur.setPassword("motdepasseEncodee");
    utilisateur.setNom("Dupont");
    utilisateur.setPrenom("Jean");
    utilisateur.setRoles(Set.of(Role.MEMBRE));

    when(utilisateurRepository.findByEmail("jean.dupont@email.com")).thenReturn(Optional.of(utilisateur));
    when(passwordEncoder.matches("motdepasse", "motdepasseEncodee")).thenReturn(true);

    Optional<UtilisateurDTO> result = utilisateurService.login("jean.dupont@email.com", "motdepasse");

    assertTrue(result.isPresent());
    assertEquals("Dupont", result.get().getNom());
    assertEquals("Jean", result.get().getPrenom());
    assertEquals("jean.dupont@email.com", result.get().getEmail());
    assertTrue(result.get().getRoles().contains(Role.MEMBRE));
}

@Test
void testLogin_MauvaisMotDePasse() {
    Utilisateur utilisateur = new Utilisateur();
    utilisateur.setEmail("jean.dupont@email.com");
    utilisateur.setPassword("motdepasseEncodee");
    utilisateur.setRoles(Set.of(Role.MEMBRE));

    when(utilisateurRepository.findByEmail("jean.dupont@email.com")).thenReturn(Optional.of(utilisateur));
    when(passwordEncoder.matches("fauxmotdepasse", "motdepasseEncodee")).thenReturn(false);

    Optional<UtilisateurDTO> result = utilisateurService.login("jean.dupont@email.com", "fauxmotdepasse");

    assertTrue(result.isEmpty());
}

@Test
void testLogin_EmailInexistant() {
    when(utilisateurRepository.findByEmail("inconnu@email.com")).thenReturn(Optional.empty());

    Optional<UtilisateurDTO> result = utilisateurService.login("inconnu@email.com", "motdepasse");

    assertTrue(result.isEmpty());
}
// ...tes imports et tests précédents...

@Test
void testUpdateUtilisateurFromDTO_Success() {
    Utilisateur utilisateur = new Utilisateur();
    utilisateur.setId(1L);
    utilisateur.setNom("AncienNom");
    utilisateur.setPrenom("AncienPrenom");
    utilisateur.setEmail("ancien@email.com");
    utilisateur.setPassword("ancienmdp");
    utilisateur.setRoles(Set.of(Role.MEMBRE));

    UtilisateurDTO dto = new UtilisateurDTO();
    dto.setNom("NouveauNom");
    dto.setPrenom("NouveauPrenom");
    dto.setEmail("nouveau@email.com");
    dto.setTelephone("0600000000");
    dto.setAdresse("Nouvelle adresse");
    dto.setDateNaissance(null);
    dto.setRoles(Set.of(Role.PARENT, Role.MEMBRE));
    dto.setPassword("nouveaumdp");

    when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(utilisateur));
    when(passwordEncoder.encode("nouveaumdp")).thenReturn("nouveaumdpEncode");

    utilisateurService.updateUtilisateurFromDTO(1L, dto);

    assertEquals("NouveauNom", utilisateur.getNom());
    assertEquals("NouveauPrenom", utilisateur.getPrenom());
    assertEquals("nouveau@email.com", utilisateur.getEmail());
    assertEquals("0600000000", utilisateur.getTelephone());
    assertEquals("Nouvelle adresse", utilisateur.getAdresse());
    assertTrue(utilisateur.getRoles().contains(Role.PARENT));
    assertTrue(utilisateur.getRoles().contains(Role.MEMBRE));
    assertEquals("nouveaumdpEncode", utilisateur.getPassword());
}

@Test
void testDeleteUtilisateur() {
    doNothing().when(utilisateurRepository).deleteById(1L);
    utilisateurService.deleteUtilisateur(1L);
    verify(utilisateurRepository, times(1)).deleteById(1L);
}

@Test
void testGetUtilisateurByEmail_Exist() {
    Utilisateur utilisateur = new Utilisateur();
    utilisateur.setEmail("test@email.com");
    utilisateur.setNom("Test");
    utilisateur.setPrenom("User");
    utilisateur.setRoles(Set.of(Role.MEMBRE));

    when(utilisateurRepository.findByEmail("test@email.com")).thenReturn(Optional.of(utilisateur));

    Optional<UtilisateurDTO> result = utilisateurService.getUtilisateurByEmail("test@email.com");
    assertTrue(result.isPresent());
    assertEquals("Test", result.get().getNom());
}

@Test
void testGetUtilisateurByEmail_NotExist() {
    when(utilisateurRepository.findByEmail("notfound@email.com")).thenReturn(Optional.empty());
    Optional<UtilisateurDTO> result = utilisateurService.getUtilisateurByEmail("notfound@email.com");
    assertTrue(result.isEmpty());
}

@Test
void testFindByNomPrenom_Exist() {
    Utilisateur utilisateur = new Utilisateur();
    utilisateur.setNom("Dupont");
    utilisateur.setPrenom("Jean");

    when(utilisateurRepository.findByNomIgnoreCaseAndPrenomIgnoreCase("Dupont", "Jean")).thenReturn(Optional.of(utilisateur));

    Optional<Utilisateur> result = utilisateurService.findByNomPrenom("Dupont", "Jean");
    assertTrue(result.isPresent());
    assertEquals("Dupont", result.get().getNom());
    assertEquals("Jean", result.get().getPrenom());
}

@Test
void testFindByNomPrenom_NotExist() {
    when(utilisateurRepository.findByNomIgnoreCaseAndPrenomIgnoreCase("Durand", "Paul")).thenReturn(Optional.empty());
    Optional<Utilisateur> result = utilisateurService.findByNomPrenom("Durand", "Paul");
    assertTrue(result.isEmpty());
}

}