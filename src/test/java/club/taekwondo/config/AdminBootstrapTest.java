package club.taekwondo.config;

import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.enums.Role;
import club.taekwondo.repository.jpa.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapTest {

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AdminBootstrap adminBootstrap;

    @BeforeEach
    void setUp() {
        adminBootstrap = new AdminBootstrap();
    }

    private CommandLineRunner runner() {
        return adminBootstrap.initAdmin(utilisateurRepository, passwordEncoder);
    }

    @Test
    void run_variablesAbsentes_neCreeAucunCompte() throws Exception {
        runner().run();

        verify(utilisateurRepository, never()).save(any());
    }

    @Test
    void run_superAdminEmailSeul_sansPassword_neCreeRien() throws Exception {
        ReflectionTestUtils.setField(adminBootstrap, "superAdminEmail", "super@test.com");

        runner().run();

        verify(utilisateurRepository, never()).save(any());
    }

    @Test
    void run_superAdminDejaPresent_neRecreePas() throws Exception {
        ReflectionTestUtils.setField(adminBootstrap, "superAdminEmail", "super@test.com");
        ReflectionTestUtils.setField(adminBootstrap, "superAdminPassword", "secret");
        when(utilisateurRepository.findByEmail("super@test.com")).thenReturn(Optional.of(new Utilisateur()));

        runner().run();

        verify(utilisateurRepository, never()).save(any());
    }

    @Test
    void run_superAdminAbsent_leCreeAvecRoleEtMotDePasseEncode() throws Exception {
        ReflectionTestUtils.setField(adminBootstrap, "superAdminEmail", "super@test.com");
        ReflectionTestUtils.setField(adminBootstrap, "superAdminPassword", "secret");
        when(utilisateurRepository.findByEmail("super@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret")).thenReturn("hashed");

        runner().run();

        ArgumentCaptor<Utilisateur> captor = ArgumentCaptor.forClass(Utilisateur.class);
        verify(utilisateurRepository).save(captor.capture());
        assertEquals("super@test.com", captor.getValue().getEmail());
        assertEquals("hashed", captor.getValue().getPassword());
        assertEquals(Role.SUPER_ADMIN, captor.getValue().getRole());
    }

    @Test
    void run_adminAbsent_leCreeAvecRoleEtMotDePasseEncode() throws Exception {
        ReflectionTestUtils.setField(adminBootstrap, "adminEmail", "admin@test.com");
        ReflectionTestUtils.setField(adminBootstrap, "adminPassword", "secret");
        when(utilisateurRepository.findByEmail("admin@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret")).thenReturn("hashed");

        runner().run();

        ArgumentCaptor<Utilisateur> captor = ArgumentCaptor.forClass(Utilisateur.class);
        verify(utilisateurRepository).save(captor.capture());
        assertEquals("admin@test.com", captor.getValue().getEmail());
        assertEquals(Role.ADMIN, captor.getValue().getRole());
    }

    @Test
    void run_adminDejaPresent_neRecreePas() throws Exception {
        ReflectionTestUtils.setField(adminBootstrap, "adminEmail", "admin@test.com");
        ReflectionTestUtils.setField(adminBootstrap, "adminPassword", "secret");
        when(utilisateurRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(new Utilisateur()));

        runner().run();

        verify(utilisateurRepository, never()).save(any());
    }

    @Test
    void run_superAdminEtAdminAbsents_creeLesDeux() throws Exception {
        ReflectionTestUtils.setField(adminBootstrap, "superAdminEmail", "super@test.com");
        ReflectionTestUtils.setField(adminBootstrap, "superAdminPassword", "secret1");
        ReflectionTestUtils.setField(adminBootstrap, "adminEmail", "admin@test.com");
        ReflectionTestUtils.setField(adminBootstrap, "adminPassword", "secret2");
        when(utilisateurRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");

        runner().run();

        verify(utilisateurRepository, org.mockito.Mockito.times(2)).save(any());
    }
}
