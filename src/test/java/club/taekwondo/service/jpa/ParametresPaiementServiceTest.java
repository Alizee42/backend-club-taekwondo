package club.taekwondo.service.jpa;

import club.taekwondo.dto.ParametresPaiementDTO;
import club.taekwondo.entity.jpa.ParametresPaiement;
import club.taekwondo.repository.jpa.ParametresPaiementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ParametresPaiementServiceTest {

    @Mock
    private ParametresPaiementRepository parametresPaiementRepository;

    @InjectMocks
    private ParametresPaiementService parametresPaiementService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetParametresPaiement_found() {
        ParametresPaiement entity = new ParametresPaiement();
        entity.setId(1L);
        entity.setMontantCotisation(100.0);
        entity.setVirement(true);
        entity.setEspeces(false);
        entity.setStripe(true);
        entity.setModePaiementParDefaut("CB");
        entity.setEcheancesAutorisees(3);
        entity.setIntervalleEcheance("mois");

        when(parametresPaiementRepository.findById(1L)).thenReturn(Optional.of(entity));

        ParametresPaiementDTO dto = parametresPaiementService.getParametresPaiement();
        assertEquals(100.0, dto.getMontantCotisation());
        assertTrue(dto.isVirement());
        assertFalse(dto.isEspeces());
        assertEquals("CB", dto.getModePaiementParDefaut());
        assertEquals(3, dto.getEcheancesAutorisees());
        assertEquals("mois", dto.getIntervalleEcheance());
    }

    @Test
    void testGetParametresPaiement_notFound() {
        when(parametresPaiementRepository.findById(1L)).thenReturn(Optional.empty());
        ParametresPaiementDTO dto = parametresPaiementService.getParametresPaiement();
        assertNotNull(dto);
    }

    @Test
    void testUpdateParametresPaiement() {
        ParametresPaiementDTO dto = new ParametresPaiementDTO();
        dto.setMontantCotisation(200.0);
        dto.setVirement(false);
        dto.setEspeces(true);
        dto.setStripe(false);
        dto.setModePaiementParDefaut("VIREMENT");
        dto.setEcheancesAutorisees(2);
        dto.setIntervalleEcheance("trimestre");

        ParametresPaiement saved = new ParametresPaiement();
        saved.setId(1L);

        when(parametresPaiementRepository.save(any(ParametresPaiement.class))).thenReturn(saved);

        assertDoesNotThrow(() -> parametresPaiementService.updateParametresPaiement(dto));
        verify(parametresPaiementRepository, times(1)).save(any(ParametresPaiement.class));
    }
}