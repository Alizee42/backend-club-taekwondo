package club.taekwondo.service.jpa;

import club.taekwondo.dto.AnnulationRequestDTO;
import club.taekwondo.dto.DashboardStatsDTO;
import club.taekwondo.dto.EcheanceDTO;
import club.taekwondo.dto.PaiementDTO;
import club.taekwondo.entity.jpa.Club;
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

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class PaiementStatsServiceTest {

    @MockBean
    private ActualiteService actualiteService;

    @MockBean
    private GalerieService galerieService;

    @Autowired
    private PaiementStatsService paiementStatsService;

    @Autowired
    private PaiementService paiementService;

    @Autowired
    private EcheanceService echeanceService;

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
        club.setName("Club Stats Test");
        club = clubRepository.save(club);

        parent = new Utilisateur();
        parent.setNom("Testeur");
        parent.setPrenom("Parent");
        parent.setEmail("parent-stats@test.com");
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

    private Paiement paiementUnique(double montant, String statut) {
        PaiementDTO dto = new PaiementDTO();
        dto.setUtilisateurId(parent.getId());
        dto.setUtilisateurNom(parent.getNom());
        dto.setUtilisateurPrenom(parent.getPrenom());
        dto.setUtilisateurEmail(parent.getEmail());
        dto.setMembreId(enfant.getId());
        dto.setType("UNIQUE");
        dto.setModePaiement("ESPECES");
        dto.setMontantTotal(montant);
        dto.setDatePaiement(LocalDate.now().toString());

        Paiement paiement = paiementService.ajouterPaiementManuel(dto);

        if ("payé".equals(statut)) {
            paiement = paiementService.validerPaiementAdmin(paiement.getId());
        } else if ("annulé".equals(statut)) {
            AnnulationRequestDTO annulation = new AnnulationRequestDTO();
            annulation.setMotif("Test");
            paiementService.annulerPaiement(paiement.getId(), annulation);
            paiement = paiementService.getById(paiement.getId()).orElseThrow();
        }
        return paiement;
    }

    @Test
    void buildDashboardStats_sansClubId_agregeTousLesPaiements() {
        paiementUnique(100.0, "payé");
        paiementUnique(50.0, "en attente");
        paiementUnique(30.0, "annulé");

        DashboardStatsDTO stats = paiementStatsService.buildDashboardStats(null);

        assertEquals(100.0, stats.totalPayes());
        assertEquals(50.0, stats.totalAttente());
    }

    @Test
    void buildDashboardStats_avecClubId_filtreParClub() {
        paiementUnique(100.0, "payé");

        Club autreClub = new Club();
        autreClub.setName("Autre Club Stats");
        autreClub = clubRepository.save(autreClub);

        Utilisateur autreParent = new Utilisateur();
        autreParent.setNom("Autre");
        autreParent.setPrenom("Parent");
        autreParent.setEmail("autre-parent-stats@test.com");
        autreParent.setPassword("secret");
        autreParent.setRole(Role.PARENT);
        autreParent.setClub(autreClub);
        autreParent = utilisateurService.save(autreParent);

        Membre autreEnfant = new Membre();
        autreEnfant.setNom("Enfant2");
        autreEnfant.setPrenom("Demo2");
        autreEnfant.setParent(autreParent);
        autreEnfant.setClub(autreClub);
        autreEnfant = membreService.save(autreEnfant);

        PaiementDTO dto = new PaiementDTO();
        dto.setUtilisateurId(autreParent.getId());
        dto.setMembreId(autreEnfant.getId());
        dto.setType("UNIQUE");
        dto.setModePaiement("ESPECES");
        dto.setMontantTotal(999.0);
        dto.setDatePaiement(LocalDate.now().toString());
        Paiement autrePaiement = paiementService.ajouterPaiementManuel(dto);
        paiementService.validerPaiementAdmin(autrePaiement.getId());

        DashboardStatsDTO statsClub = paiementStatsService.buildDashboardStats(club.getId());
        DashboardStatsDTO statsAutreClub = paiementStatsService.buildDashboardStats(autreClub.getId());

        assertEquals(100.0, statsClub.totalPayes());
        assertEquals(999.0, statsAutreClub.totalPayes());
    }

    @Test
    void buildDashboardStats_paiementEchelonne_ventileParStatutDEcheance() {
        PaiementDTO dto = new PaiementDTO();
        dto.setUtilisateurId(parent.getId());
        dto.setMembreId(enfant.getId());
        dto.setType("ECHELONNE");
        dto.setModePaiement("VIREMENT");
        dto.setDatePaiement(LocalDate.now().toString());

        EcheanceDTO e1 = new EcheanceDTO();
        e1.setNumero(1);
        e1.setDateEcheance(LocalDate.now().plusDays(10));
        e1.setMontant(50.0);
        EcheanceDTO e2 = new EcheanceDTO();
        e2.setNumero(2);
        e2.setDateEcheance(LocalDate.now().plusDays(40));
        e2.setMontant(50.0);
        dto.setEcheances(List.of(e1, e2));

        Paiement paiement = paiementService.ajouterPaiementManuel(dto);
        // Paye la premiere echeance -> le paiement reste "en attente" globalement
        echeanceService.payerEcheance(paiement.getEcheances().get(0).getId(), "especes", null, null);

        DashboardStatsDTO stats = paiementStatsService.buildDashboardStats(null);

        assertEquals(50.0, stats.totalPayes());
        assertEquals(50.0, stats.totalAttente());
    }

    @Test
    void buildDashboardStats_membresEnRetard_filtresParClubQuandClubIdFourni() {
        // echeance en retard pour le parent du club
        PaiementDTO dto = new PaiementDTO();
        dto.setUtilisateurId(parent.getId());
        dto.setMembreId(enfant.getId());
        dto.setType("ECHELONNE");
        dto.setModePaiement("VIREMENT");
        dto.setDatePaiement(LocalDate.now().toString());
        EcheanceDTO enRetard = new EcheanceDTO();
        enRetard.setNumero(1);
        enRetard.setDateEcheance(LocalDate.now().minusDays(5));
        enRetard.setMontant(50.0);
        dto.setEcheances(List.of(enRetard));
        paiementService.ajouterPaiementManuel(dto);

        DashboardStatsDTO statsClub = paiementStatsService.buildDashboardStats(club.getId());
        DashboardStatsDTO statsAutreClub = paiementStatsService.buildDashboardStats(999999L);

        assertEquals(1, statsClub.membresEnRetard().size());
        assertTrue(statsAutreClub.membresEnRetard().isEmpty());
    }

    @Test
    void buildDashboardStats_aucunPaiement_retourneDesZeros() {
        DashboardStatsDTO stats = paiementStatsService.buildDashboardStats(club.getId());

        assertEquals(0.0, stats.totalPayes());
        assertEquals(0.0, stats.totalAttente());
        assertEquals(0.0, stats.totalAnnules());
        assertEquals(0.0, stats.pourcentagePayesMois());
        assertTrue(stats.membresEnRetard().isEmpty());
    }
}
