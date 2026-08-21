package club.taekwondo.service.jpa;

import club.taekwondo.dto.LigneCommandeDTO;
import club.taekwondo.entity.jpa.Club;
import club.taekwondo.entity.jpa.Commande;
import club.taekwondo.entity.jpa.LigneCommande;
import club.taekwondo.entity.jpa.Membre;
import club.taekwondo.entity.jpa.Paiement;
import club.taekwondo.entity.jpa.Produit;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.enums.Role;
import club.taekwondo.repository.jpa.ClubRepository;
import club.taekwondo.repository.jpa.CommandeRepository;
import club.taekwondo.repository.jpa.EvenementRepository;
import club.taekwondo.repository.jpa.InscriptionEvenementRepository;
import club.taekwondo.repository.jpa.LigneCommandeRepository;
import club.taekwondo.repository.jpa.MembreRepository;
import club.taekwondo.repository.jpa.NotificationRepository;
import club.taekwondo.repository.jpa.PaiementRepository;
import club.taekwondo.repository.jpa.ProduitRepository;
import club.taekwondo.repository.jpa.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class LigneCommandeServiceTest {

    @MockBean
    private ActualiteService actualiteService;

    @MockBean
    private GalerieService galerieService;

    @Autowired
    private LigneCommandeService ligneCommandeService;

    @Autowired
    private LigneCommandeRepository ligneCommandeRepository;

    @Autowired
    private CommandeRepository commandeRepository;

    @Autowired
    private ProduitRepository produitRepository;

    @Autowired
    private PaiementRepository paiementRepository;

    @Autowired
    private MembreRepository membreRepository;

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private InscriptionEvenementRepository inscriptionRepository;

    @Autowired
    private EvenementRepository evenementRepository;

    @Autowired
    private UtilisateurService utilisateurService;

    @Autowired
    private MembreService membreService;

    private Club club;
    private Utilisateur parent;
    private Membre enfant;
    private Produit produit;
    private Commande commande;

    @BeforeEach
    void setup() {
        inscriptionRepository.deleteAll();
        evenementRepository.deleteAll();
        ligneCommandeRepository.deleteAll();
        paiementRepository.deleteAll();
        commandeRepository.deleteAll();
        produitRepository.deleteAll();
        membreRepository.deleteAll();
        notificationRepository.deleteAll();
        utilisateurRepository.deleteAll();
        clubRepository.deleteAll();

        club = new Club();
        club.setName("Club LigneCommande Test");
        club = clubRepository.save(club);

        parent = new Utilisateur();
        parent.setNom("Testeur");
        parent.setPrenom("Parent");
        parent.setEmail("parent-lignecommande@test.com");
        parent.setPassword("secret");
        parent.setRole(Role.PARENT);
        parent.setClub(club);
        parent = utilisateurService.save(parent);

        enfant = new Membre();
        enfant.setNom("Enfant");
        enfant.setPrenom("Demo");
        enfant.setParent(parent);
        enfant.setClub(club);
        enfant = membreService.save(enfant);

        produit = new Produit();
        produit.setNom("Kimono");
        produit.setPrix(BigDecimal.valueOf(40.0));
        produit.setStock(10);
        produit = produitRepository.save(produit);

        commande = new Commande();
        commande.setDateCommande(LocalDate.now());
        commande.setMontantTotal(BigDecimal.ZERO);
        commande.setUtilisateur(parent);
        commande.setClub(club);
        commande.setStatut("EN_ATTENTE");
        commande = commandeRepository.save(commande);
    }

    private Paiement creerPaiementAvecCommande() {
        Paiement paiement = new Paiement();
        paiement.setType("UNIQUE");
        paiement.setModePaiement("ESPECES");
        paiement.setDatePaiement(LocalDate.now());
        paiement.setUtilisateur(parent);
        paiement.setMembre(enfant);
        paiement.setCommande(commande);
        paiement.setMontantTotal(80.0);
        paiement.setMontantPaye(0.0);
        paiement.setMontantRestant(80.0);
        paiement.setStatut("en attente");
        return paiementRepository.save(paiement);
    }

    @Test
    void creerPourPaiement_arrondiLePrixEtLeSousTotal() {
        Paiement paiement = creerPaiementAvecCommande();

        LigneCommande ligne = ligneCommandeService.creerPourPaiement(
                paiement, produit, 33.336, 3, "M", "Rouge", true, "Nom", null);

        assertEquals(33.34, ligne.getPrixUnitaire());
        assertEquals(100.02, ligne.getSousTotal());
    }

    @Test
    void creerPourPaiement_flocageInactif_neConservePasLeFlocage() {
        Paiement paiement = creerPaiementAvecCommande();

        LigneCommande ligne = ligneCommandeService.creerPourPaiement(
                paiement, produit, 40.0, 1, null, null, false, "Nom Ignore", null);

        assertNull(ligne.getFlocage());
    }

    @Test
    void creerPourPaiement_avecBeneficiaire_estRattache() {
        Paiement paiement = creerPaiementAvecCommande();

        LigneCommande ligne = ligneCommandeService.creerPourPaiement(
                paiement, produit, 40.0, 1, null, null, false, null, enfant.getId());

        assertNotNull(ligne.getBeneficiaire());
        assertEquals(enfant.getId(), ligne.getBeneficiaire().getId());
    }

    @Test
    void creerPourPaiement_paiementSansId_leveIllegalArgumentException() {
        Paiement paiement = new Paiement();

        assertThrows(IllegalArgumentException.class, () -> ligneCommandeService.creerPourPaiement(
                paiement, produit, 40.0, 1, null, null, false, null, null));
    }

    @Test
    void creerPourPaiement_paiementSansCommande_leveIllegalArgumentException() {
        Paiement paiement = new Paiement();
        paiement.setType("UNIQUE");
        paiement.setModePaiement("ESPECES");
        paiement.setDatePaiement(LocalDate.now());
        paiement.setUtilisateur(parent);
        paiement.setMembre(enfant);
        paiement.setMontantTotal(50.0);
        paiement.setMontantPaye(0.0);
        paiement.setMontantRestant(50.0);
        paiement.setStatut("en attente");
        paiement = paiementRepository.save(paiement);

        Paiement finalPaiement = paiement;
        assertThrows(IllegalArgumentException.class, () -> ligneCommandeService.creerPourPaiement(
                finalPaiement, produit, 40.0, 1, null, null, false, null, null));
    }

    @Test
    void creerPourPaiement_produitNull_leveIllegalArgumentException() {
        Paiement paiement = creerPaiementAvecCommande();

        assertThrows(IllegalArgumentException.class, () -> ligneCommandeService.creerPourPaiement(
                paiement, null, 40.0, 1, null, null, false, null, null));
    }

    @Test
    void creerPourPaiement_prixNegatif_leveIllegalArgumentException() {
        Paiement paiement = creerPaiementAvecCommande();

        assertThrows(IllegalArgumentException.class, () -> ligneCommandeService.creerPourPaiement(
                paiement, produit, -1.0, 1, null, null, false, null, null));
    }

    @Test
    void creerPourPaiement_quantiteNulleOuNegative_leveIllegalArgumentException() {
        Paiement paiement = creerPaiementAvecCommande();

        assertThrows(IllegalArgumentException.class, () -> ligneCommandeService.creerPourPaiement(
                paiement, produit, 40.0, 0, null, null, false, null, null));
    }

    @Test
    void creerPourPaiement_beneficiaireIntrouvable_leveIllegalArgumentException() {
        Paiement paiement = creerPaiementAvecCommande();

        assertThrows(IllegalArgumentException.class, () -> ligneCommandeService.creerPourPaiement(
                paiement, produit, 40.0, 1, null, null, false, null, 999999L));
    }

    @Test
    void getLignesParCommande_retourneLesLignesDeLaCommande() {
        Paiement paiement = creerPaiementAvecCommande();
        ligneCommandeService.creerPourPaiement(paiement, produit, 40.0, 1, null, null, false, null, null);

        List<LigneCommande> lignes = ligneCommandeService.getLignesParCommande(commande.getId());

        assertEquals(1, lignes.size());
    }

    @Test
    void getLignesParPaiement_paiementSansCommande_retourneListeVide() {
        assertTrue(ligneCommandeService.getLignesParPaiement(null).isEmpty());

        Paiement sansCommande = new Paiement();
        assertTrue(ligneCommandeService.getLignesParPaiement(sansCommande).isEmpty());
    }

    @Test
    void getLignesParPaiement_delegueALaCommandeLiee() {
        Paiement paiement = creerPaiementAvecCommande();
        ligneCommandeService.creerPourPaiement(paiement, produit, 40.0, 2, null, null, false, null, null);

        List<LigneCommande> lignes = ligneCommandeService.getLignesParPaiement(paiement);

        assertEquals(1, lignes.size());
        assertEquals(2, lignes.get(0).getQuantite());
    }

    @Test
    void createLigneCommande_produitIntrouvable_leveRuntimeException() {
        LigneCommandeDTO dto = new LigneCommandeDTO();
        dto.setCommandeId(commande.getId());
        dto.setProduitId(999999L);
        dto.setQuantite(1);
        dto.setPrixUnitaire(40.0);
        dto.setSousTotal(40.0);

        assertThrows(RuntimeException.class, () -> ligneCommandeService.createLigneCommande(dto));
    }

    @Test
    void createLigneCommande_succes_persisteEtRetourneLeDto() {
        LigneCommandeDTO dto = new LigneCommandeDTO();
        dto.setCommandeId(commande.getId());
        dto.setProduitId(produit.getId());
        dto.setQuantite(2);
        dto.setPrixUnitaire(40.0);
        dto.setSousTotal(80.0);
        dto.setBeneficiaireId(enfant.getId());

        LigneCommandeDTO created = ligneCommandeService.createLigneCommande(dto);

        assertNotNull(created.getId());
        assertEquals(commande.getId(), created.getCommandeId());
        assertEquals(enfant.getId(), created.getBeneficiaireId());
    }

    @Test
    void updateLigneCommande_introuvable_leveRuntimeException() {
        LigneCommandeDTO dto = new LigneCommandeDTO();
        dto.setCommandeId(commande.getId());
        dto.setProduitId(produit.getId());
        dto.setQuantite(1);
        dto.setPrixUnitaire(40.0);
        dto.setSousTotal(40.0);

        assertThrows(RuntimeException.class, () -> ligneCommandeService.updateLigneCommande(999999L, dto));
    }

    @Test
    void deleteLigneCommande_supprimeLaLigne() {
        LigneCommandeDTO dto = new LigneCommandeDTO();
        dto.setCommandeId(commande.getId());
        dto.setProduitId(produit.getId());
        dto.setQuantite(1);
        dto.setPrixUnitaire(40.0);
        dto.setSousTotal(40.0);
        LigneCommandeDTO created = ligneCommandeService.createLigneCommande(dto);

        ligneCommandeService.deleteLigneCommande(created.getId());

        assertTrue(ligneCommandeService.getLigneCommandeById(created.getId()).isEmpty());
    }

    @Test
    void getLignesCommandeByClubId_filtreParClub() {
        Paiement paiement = creerPaiementAvecCommande();
        ligneCommandeService.creerPourPaiement(paiement, produit, 40.0, 1, null, null, false, null, null);

        List<LigneCommandeDTO> lignes = ligneCommandeService.getLignesCommandeByClubId(club.getId());

        assertEquals(1, lignes.size());
    }
}
