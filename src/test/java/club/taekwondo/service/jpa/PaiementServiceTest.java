package club.taekwondo.service.jpa;

import club.taekwondo.dto.*;
import club.taekwondo.entity.jpa.Paiement;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.repository.jpa.PaiementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaiementServiceTest {

    @Mock
    private PaiementRepository paiementRepository;
    @Mock
    private EcheanceService echeanceService;
    @Mock
    private UtilisateurService utilisateurService;

    @InjectMocks
    private PaiementService paiementService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetAllWithEcheances() {
        Paiement paiement = new Paiement();
        paiement.setId(1L);
        // Ajout utilisateur pour éviter NullPointerException
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(1L);
        utilisateur.setNom("Test");
        utilisateur.setPrenom("Test");
        utilisateur.setEmail("test@test.com");
        paiement.setUtilisateur(utilisateur);

        when(paiementRepository.findAllWithEcheances()).thenReturn(List.of(paiement));

        List<PaiementDTO> result = paiementService.getAllWithEcheances();
        assertEquals(1, result.size());
    }

    @Test
    void testGetById_Found() {
        Paiement paiement = new Paiement();
        paiement.setId(1L);
        when(paiementRepository.findById(1L)).thenReturn(Optional.of(paiement));

        Optional<Paiement> result = paiementService.getById(1L);
        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
    }

    @Test
    void testGetById_NotFound() {
        when(paiementRepository.findById(99L)).thenReturn(Optional.empty());
        Optional<Paiement> result = paiementService.getById(99L);
        assertFalse(result.isPresent());
    }

    @Test
    void testAjouterPaiementManuel_Success() {
        PaiementDTO dto = new PaiementDTO();
        dto.setType("unique");
        dto.setModePaiement("CB");
        dto.setMontantTotal(100.0);
        dto.setUtilisateurId(1L);

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(1L);

        when(utilisateurService.getUtilisateurEntityById(1L)).thenReturn(Optional.of(utilisateur));
        when(paiementRepository.save(any(Paiement.class))).thenAnswer(inv -> {
            Paiement p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        Paiement paiement = paiementService.ajouterPaiementManuel(dto);
        assertNotNull(paiement);
        assertEquals(1L, paiement.getId());
        assertEquals("unique", paiement.getType());
    }

    @Test
    void testAjouterPaiementManuel_UtilisateurNonTrouve() {
        PaiementDTO dto = new PaiementDTO();
        dto.setType("unique");
        dto.setModePaiement("CB");
        dto.setMontantTotal(100.0);
        dto.setUtilisateurId(99L);

        when(utilisateurService.getUtilisateurEntityById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> paiementService.ajouterPaiementManuel(dto));
    }

    @Test
    void testAnnulerPaiement_Success() {
        Paiement paiement = new Paiement();
        paiement.setId(1L);
        paiement.setStatut("en attente");
        paiement.setMontantTotal(100.0);
        paiement.setMontantRestant(100.0);
        // Ajout utilisateur pour éviter NullPointerException
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(1L);
        utilisateur.setNom("Test");
        utilisateur.setPrenom("Test");
        utilisateur.setEmail("test@test.com");
        paiement.setUtilisateur(utilisateur);

        AnnulationRequestDTO request = new AnnulationRequestDTO();
        request.setMotif("Erreur");

        when(paiementRepository.findById(1L)).thenReturn(Optional.of(paiement));
        when(paiementRepository.save(any(Paiement.class))).thenReturn(paiement);

        PaiementDTO result = paiementService.annulerPaiement(1L, request);
        assertNotNull(result);
        assertEquals("annulé", result.getStatut());
    }

    @Test
    void testAnnulerPaiement_AlreadyPaid() {
        Paiement paiement = new Paiement();
        paiement.setId(1L);
        paiement.setStatut("payé");
        // Ajout utilisateur pour éviter NullPointerException
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(1L);
        utilisateur.setNom("Test");
        utilisateur.setPrenom("Test");
        utilisateur.setEmail("test@test.com");
        paiement.setUtilisateur(utilisateur);

        AnnulationRequestDTO request = new AnnulationRequestDTO();
        request.setMotif("Erreur");

        when(paiementRepository.findById(1L)).thenReturn(Optional.of(paiement));

        assertThrows(IllegalStateException.class, () -> paiementService.annulerPaiement(1L, request));
    }

    @Test
    void testAnnulerPaiement_NotFound() {
        AnnulationRequestDTO request = new AnnulationRequestDTO();
        request.setMotif("Erreur");

        when(paiementRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> paiementService.annulerPaiement(99L, request));
    }

    @Test
    void testDelete_Success() {
        Paiement paiement = new Paiement();
        paiement.setId(1L);

        when(paiementRepository.findById(1L)).thenReturn(Optional.of(paiement));
        doNothing().when(paiementRepository).deleteById(1L);

        assertDoesNotThrow(() -> paiementService.delete(1L));
        verify(paiementRepository, times(1)).deleteById(1L);
    }

    @Test
    void testDelete_NotFound() {
        when(paiementRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> paiementService.delete(99L));
    }
}