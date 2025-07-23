package club.taekwondo.service.jpa;

import club.taekwondo.dto.ProduitDTO;
import club.taekwondo.entity.jpa.Produit;
import club.taekwondo.repository.jpa.ProduitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProduitServiceTest {

    @Mock
    private ProduitRepository produitRepository;

    @InjectMocks
    private ProduitService produitService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetAllProduits() {
        Produit produit = new Produit();
        produit.setId(1L);
        produit.setNom("Dobok");
        produit.setPrix(BigDecimal.valueOf(30));
        produit.setStock(10);

        when(produitRepository.findAll()).thenReturn(List.of(produit));

        List<ProduitDTO> result = produitService.getAllProduits();
        assertEquals(1, result.size());
        assertEquals("Dobok", result.get(0).getNom());
    }

    @Test
    void testGetProduitById_Found() {
        Produit produit = new Produit();
        produit.setId(1L);
        produit.setNom("Ceinture");

        when(produitRepository.findById(1L)).thenReturn(Optional.of(produit));

        Optional<ProduitDTO> optionalDto = produitService.getProduitById(1L);
        assertTrue(optionalDto.isPresent());
        ProduitDTO dto = optionalDto.get();
        assertEquals("Ceinture", dto.getNom());
    }

    @Test
    void testGetProduitById_NotFound() {
        when(produitRepository.findById(99L)).thenReturn(Optional.empty());
        Optional<ProduitDTO> optionalDto = produitService.getProduitById(99L);
        assertTrue(optionalDto.isEmpty());
    }

    @Test
    void testCreateProduit() {
        ProduitDTO dto = new ProduitDTO();
        dto.setNom("Plastron");
        dto.setPrix(BigDecimal.valueOf(50));
        dto.setStock(5);

        Produit produit = new Produit();
        produit.setId(1L);

        when(produitRepository.save(any(Produit.class))).thenReturn(produit);

        ProduitDTO result = produitService.createProduit(dto);
        assertNotNull(result);
    }

    @Test
    void testUpdateProduit() {
        Produit produit = new Produit();
        produit.setId(1L);
        produit.setNom("Dobok");

        ProduitDTO dto = new ProduitDTO();
        dto.setNom("Dobok Premium");
        dto.setPrix(BigDecimal.valueOf(40));
        dto.setStock(8);

        when(produitRepository.existsById(1L)).thenReturn(true);
        when(produitRepository.save(any(Produit.class))).thenReturn(produit);

        ProduitDTO result = produitService.updateProduit(1L, dto);
        assertNotNull(result);
    }

    @Test
    void testUpdateProduit_NotFound() {
        ProduitDTO dto = new ProduitDTO();
        when(produitRepository.existsById(99L)).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> produitService.updateProduit(99L, dto));
    }

    @Test
    void testDeleteProduit() {
        when(produitRepository.existsById(1L)).thenReturn(true);
        doNothing().when(produitRepository).deleteById(1L);

        assertDoesNotThrow(() -> produitService.deleteProduit(1L));
        verify(produitRepository, times(1)).deleteById(1L);
    }

    @Test
    void testDeleteProduit_NotFound() {
        when(produitRepository.existsById(99L)).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> produitService.deleteProduit(99L));
    }
}