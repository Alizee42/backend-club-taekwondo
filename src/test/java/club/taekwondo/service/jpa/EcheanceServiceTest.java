package club.taekwondo.service.jpa;

import club.taekwondo.dto.EcheanceDTO;
import club.taekwondo.dto.MembreRetardDTO;
import club.taekwondo.dto.PaiementDTO;
import club.taekwondo.entity.jpa.Echeance;
import club.taekwondo.entity.jpa.Membre;
import club.taekwondo.entity.jpa.Paiement;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EcheanceServiceTest extends AbstractServiceIntegrationTest {

    @Autowired
    private EcheanceService echeanceService;

    @Autowired
    private PaiementService paiementService;

    @Autowired
    private UtilisateurService utilisateurService;

    @Autowired
    private MembreService membreService;

    private Utilisateur parent;
    private Membre enfant;

    @BeforeEach
    void setupEcheances() {
        parent = new Utilisateur();
        parent.setNom("Testeur");
        parent.setPrenom("Parent");
        parent.setEmail("parent-echeance@test.com");
        parent.setPassword("secret");
        parent.setRole(Role.PARENT);
        parent = utilisateurService.save(parent);

        enfant = new Membre();
        enfant.setNom("Enfant");
        enfant.setPrenom("Demo");
        enfant.setParent(parent);
        enfant = membreService.save(enfant);
    }

    private Paiement creerPaiementEchelonne(double montant1, double montant2) {
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
        e1.setDateEcheance(LocalDate.now().minusDays(10));
        e1.setMontant(montant1);

        EcheanceDTO e2 = new EcheanceDTO();
        e2.setNumero(2);
        e2.setDateEcheance(LocalDate.now().plusDays(30));
        e2.setMontant(montant2);

        dto.setEcheances(List.of(e1, e2));

        return paiementService.ajouterPaiementManuel(dto);
    }

    @Test
    void payerEcheance_marqueLEcheancePayeeEtMetAJourLePaiementParent() {
        Paiement paiement = creerPaiementEchelonne(50.0, 50.0);
        Echeance premiere = paiement.getEcheances().stream()
                .filter(e -> e.getNumero() == 1).findFirst().orElseThrow();

        EcheanceDTO payee = echeanceService.payerEcheance(premiere.getId(), "especes", "REF-1", LocalDate.now());

        assertEquals("payé", payee.getStatut());
        // Le setter de l'entite Echeance normalise en majuscules ASCII (ecrase la normalisation du service)
        assertEquals("ESPECES", payee.getModePaiement());
        assertEquals("REF-1", payee.getReference());

        Paiement reloaded = paiementService.getById(paiement.getId()).orElseThrow();
        assertEquals(50.0, reloaded.getMontantRestant());
        assertEquals(1, reloaded.getEcheancesRestantes());
        assertEquals("en attente", reloaded.getStatut());
    }

    @Test
    void payerEcheance_derniereEcheance_paiementParentPasseAPaye() {
        Paiement paiement = creerPaiementEchelonne(50.0, 50.0);
        List<Echeance> echeances = paiement.getEcheances();

        echeanceService.payerEcheance(echeances.get(0).getId(), "especes", null, null);
        echeanceService.payerEcheance(echeances.get(1).getId(), "cb", null, null);

        Paiement reloaded = paiementService.getById(paiement.getId()).orElseThrow();
        assertEquals("payé", reloaded.getStatut());
        assertEquals(0.0, reloaded.getMontantRestant());
        assertEquals(0, reloaded.getEcheancesRestantes());
    }

    @Test
    void payerEcheance_dejaPayee_leveIllegalStateException() {
        Paiement paiement = creerPaiementEchelonne(50.0, 50.0);
        Echeance premiere = paiement.getEcheances().get(0);

        echeanceService.payerEcheance(premiere.getId(), "especes", null, null);

        assertThrows(IllegalStateException.class,
                () -> echeanceService.payerEcheance(premiere.getId(), "especes", null, null));
    }

    @Test
    void payerEcheance_introuvable_leveRuntimeException() {
        assertThrows(RuntimeException.class,
                () -> echeanceService.payerEcheance(999999L, "especes", null, null));
    }

    @Test
    void payerEcheance_sansDatePaiementReel_utiliseAujourdhui() {
        Paiement paiement = creerPaiementEchelonne(50.0, 50.0);
        Echeance premiere = paiement.getEcheances().get(0);

        EcheanceDTO payee = echeanceService.payerEcheance(premiere.getId(), "virement", "REF", null);

        assertEquals(LocalDate.now(), payee.getDatePaiementReel());
    }

    @Test
    void updateEcheance_modifieLesChampsFournis() {
        Paiement paiement = creerPaiementEchelonne(50.0, 50.0);
        Echeance premiere = paiement.getEcheances().get(0);

        EcheanceDTO update = new EcheanceDTO();
        update.setDateEcheance(LocalDate.now().plusDays(5));
        update.setMontant(60.0);
        update.setStatut("en attente");
        update.setNumero(1);
        update.setModePaiement("carte bancaire");

        EcheanceDTO updated = echeanceService.updateEcheance(premiere.getId(), update);

        assertEquals(60.0, updated.getMontant());
        assertEquals("CB", updated.getModePaiement());
    }

    @Test
    void updateEcheance_introuvable_leveRuntimeException() {
        EcheanceDTO update = new EcheanceDTO();
        update.setDateEcheance(LocalDate.now());
        update.setStatut("en attente");
        update.setNumero(1);

        assertThrows(RuntimeException.class, () -> echeanceService.updateEcheance(999999L, update));
    }

    @Test
    void getMembresEnRetard_neRemonteQueLesEcheancesEnAttentePassees() {
        creerPaiementEchelonne(50.0, 50.0);

        List<MembreRetardDTO> retards = echeanceService.getMembresEnRetard();

        assertEquals(1, retards.size());
        assertEquals(parent.getNom(), retards.get(0).getNom());
        // Seule l'echeance 1 est en retard (dateEcheance = -10j), l'echeance 2 (+30j) n'y est pas
        assertEquals(50.0, retards.get(0).getEcheanceMontant());
    }

    @Test
    void getMembresEnRetard_neComptePasLesEcheancesDejaPayees() {
        Paiement paiement = creerPaiementEchelonne(50.0, 50.0);
        Echeance premiere = paiement.getEcheances().get(0);
        echeanceService.payerEcheance(premiere.getId(), "especes", null, null);

        List<MembreRetardDTO> retards = echeanceService.getMembresEnRetard();

        assertTrue(retards.isEmpty());
    }

    @Test
    void delete_echeanceExistante_laSupprime() {
        Paiement paiement = creerPaiementEchelonne(50.0, 50.0);
        Long echeanceId = paiement.getEcheances().get(0).getId();

        echeanceService.delete(echeanceId);

        assertTrue(echeanceService.getEcheanceEntityById(echeanceId).isEmpty());
    }

    @Test
    void delete_echeanceIntrouvable_leveRuntimeException() {
        assertThrows(RuntimeException.class, () -> echeanceService.delete(999999L));
    }

    @Test
    void getEcheancesByClubId_filtreParClubDuMembre() {
        creerPaiementEchelonne(50.0, 50.0);

        List<EcheanceDTO> echeances = echeanceService.getEcheancesByClubId(999999L);

        assertTrue(echeances.isEmpty());
    }

    @Test
    void getEcheancesByClubId_avecLeBonClub_retourneLesEcheances() {
        club.taekwondo.entity.jpa.Club club = new club.taekwondo.entity.jpa.Club();
        club.setName("Club Echeance");
        club = clubRepository.save(club);

        club.taekwondo.entity.jpa.Membre membreDuClub = membreService.findById(enfant.getId()).orElseThrow();
        membreDuClub.setClub(club);
        membreService.save(membreDuClub);

        creerPaiementEchelonne(50.0, 50.0);

        List<EcheanceDTO> echeances = echeanceService.getEcheancesByClubId(club.getId());

        assertEquals(2, echeances.size());
        assertEquals("Enfant", echeances.get(0).getEnfantNom());
    }

    @Test
    void getAllEcheanceDTOs_retourneToutesLesEcheances() {
        creerPaiementEchelonne(50.0, 50.0);

        List<EcheanceDTO> all = echeanceService.getAllEcheanceDTOs();

        assertEquals(2, all.size());
    }

    @Test
    void createEcheance_associeAuPaiementExistant() {
        Paiement paiement = creerPaiementEchelonne(50.0, 50.0);

        EcheanceDTO dto = new EcheanceDTO();
        dto.setNumero(3);
        dto.setDateEcheance(LocalDate.now().plusDays(60));
        dto.setMontant(25.0);
        dto.setStatut("en attente");

        EcheanceDTO created = echeanceService.createEcheance(dto, paiement.getId());

        assertNotNull(created.getId());
        assertEquals(25.0, created.getMontant());
        assertEquals(3, created.getNumero());
    }

    @Test
    void createEcheance_paiementIntrouvable_leveRuntimeException() {
        EcheanceDTO dto = new EcheanceDTO();
        dto.setNumero(1);
        dto.setDateEcheance(LocalDate.now());
        dto.setStatut("en attente");

        assertThrows(RuntimeException.class, () -> echeanceService.createEcheance(dto, 999999L));
    }

    @Test
    void payerEcheance_sansCompteurEcheancesRestantes_derivelestatutDepuisLesEcheances() {
        Paiement paiement = creerPaiementEchelonne(50.0, 50.0);
        paiement.setEcheancesRestantes(null);
        paiementService.save(paiement);

        List<Echeance> echeances = paiement.getEcheances();
        echeanceService.payerEcheance(echeances.get(0).getId(), "especes", null, null);
        echeanceService.payerEcheance(echeances.get(1).getId(), "cb", null, null);

        Paiement reloaded = paiementService.getById(paiement.getId()).orElseThrow();
        assertEquals("payé", reloaded.getStatut());
    }

    @Test
    void save_persisteDirectementLEntite() {
        Paiement paiement = creerPaiementEchelonne(50.0, 50.0);
        Echeance echeance = paiement.getEcheances().get(0);
        echeance.setMontant(99.0);

        Echeance saved = echeanceService.save(echeance);

        assertEquals(99.0, saved.getMontant());
    }

    @Test
    void updateEcheance_normalizeModeVirementEtEspeces() {
        Paiement paiement = creerPaiementEchelonne(50.0, 50.0);
        Echeance premiere = paiement.getEcheances().get(0);

        EcheanceDTO update = new EcheanceDTO();
        update.setDateEcheance(LocalDate.now());
        update.setStatut("en attente");
        update.setNumero(1);
        update.setModePaiement("virement");

        EcheanceDTO updated = echeanceService.updateEcheance(premiere.getId(), update);

        assertEquals("VIREMENT", updated.getModePaiement());
    }
}
