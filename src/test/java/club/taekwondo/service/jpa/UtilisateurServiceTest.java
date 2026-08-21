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

    // ---- getAllUtilisateurs / getAllUtilisateursByClubId / getAllWithPaiements ----

    @Test
    void testGetAllUtilisateurs_convertitTousLesUtilisateurs() {
        Utilisateur u1 = new Utilisateur();
        u1.setId(1L);
        u1.setNom("A");
        Utilisateur u2 = new Utilisateur();
        u2.setId(2L);
        u2.setNom("B");
        when(utilisateurRepository.findAll()).thenReturn(List.of(u1, u2));

        List<UtilisateurDTO> result = utilisateurService.getAllUtilisateurs();

        assertEquals(2, result.size());
        assertEquals("A", result.get(0).getNom());
    }

    @Test
    void testGetAllUtilisateursByClubId_filtreParClub() {
        Utilisateur u1 = new Utilisateur();
        u1.setId(1L);
        u1.setNom("ClubUser");
        when(utilisateurRepository.findByClub_Id(10L)).thenReturn(List.of(u1));

        List<UtilisateurDTO> result = utilisateurService.getAllUtilisateursByClubId(10L);

        assertEquals(1, result.size());
        assertEquals("ClubUser", result.get(0).getNom());
    }

    @Test
    void testGetAllWithPaiements_mappeVersDtoLeger() {
        Utilisateur u = new Utilisateur();
        u.setId(1L);
        u.setNom("Dupont");
        u.setPrenom("Jean");
        u.setEmail("jean@test.com");
        u.setRole(Role.ADMIN);
        when(utilisateurRepository.findAll()).thenReturn(List.of(u));

        var result = utilisateurService.getAllWithPaiements();

        assertEquals(1, result.size());
        assertEquals("Dupont", result.get(0).getNom());
        assertEquals("ADMIN", result.get(0).getRole());
    }

    @Test
    void testGetAllWithPaiements_roleNull_utiliseMembreParDefaut() {
        Utilisateur u = new Utilisateur();
        u.setId(1L);
        u.setNom("SansRole");
        when(utilisateurRepository.findAll()).thenReturn(List.of(u));

        var result = utilisateurService.getAllWithPaiements();

        assertEquals("MEMBRE", result.get(0).getRole());
    }

    // ---- lookups par email / nom-prenom ----

    @Test
    void testGetUtilisateurByEmail_trouve() {
        Utilisateur u = new Utilisateur();
        u.setId(1L);
        u.setEmail("test@example.com");
        when(utilisateurRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(u));

        Optional<UtilisateurDTO> result = utilisateurService.getUtilisateurByEmail("TEST@example.com");

        assertTrue(result.isPresent());
    }

    @Test
    void testGetUtilisateurByEmail_emailNull_retourneEmpty() {
        Optional<UtilisateurDTO> result = utilisateurService.getUtilisateurByEmail(null);

        assertTrue(result.isEmpty());
        verify(utilisateurRepository, never()).findByEmailIgnoreCase(anyString());
    }

    @Test
    void testGetUtilisateurByEmail_emailBlanc_retourneEmpty() {
        Optional<UtilisateurDTO> result = utilisateurService.getUtilisateurByEmail("   ");

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetUtilisateurEntityByEmail_trouve() {
        Utilisateur u = new Utilisateur();
        u.setId(1L);
        u.setEmail("test@example.com");
        when(utilisateurRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(u));

        Optional<Utilisateur> result = utilisateurService.getUtilisateurEntityByEmail("Test@Example.com");

        assertTrue(result.isPresent());
    }

    @Test
    void testGetUtilisateurEntityByEmail_emailNull_retourneEmptySansAppelRepo() {
        Optional<Utilisateur> result = utilisateurService.getUtilisateurEntityByEmail(null);

        assertTrue(result.isEmpty());
        verify(utilisateurRepository, never()).findByEmailIgnoreCase(anyString());
    }

    @Test
    void testFindByEmail_trouve() {
        Utilisateur u = new Utilisateur();
        u.setId(1L);
        when(utilisateurRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(u));

        Optional<Utilisateur> result = utilisateurService.findByEmail("test@example.com");

        assertTrue(result.isPresent());
    }

    @Test
    void testFindByNomPrenom_trouve() {
        Utilisateur u = new Utilisateur();
        u.setId(1L);
        when(utilisateurRepository.findByNomIgnoreCaseAndPrenomIgnoreCase("Dupont", "Jean")).thenReturn(Optional.of(u));

        Optional<Utilisateur> result = utilisateurService.findByNomPrenom("Dupont", "Jean");

        assertTrue(result.isPresent());
    }

    @Test
    void testFindByNomPrenom_nomNull_retourneEmptySansAppelRepo() {
        Optional<Utilisateur> result = utilisateurService.findByNomPrenom(null, "Jean");

        assertTrue(result.isEmpty());
        verify(utilisateurRepository, never()).findByNomIgnoreCaseAndPrenomIgnoreCase(anyString(), anyString());
    }

    @Test
    void testFindByNomPrenom_prenomNull_retourneEmptySansAppelRepo() {
        Optional<Utilisateur> result = utilisateurService.findByNomPrenom("Dupont", null);

        assertTrue(result.isEmpty());
    }

    // ---- updateProfilPersonnel ----

    @Test
    void testUpdateProfilPersonnel_metAJourLesChampsPersonnelsUniquement() {
        Utilisateur existing = new Utilisateur();
        existing.setId(1L);
        existing.setNom("Ancien");
        existing.setRole(Role.MEMBRE);
        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(utilisateurRepository.save(any(Utilisateur.class))).thenAnswer(inv -> inv.getArgument(0));

        UtilisateurDTO update = new UtilisateurDTO();
        update.setNom("Nouveau");
        update.setTelephone("0102030405");

        utilisateurService.updateProfilPersonnel(1L, update);

        assertEquals("Nouveau", existing.getNom());
        assertEquals("0102030405", existing.getTelephone());
        assertEquals(Role.MEMBRE, existing.getRole());
        verify(utilisateurRepository).save(existing);
    }

    @Test
    void testUpdateProfilPersonnel_utilisateurIntrouvable_neSauvegardePas() {
        when(utilisateurRepository.findById(99L)).thenReturn(Optional.empty());

        utilisateurService.updateProfilPersonnel(99L, new UtilisateurDTO());

        verify(utilisateurRepository, never()).save(any());
    }

    // ---- changerMotDePassePersonnel ----

    @Test
    void testChangerMotDePassePersonnel_succes_retourneTrue() {
        Utilisateur existing = new Utilisateur();
        existing.setId(1L);
        existing.setPassword("hashedOld");
        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("old", "hashedOld")).thenReturn(true);
        when(passwordEncoder.encode("new")).thenReturn("hashedNew");

        boolean result = utilisateurService.changerMotDePassePersonnel(1L, "old", "new");

        assertTrue(result);
        assertEquals("hashedNew", existing.getPassword());
        assertFalse(existing.isPasswordTemporaire());
        verify(utilisateurRepository).save(existing);
    }

    @Test
    void testChangerMotDePassePersonnel_mauvaisMotDePasseActuel_retourneFalse() {
        Utilisateur existing = new Utilisateur();
        existing.setId(1L);
        existing.setPassword("hashedOld");
        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("wrong", "hashedOld")).thenReturn(false);

        boolean result = utilisateurService.changerMotDePassePersonnel(1L, "wrong", "new");

        assertFalse(result);
        verify(utilisateurRepository, never()).save(any());
    }

    @Test
    void testChangerMotDePassePersonnel_utilisateurIntrouvable_retourneFalse() {
        when(utilisateurRepository.findById(99L)).thenReturn(Optional.empty());

        boolean result = utilisateurService.changerMotDePassePersonnel(99L, "old", "new");

        assertFalse(result);
    }

    // ---- save (bas niveau) ----

    @Test
    void testSave_normaliseEmailEnMinuscule() {
        Utilisateur u = new Utilisateur();
        u.setEmail("Test@EXAMPLE.com");
        when(utilisateurRepository.save(any(Utilisateur.class))).thenAnswer(inv -> inv.getArgument(0));

        Utilisateur result = utilisateurService.save(u);

        assertEquals("test@example.com", result.getEmail());
    }

    // ---- login : cas complementaires ----

    @Test
    void testLogin_adminSansClub_refuse() {
        Utilisateur user = new Utilisateur();
        user.setId(1L);
        user.setEmail("admin@example.com");
        user.setPassword("hashed");
        user.setRole(Role.ADMIN);

        when(utilisateurRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "hashed")).thenReturn(true);

        Optional<UtilisateurDTO> result = utilisateurService.login("admin@example.com", "secret");

        assertFalse(result.isPresent());
    }

    @Test
    void testLogin_emailAvecClubIdEtMismatch_refuse() {
        Utilisateur user = new Utilisateur();
        user.setId(1L);
        user.setEmail("admin@example.com");
        user.setPassword("hashed");
        user.setRole(Role.ADMIN);
        club.taekwondo.entity.jpa.Club club = new club.taekwondo.entity.jpa.Club();
        club.setId(10L);
        user.setClub(club);

        when(utilisateurRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "hashed")).thenReturn(true);

        Optional<UtilisateurDTO> result = utilisateurService.login("admin@example.com|99", "secret");

        assertFalse(result.isPresent());
    }

    @Test
    void testLogin_emailAvecClubIdMatch_bugConnu_rechercheAvecLeSuffixeEtEchoueQuandMeme() {
        // Bug connu : le split "email|clubId" (lignes 273+) intervient APRES l'appel au
        // repository (ligne 262), qui reçoit donc "admin@example.com|10" tel quel.
        // La convention "login multi-club via email|clubId" ne peut donc jamais fonctionner :
        // findByEmailIgnoreCase ne trouvera jamais cet email compose en base.
        Utilisateur user = new Utilisateur();
        user.setId(1L);
        user.setEmail("admin@example.com");
        user.setPassword("hashed");
        user.setRole(Role.ADMIN);
        club.taekwondo.entity.jpa.Club club = new club.taekwondo.entity.jpa.Club();
        club.setId(10L);
        user.setClub(club);

        when(utilisateurRepository.findByEmailIgnoreCase("admin@example.com|10")).thenReturn(Optional.empty());

        Optional<UtilisateurDTO> result = utilisateurService.login("admin@example.com|10", "secret");

        assertFalse(result.isPresent());
        verify(utilisateurRepository, never()).findByEmailIgnoreCase("admin@example.com");
    }
}

