package club.taekwondo.service.jpa;

import club.taekwondo.dto.CommandeDTO;
import club.taekwondo.dto.CommandeUpdateDTO;
import club.taekwondo.entity.jpa.Commande;
import club.taekwondo.repository.jpa.CommandeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CommandeServiceTest {

    @Mock
    private CommandeRepository commandeRepository;

    @InjectMocks
    private CommandeService commandeService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetAllCommandes() {
        Commande commande = new Commande();
        commande.setId(1L);
        commande.setMontantTotal(BigDecimal.valueOf(100));
        commande.setStatut("EN_ATTENTE");

        when(commandeRepository.findAll()).thenReturn(List.of(commande));

        List<CommandeDTO> result = commandeService.getAllCommandes();
        assertEquals(1, result.size());
        assertEquals("EN_ATTENTE", result.get(0).getStatut());
    }

    @Test
    void testGetCommandeById_Found() {
        Commande commande = new Commande();
        commande.setId(1L);
        commande.setStatut("PAYEE");

        when(commandeRepository.findById(1L)).thenReturn(Optional.of(commande));

        Optional<CommandeDTO> optionalDto = commandeService.getCommandeById(1L);
        assertTrue(optionalDto.isPresent());
        assertEquals("PAYEE", optionalDto.get().getStatut());
    }

    @Test
    void testGetCommandeById_NotFound() {
        when(commandeRepository.findById(99L)).thenReturn(Optional.empty());
        Optional<CommandeDTO> optionalDto = commandeService.getCommandeById(99L);
        assertTrue(optionalDto.isEmpty());
    }

    @Test
    void testCreateCommande() {
        CommandeDTO dto = new CommandeDTO();
        dto.setMontantTotal(BigDecimal.valueOf(150));
        dto.setStatut("EN_ATTENTE");

        Commande commande = new Commande();
        commande.setId(1L);

        when(commandeRepository.save(any(Commande.class))).thenReturn(commande);

        CommandeDTO result = commandeService.createCommande(dto);
        assertNotNull(result);
    }

    @Test
    void testMettreAJourCommande() {
        Commande commande = new Commande();
        commande.setId(1L);
        commande.setStatut("EN_ATTENTE");

        CommandeUpdateDTO updateDTO = new CommandeUpdateDTO();
        updateDTO.setStatut("PAYEE");
        updateDTO.setModePaiement("CB");
        updateDTO.setDatePaiement(LocalDate.now());

        when(commandeRepository.findById(1L)).thenReturn(Optional.of(commande));
        when(commandeRepository.save(any(Commande.class))).thenReturn(commande);

        assertDoesNotThrow(() -> commandeService.mettreAJourCommande(1L, updateDTO));
    }

    @Test
    void testDeleteCommande() {
        Commande commande = new Commande();
        commande.setId(1L);

        when(commandeRepository.findById(1L)).thenReturn(Optional.of(commande));
        doNothing().when(commandeRepository).delete(commande);

        assertDoesNotThrow(() -> commandeService.deleteCommande(1L));
        verify(commandeRepository, times(1)).delete(commande);
    }

    @Test
    void testDeleteCommande_NotFound() {
        when(commandeRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> commandeService.deleteCommande(99L));
    }
}