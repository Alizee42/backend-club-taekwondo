package club.taekwondo.service.jpa;

import club.taekwondo.dto.MembreDTO;
import club.taekwondo.entity.jpa.Membre;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.repository.jpa.MembreRepository;
import club.taekwondo.repository.jpa.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MembreServiceTest {

    private MembreRepository membreRepository;
    private UtilisateurRepository utilisateurRepository;
    private MembreService membreService;

    @BeforeEach
    void setUp() {
        membreRepository = mock(MembreRepository.class);
        utilisateurRepository = mock(UtilisateurRepository.class);
        membreService = new MembreService(membreRepository, utilisateurRepository);
    }

    @Test
    void testGetAllMembres() {
        Membre membre1 = new Membre();
        membre1.setId(1L);
        membre1.setNom("Dupont");
        membre1.setPrenom("Jean");

        Membre membre2 = new Membre();
        membre2.setId(2L);
        membre2.setNom("Durand");
        membre2.setPrenom("Paul");

        when(membreRepository.findAll()).thenReturn(List.of(membre1, membre2));

        List<MembreDTO> result = membreService.getAllMembres();

        assertEquals(2, result.size());
        assertEquals("Dupont", result.get(0).getNom());
        assertEquals("Durand", result.get(1).getNom());
    }

    @Test
    void testGetMembreById_Exist() {
        Membre membre = new Membre();
        membre.setId(1L);
        membre.setNom("Dupont");

        when(membreRepository.findById(1L)).thenReturn(Optional.of(membre));

        Optional<MembreDTO> result = membreService.getMembreById(1L);

        assertTrue(result.isPresent());
        assertEquals("Dupont", result.get().getNom());
    }

    @Test
    void testGetMembreById_NotExist() {
        when(membreRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<MembreDTO> result = membreService.getMembreById(99L);

        assertFalse(result.isPresent());
    }

    @Test
    void testGetMembreByEmail_Exist() {
        Membre membre = new Membre();
        membre.setId(1L);
        membre.setNom("Dupont");

        when(membreRepository.findByUtilisateurEmail("test@email.com")).thenReturn(Optional.of(membre));

        Optional<MembreDTO> result = membreService.getMembreByEmail("test@email.com");

        assertTrue(result.isPresent());
        assertEquals("Dupont", result.get().getNom());
    }

    @Test
    void testGetMembreByEmail_NotExist() {
        when(membreRepository.findByUtilisateurEmail("notfound@email.com")).thenReturn(Optional.empty());

        Optional<MembreDTO> result = membreService.getMembreByEmail("notfound@email.com");

        assertFalse(result.isPresent());
    }

    @Test
    void testCreateMembre_Success() {
        MembreDTO dto = new MembreDTO();
        dto.setNom("Dupont");
        dto.setPrenom("Jean");
        dto.setDateNaissance(LocalDate.of(2000, 1, 1));
        dto.setCeinture("Noire");
        dto.setNumeroLicence("12345");
        dto.setUtilisateurId(10L);

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(10L);

        when(utilisateurRepository.findById(10L)).thenReturn(Optional.of(utilisateur));
        when(membreRepository.save(any(Membre.class))).thenAnswer(invocation -> {
            Membre m = invocation.getArgument(0);
            m.setId(1L);
            return m;
        });

        MembreDTO result = membreService.createMembre(dto);

        assertEquals("Dupont", result.getNom());
        assertEquals("Jean", result.getPrenom());
        assertEquals("Noire", result.getCeinture());
        assertEquals("12345", result.getNumeroLicence());
        assertEquals(10L, result.getUtilisateurId());
    }

    @Test
    void testCreateMembre_UtilisateurNonTrouve() {
        MembreDTO dto = new MembreDTO();
        dto.setNom("Dupont");
        dto.setPrenom("Jean");
        dto.setUtilisateurId(99L);

        when(utilisateurRepository.findById(99L)).thenReturn(Optional.empty());

        Exception exception = assertThrows(RuntimeException.class, () -> {
            membreService.createMembre(dto);
        });

        assertEquals("Utilisateur non trouvé avec l'ID : 99", exception.getMessage());
    }

    @Test
    void testCreateMembre_SansUtilisateur() {
        MembreDTO dto = new MembreDTO();
        dto.setNom("Dupont");
        dto.setPrenom("Jean");
        // Pas de utilisateurId

        Exception exception = assertThrows(RuntimeException.class, () -> {
            membreService.createMembre(dto);
        });

        assertEquals("Impossible de créer un membre sans utilisateur associé.", exception.getMessage());
    }

    @Test
    void testUpdateMembre_Success() {
        Membre membre = new Membre();
        membre.setId(1L);
        membre.setNom("AncienNom");
        membre.setPrenom("AncienPrenom");

        MembreDTO dto = new MembreDTO();
        dto.setNom("NouveauNom");
        dto.setPrenom("NouveauPrenom");
        dto.setDateNaissance(LocalDate.of(2010, 5, 5));
        dto.setCeinture("Bleue");
        dto.setNumeroLicence("54321");

        when(membreRepository.findById(1L)).thenReturn(Optional.of(membre));
        when(membreRepository.save(any(Membre.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MembreDTO result = membreService.updateMembre(1L, dto);

        assertEquals("NouveauNom", result.getNom());
        assertEquals("NouveauPrenom", result.getPrenom());
        assertEquals("Bleue", result.getCeinture());
        assertEquals("54321", result.getNumeroLicence());
        assertEquals(LocalDate.of(2010, 5, 5), result.getDateNaissance());
    }

    @Test
    void testUpdateMembre_NotExist() {
        MembreDTO dto = new MembreDTO();
        dto.setNom("NouveauNom");

        when(membreRepository.findById(99L)).thenReturn(Optional.empty());

        Exception exception = assertThrows(RuntimeException.class, () -> {
            membreService.updateMembre(99L, dto);
        });

        assertEquals("Membre non trouvé avec l'ID : 99", exception.getMessage());
    }

    @Test
    void testDeleteMembre_Success() {
        when(membreRepository.existsById(1L)).thenReturn(true);
        doNothing().when(membreRepository).deleteById(1L);

        membreService.deleteMembre(1L);

        verify(membreRepository, times(1)).deleteById(1L);
    }

    @Test
    void testDeleteMembre_NotExist() {
        when(membreRepository.existsById(99L)).thenReturn(false);

        Exception exception = assertThrows(RuntimeException.class, () -> {
            membreService.deleteMembre(99L);
        });

        assertEquals("Membre non trouvé avec l'ID : 99", exception.getMessage());
    }
}