package club.taekwondo.service.jpa;

import club.taekwondo.dto.EcheanceDTO;
import club.taekwondo.dto.PaiementDTO;
import club.taekwondo.entity.jpa.Club;
import club.taekwondo.entity.jpa.Commande;
import club.taekwondo.entity.jpa.Echeance;
import club.taekwondo.entity.jpa.Membre;
import club.taekwondo.entity.jpa.Paiement;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.enums.Role;
import club.taekwondo.repository.jpa.ClubRepository;
import club.taekwondo.repository.jpa.CommandeRepository;
import club.taekwondo.repository.jpa.EcheanceRepository;
import club.taekwondo.repository.jpa.EvenementRepository;
import club.taekwondo.repository.jpa.InscriptionEvenementRepository;
import club.taekwondo.repository.jpa.LigneCommandeRepository;
import club.taekwondo.repository.jpa.MembreRepository;
import club.taekwondo.repository.jpa.NotificationRepository;
import club.taekwondo.repository.jpa.PaiementRepository;
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
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class PaiementStripeServiceTest {

    @MockBean
    private ActualiteService actualiteService;

    @MockBean
    private GalerieService galerieService;

    @Autowired
    private PaiementStripeService paiementStripeService;

    @Autowired
    private PaiementService paiementService;

    @Autowired
    private PaiementRepository paiementRepository;

    @Autowired
    private EcheanceRepository echeanceRepository;

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private CommandeRepository commandeRepository;

    @Autowired
    private LigneCommandeRepository ligneCommandeRepository;

    @Autowired
    private MembreRepository membreRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

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

    @BeforeEach
    void setup() {
        inscriptionRepository.deleteAll();
        evenementRepository.deleteAll();
        ligneCommandeRepository.deleteAll();
        echeanceRepository.deleteAll();
        paiementRepository.deleteAll();
        commandeRepository.deleteAll();
        membreRepository.deleteAll();
        notificationRepository.deleteAll();
        utilisateurRepository.deleteAll();
        clubRepository.deleteAll();

        club = new Club();
        club.setName("Club Stripe Test");
        club = clubRepository.save(club);

        parent = new Utilisateur();
        parent.setNom("Testeur");
        parent.setPrenom("Parent");
        parent.setEmail("parent-stripe@test.com");
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
    }

    private Paiement paiementUnique(double montant) {
        PaiementDTO dto = new PaiementDTO();
        dto.setUtilisateurId(parent.getId());
        dto.setUtilisateurNom(parent.getNom());
        dto.setUtilisateurPrenom(parent.getPrenom());
        dto.setUtilisateurEmail(parent.getEmail());
        dto.setMembreId(enfant.getId());
        dto.setType("UNIQUE");
        dto.setModePaiement("VIREMENT");
        dto.setMontantTotal(montant);
        dto.setDatePaiement(LocalDate.now().toString());
        return paiementService.ajouterPaiementManuel(dto);
    }

    private Paiement paiementEchelonne(double m1, double m2) {
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
        e1.setDateEcheance(LocalDate.now().plusDays(10));
        e1.setMontant(m1);
        EcheanceDTO e2 = new EcheanceDTO();
        e2.setNumero(2);
        e2.setDateEcheance(LocalDate.now().plusDays(40));
        e2.setMontant(m2);
        dto.setEcheances(List.of(e1, e2));

        return paiementService.ajouterPaiementManuel(dto);
    }

    @Test
    void saveEcheanceReference_persisteLaReferenceSurLEcheance() {
        Paiement paiement = paiementEchelonne(50.0, 50.0);
        Long echeanceId = paiement.getEcheances().get(0).getId();

        paiementStripeService.saveEcheanceReference(echeanceId, "pi_test_123");

        Echeance reloaded = echeanceRepository.findById(echeanceId).orElseThrow();
        assertEquals("pi_test_123", reloaded.getReference());
    }

    @Test
    void saveEcheanceReference_paymentIntentVide_leveIllegalArgumentException() {
        Paiement paiement = paiementEchelonne(50.0, 50.0);
        Long echeanceId = paiement.getEcheances().get(0).getId();

        assertThrows(IllegalArgumentException.class,
                () -> paiementStripeService.saveEcheanceReference(echeanceId, ""));
    }

    @Test
    void saveEcheanceReference_echeanceIntrouvable_leveNoSuchElementException() {
        assertThrows(NoSuchElementException.class,
                () -> paiementStripeService.saveEcheanceReference(999999L, "pi_test"));
    }

    @Test
    void marquerEcheancePayeeParStripe_paiementUnique_marqueLePaiementEtLaCommande() {
        Paiement paiement = paiementUnique(80.0);
        Commande commande = new Commande();
        commande.setDateCommande(LocalDate.now());
        commande.setMontantTotal(BigDecimal.valueOf(80.0));
        commande.setUtilisateur(parent);
        commande.setClub(club);
        commande.setStatut("EN_ATTENTE");
        commande = commandeRepository.save(commande);

        Paiement managedPaiement = paiementRepository.findById(paiement.getId()).orElseThrow();
        managedPaiement.setCommande(commande);
        paiement = paiementRepository.save(managedPaiement);

        paiementStripeService.marquerEcheancePayeeParStripe(paiement.getId(), null, "pi_unique_1", 8000L);

        Paiement reloaded = paiementRepository.findById(paiement.getId()).orElseThrow();
        assertEquals("payé", reloaded.getStatut());
        assertEquals("CB", reloaded.getModePaiement());
        assertEquals(80.0, reloaded.getMontantPaye());
        assertEquals(0.0, reloaded.getMontantRestant());
        assertEquals("pi_unique_1", reloaded.getPaymentIntentId());

        Commande commandeReloaded = commandeRepository.findById(commande.getId()).orElseThrow();
        assertEquals("PAYEE", commandeReloaded.getStatut());
    }

    @Test
    void marquerEcheancePayeeParStripe_paiementIdNull_neFaitRien() {
        // Ne doit pas lever d'exception : retour silencieux
        paiementStripeService.marquerEcheancePayeeParStripe(null, null, "pi_x", 1000L);
    }

    @Test
    void marquerEcheancePayeeParStripe_paiementIntrouvable_leveNoSuchElementException() {
        assertThrows(NoSuchElementException.class,
                () -> paiementStripeService.marquerEcheancePayeeParStripe(999999L, null, "pi_x", 1000L));
    }

    @Test
    void marquerEcheancePayeeParStripe_echeanceIntrouvable_leveNoSuchElementException() {
        assertThrows(NoSuchElementException.class,
                () -> paiementStripeService.marquerEcheancePayeeParStripe(null, 999999L, "pi_x", 1000L));
    }

    @Test
    void marquerEcheancePayeeParStripe_echeancePartielle_paiementResteEnAttente() {
        Paiement paiement = paiementEchelonne(50.0, 50.0);
        Long premiereEcheanceId = paiement.getEcheances().get(0).getId();

        paiementStripeService.marquerEcheancePayeeParStripe(paiement.getId(), premiereEcheanceId, "pi_ech_1", 5000L);

        Paiement reloaded = paiementService.getById(paiement.getId()).orElseThrow();
        assertEquals("en attente", reloaded.getStatut());
        assertEquals(50.0, reloaded.getMontantPaye());
        assertEquals(50.0, reloaded.getMontantRestant());

        Echeance echeanceReloaded = echeanceRepository.findById(premiereEcheanceId).orElseThrow();
        assertEquals("payé", echeanceReloaded.getStatut());
        assertEquals("CB", echeanceReloaded.getModePaiement());
        assertEquals("pi_ech_1", echeanceReloaded.getReference());
    }

    @Test
    void marquerEcheancePayeeParStripe_toutesLesEcheancesPayees_paiementDevientPaye() {
        Paiement paiement = paiementEchelonne(50.0, 50.0);
        List<Echeance> echeances = paiement.getEcheances();

        paiementStripeService.marquerEcheancePayeeParStripe(paiement.getId(), echeances.get(0).getId(), "pi_1", 5000L);
        paiementStripeService.marquerEcheancePayeeParStripe(paiement.getId(), echeances.get(1).getId(), "pi_2", 5000L);

        Paiement reloaded = paiementService.getById(paiement.getId()).orElseThrow();
        assertEquals("payé", reloaded.getStatut());
        assertEquals(0.0, reloaded.getMontantRestant());
        assertEquals(100.0, reloaded.getMontantPaye());
    }

    @Test
    void marquerEcheancePayeeParStripe_montantIncoherent_neBloquePasLaMiseAJour() {
        // Le service se contente de logger un warning en cas de montant Stripe different
        // du montant attendu de l'echeance : la mise a jour doit quand meme s'appliquer.
        Paiement paiement = paiementEchelonne(50.0, 50.0);
        Long echeanceId = paiement.getEcheances().get(0).getId();

        paiementStripeService.marquerEcheancePayeeParStripe(paiement.getId(), echeanceId, "pi_montant_diff", 999L);

        Echeance reloaded = echeanceRepository.findById(echeanceId).orElseThrow();
        assertEquals("payé", reloaded.getStatut());
    }
}
