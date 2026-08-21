package club.taekwondo.service.jpa;

import club.taekwondo.dto.ProduitDTO;
import club.taekwondo.entity.jpa.Club;
import club.taekwondo.entity.jpa.Produit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

class ProduitServiceTest extends AbstractServiceIntegrationTest {

    @Autowired
    private ProduitService produitService;

    private Club club;

    @BeforeEach
    void setupProduits() {
        club = new Club();
        club.setName("Club Produit Test");
        club = clubRepository.save(club);
    }

    private ProduitDTO dto(String nom, double prix, int stock) {
        ProduitDTO dto = new ProduitDTO();
        dto.setNom(nom);
        dto.setPrix(BigDecimal.valueOf(prix));
        dto.setStock(stock);
        dto.setClubId(club.getId());
        return dto;
    }

    @Test
    void createProduit_succes_persisteEtRattacheLeClub() {
        ProduitDTO created = produitService.createProduit(dto("Kimono", 40.0, 10));

        assertNotNull(created.getId());
        assertEquals("Kimono", created.getNom());
        assertEquals(club.getId(), created.getClubId());
    }

    @Test
    void createProduit_nomDejaExistant_leveIllegalArgumentException() {
        produitService.createProduit(dto("Kimono", 40.0, 10));

        assertThrows(IllegalArgumentException.class,
                () -> produitService.createProduit(dto("Kimono", 50.0, 5)));
    }

    @Test
    void createProduit_prixNulOuNegatif_leveIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> produitService.createProduit(dto("Ceinture", 0.0, 10)));
        assertThrows(IllegalArgumentException.class,
                () -> produitService.createProduit(dto("Ceinture", -5.0, 10)));
    }

    @Test
    void createProduit_stockNegatif_leveIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> produitService.createProduit(dto("Ceinture", 10.0, -1)));
    }

    @Test
    void updateProduit_introuvable_leveIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> produitService.updateProduit(999999L, dto("Kimono", 40.0, 10)));
    }

    @Test
    void updateProduit_succes_appliqueLesNouvellesValeurs() {
        ProduitDTO created = produitService.createProduit(dto("Kimono", 40.0, 10));

        ProduitDTO updated = produitService.updateProduit(created.getId(), dto("Kimono Pro", 55.0, 3));

        assertEquals("Kimono Pro", updated.getNom());
        assertEquals(0, BigDecimal.valueOf(55.0).compareTo(updated.getPrix()));
        assertEquals(3, updated.getStock());
    }

    @Test
    void updateProduit_prixInvalide_leveIllegalArgumentException() {
        ProduitDTO created = produitService.createProduit(dto("Kimono", 40.0, 10));

        assertThrows(IllegalArgumentException.class,
                () -> produitService.updateProduit(created.getId(), dto("Kimono", 0.0, 10)));
    }

    @Test
    void deleteProduit_introuvable_leveIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> produitService.deleteProduit(999999L));
    }

    @Test
    void deleteProduit_existant_leSupprime() {
        ProduitDTO created = produitService.createProduit(dto("Kimono", 40.0, 10));

        produitService.deleteProduit(created.getId());

        assertTrue(produitService.getProduitById(created.getId()).isEmpty());
    }

    @Test
    void getProduitsByClubId_neRetourneQueLesProduitsDuClub() {
        produitService.createProduit(dto("Kimono", 40.0, 10));

        Club autreClub = new Club();
        autreClub.setName("Autre Club Produit");
        autreClub = clubRepository.save(autreClub);

        Produit autreProduit = new Produit();
        autreProduit.setNom("Ceinture");
        autreProduit.setPrix(BigDecimal.valueOf(15.0));
        autreProduit.setStock(5);
        autreProduit.setClub(autreClub);
        produitRepository.save(autreProduit);

        assertEquals(1, produitService.getProduitsByClubId(club.getId()).size());
    }

    @Test
    void getAllProduits_retourneTousLesProduits() {
        produitService.createProduit(dto("Kimono", 40.0, 10));
        produitService.createProduit(dto("Ceinture", 15.0, 20));

        assertEquals(2, produitService.getAllProduits().size());
    }
}
