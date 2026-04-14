package club.taekwondo.service.jpa;

import club.taekwondo.dto.AnnulationRequestDTO;
import club.taekwondo.dto.EcheanceDTO;
import club.taekwondo.dto.PaiementDTO;
import club.taekwondo.entity.jpa.Membre;
import club.taekwondo.entity.jpa.Paiement;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.enums.Role;
import club.taekwondo.repository.jpa.MembreRepository;
import club.taekwondo.repository.jpa.PaiementRepository;
import club.taekwondo.repository.jpa.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

import club.taekwondo.service.mongo.ActualiteService;
import club.taekwondo.service.mongo.GalerieService;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration"
})
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class PaiementServiceTest {

    @MockBean
    private ActualiteService actualiteService;

    @MockBean
    private GalerieService galerieService;

    @Autowired
    private PaiementService paiementService;

    @Autowired
    private PaiementRepository paiementRepository;

    @Autowired
    private MembreRepository membreRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private UtilisateurService utilisateurService;

    @Autowired
    private MembreService membreService;

    private Utilisateur parent;
    private Membre enfant;

    @BeforeEach
    void setup() {
        // Nettoyage complet dans l'ordre des dépendances FK
        paiementRepository.deleteAll();
        membreRepository.deleteAll();
        utilisateurRepository.deleteAll();

        // Création d'un utilisateur parent
        parent = new Utilisateur();
        parent.setNom("Testeur");
        parent.setPrenom("Parent");
        parent.setEmail("parent@test.com");
        parent.setPassword("secret");
        parent.setRole(Role.PARENT);
        parent = utilisateurService.save(parent);

        // Création d'un membre enfant
        enfant = new Membre();
        enfant.setNom("Enfant");
        enfant.setPrenom("Demo");
        enfant.setParent(parent);
        enfant = membreService.save(enfant);
    }

    @Test
    void testAjouterPaiementManuelUnique() {
        PaiementDTO dto = new PaiementDTO();
        dto.setUtilisateurId(parent.getId());
        dto.setUtilisateurNom(parent.getNom());
        dto.setUtilisateurPrenom(parent.getPrenom());
        dto.setUtilisateurEmail(parent.getEmail());
        dto.setMembreId(enfant.getId());
        dto.setType("UNIQUE");
        dto.setModePaiement("ESPECES");
        dto.setMontantTotal(100.0);
        dto.setDatePaiement(LocalDate.now().toString());

        Paiement paiement = paiementService.ajouterPaiementManuel(dto);

        assertNotNull(paiement.getId());
        assertEquals("UNIQUE", paiement.getType());
        assertEquals("en attente", paiement.getStatut());
        assertEquals(100.0, paiement.getMontantTotal());
        assertEquals(0, paiement.getEcheancesTotales());
        assertEquals(parent.getId(), paiement.getUtilisateur().getId());
        assertEquals(enfant.getId(), paiement.getMembre().getId());
    }

    @Test
    void testAjouterPaiementManuelAvecEcheances() {
        PaiementDTO dto = new PaiementDTO();
        dto.setUtilisateurId(parent.getId());
        dto.setUtilisateurNom(parent.getNom());
        dto.setUtilisateurPrenom(parent.getPrenom());
        dto.setUtilisateurEmail(parent.getEmail());
        dto.setMembreId(enfant.getId());
        dto.setType("ECHELONNE");
        dto.setModePaiement("VIREMENT");
        dto.setDatePaiement(LocalDate.now().toString());

        EcheanceDTO e1 = new EcheanceDTO();
        e1.setNumero(1);
        e1.setDateEcheance(LocalDate.now().plusDays(30));
        e1.setMontant(50.0);

        EcheanceDTO e2 = new EcheanceDTO();
        e2.setNumero(2);
        e2.setDateEcheance(LocalDate.now().plusDays(60));
        e2.setMontant(50.0);

        dto.setEcheances(List.of(e1, e2));

        Paiement paiement = paiementService.ajouterPaiementManuel(dto);

        assertNotNull(paiement.getId());
        assertEquals("ECHELONNE", paiement.getType());
        assertEquals(2, paiement.getEcheancesTotales());
        assertEquals(100.0, paiement.getMontantTotal());
        assertEquals("en attente", paiement.getStatut());
    }

    @Test
    void testValiderPaiementAdmin() {
        PaiementDTO dto = new PaiementDTO();
        dto.setUtilisateurId(parent.getId());
        dto.setMembreId(enfant.getId());
        dto.setType("UNIQUE");
        dto.setModePaiement("ESPECES");
        dto.setMontantTotal(200.0);
        dto.setDatePaiement(LocalDate.now().toString());

        Paiement paiement = paiementService.ajouterPaiementManuel(dto);
        Paiement valide = paiementService.validerPaiementAdmin(paiement.getId());

        assertEquals("payé", valide.getStatut());
        assertEquals(200.0, valide.getMontantPaye());
        assertEquals(0.0, valide.getMontantRestant());
    }

    @Test
    void testAnnulerPaiement() {
        PaiementDTO dto = new PaiementDTO();
        dto.setUtilisateurId(parent.getId());
        dto.setMembreId(enfant.getId());
        dto.setType("UNIQUE");
        dto.setModePaiement("ESPECES");
        dto.setMontantTotal(150.0);
        dto.setDatePaiement(LocalDate.now().toString());

        Paiement paiement = paiementService.ajouterPaiementManuel(dto);

        AnnulationRequestDTO annulation = new AnnulationRequestDTO();
        annulation.setMotif("Test annulation");
        annulation.setAdminResponsable("admin");
        annulation.setDateAnnulation(LocalDate.now().atStartOfDay());

        PaiementDTO annule = paiementService.annulerPaiement(paiement.getId(), annulation);

        assertEquals("annulé", annule.getStatut());
    }

    @Test
    void testFilterPaiements() {
        PaiementDTO dto = new PaiementDTO();
        dto.setUtilisateurId(parent.getId());
        dto.setMembreId(enfant.getId());
        dto.setType("UNIQUE");
        dto.setModePaiement("VIREMENT");
        dto.setMontantTotal(120.0);
        dto.setDatePaiement(LocalDate.now().toString());

        paiementService.ajouterPaiementManuel(dto);

        List<Paiement> filtre = paiementService.filterPaiements("en attente", "VIREMENT");

        assertFalse(filtre.isEmpty());
        assertEquals("VIREMENT", filtre.get(0).getModePaiement());
    }
}
