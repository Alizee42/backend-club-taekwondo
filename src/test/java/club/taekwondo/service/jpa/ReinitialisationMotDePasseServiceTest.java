package club.taekwondo.service.jpa;

import club.taekwondo.dto.ReinitialisationMotDePasseDTO;
import club.taekwondo.entity.jpa.ReinitialisationMotDePasse;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.repository.jpa.ReinitialisationMotDePasseRepository;
import club.taekwondo.repository.jpa.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ReinitialisationMotDePasseServiceTest {

    private ReinitialisationMotDePasseRepository repository;
    private UtilisateurRepository utilisateurRepository;
    private ReinitialisationMotDePasseService service;

    @BeforeEach
    void setUp() {
        repository = mock(ReinitialisationMotDePasseRepository.class);
        utilisateurRepository = mock(UtilisateurRepository.class);
        service = new ReinitialisationMotDePasseService(repository, utilisateurRepository); // ✅ injection par constructeur
    }

    @Test
    void testDemanderReinitialisation() {
        Utilisateur user = new Utilisateur();
        user.setId(1L);
        user.setEmail("test@example.com");

        when(utilisateurRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        service.demanderReinitialisation("test@example.com");

        ArgumentCaptor<ReinitialisationMotDePasse> captor = ArgumentCaptor.forClass(ReinitialisationMotDePasse.class);
        verify(repository).save(captor.capture());

        ReinitialisationMotDePasse demande = captor.getValue();
        assertNotNull(demande.getToken());
        assertFalse(demande.isUtilise());
        assertEquals(user, demande.getUtilisateur());
        assertTrue(demande.getDateExpiration().isAfter(LocalDateTime.now()));
    }

    @Test
    void testValiderToken_valide() {
        ReinitialisationMotDePasse demande = new ReinitialisationMotDePasse();
        demande.setToken("abc123");
        demande.setUtilise(false);
        demande.setDateExpiration(LocalDateTime.now().plusMinutes(30));

        when(repository.findByToken("abc123")).thenReturn(Optional.of(demande));

        boolean result = service.validerToken("abc123");

        assertTrue(result);
        assertTrue(demande.isUtilise());
    }

    @Test
    void testValiderToken_expire() {
        ReinitialisationMotDePasse demande = new ReinitialisationMotDePasse();
        demande.setToken("expired");
        demande.setUtilise(false);
        demande.setDateExpiration(LocalDateTime.now().minusMinutes(10));

        when(repository.findByToken("expired")).thenReturn(Optional.of(demande));

        boolean result = service.validerToken("expired");

        assertFalse(result);
        assertFalse(demande.isUtilise());
    }

    @Test
    void testGetByToken() {
        ReinitialisationMotDePasse demande = new ReinitialisationMotDePasse();
        demande.setId(1L);
        demande.setToken("token123");
        demande.setDateExpiration(LocalDateTime.now().plusHours(1));
        demande.setUtilise(false);
        Utilisateur user = new Utilisateur();
        user.setId(5L);
        demande.setUtilisateur(user);

        when(repository.findByToken("token123")).thenReturn(Optional.of(demande));

        Optional<ReinitialisationMotDePasseDTO> result = service.getByToken("token123");

        assertTrue(result.isPresent());
        assertEquals("token123", result.get().getToken());
        assertEquals(5L, result.get().getUtilisateurId());
    }
}
