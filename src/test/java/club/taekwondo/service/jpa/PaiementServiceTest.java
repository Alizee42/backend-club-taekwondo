package club.taekwondo.service.jpa;

import club.taekwondo.dto.AnnulationRequestDTO;
import club.taekwondo.dto.EcheanceDTO;
import club.taekwondo.dto.PaiementDTO;
import club.taekwondo.dto.PaiementRequestDTO;
import club.taekwondo.entity.jpa.Club;
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

class PaiementServiceTest extends AbstractServiceIntegrationTest {

    @Autowired
    private PaiementService paiementService;

    @Autowired
    private UtilisateurService utilisateurService;

    @Autowired
    private MembreService membreService;

    private Utilisateur parent;
    private Membre enfant;

    @BeforeEach
    void setupPaiements() {
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
    void testGetByIdChargeLesEcheances() {
        PaiementDTO dto = new PaiementDTO();
        dto.setUtilisateurId(parent.getId());
        dto.setUtilisateurNom(parent.getNom());
        dto.setUtilisateurPrenom(parent.getPrenom());
        dto.setUtilisateurEmail(parent.getEmail());
        dto.setMembreId(enfant.getId());
        dto.setType("ECHELONNE");
        dto.setModePaiement("CB");
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

        Paiement created = paiementService.ajouterPaiementManuel(dto);
        Paiement loaded = paiementService.getById(created.getId()).orElseThrow();

        assertEquals(2, loaded.getEcheances().size());
        assertEquals(1, loaded.getEcheances().get(0).getNumero());
        assertEquals(2, loaded.getEcheances().get(1).getNumero());
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

    @Test
    void testAnnulerPaiement_dejaPaye_leveIllegalStateException() {
        PaiementDTO dto = new PaiementDTO();
        dto.setUtilisateurId(parent.getId());
        dto.setMembreId(enfant.getId());
        dto.setType("UNIQUE");
        dto.setModePaiement("ESPECES");
        dto.setMontantTotal(80.0);
        dto.setDatePaiement(LocalDate.now().toString());

        Paiement paiement = paiementService.ajouterPaiementManuel(dto);
        paiementService.validerPaiementAdmin(paiement.getId());

        AnnulationRequestDTO annulation = new AnnulationRequestDTO();
        annulation.setMotif("Trop tard");

        assertThrows(IllegalStateException.class,
                () -> paiementService.annulerPaiement(paiement.getId(), annulation));
    }

    @Test
    void testAnnulerPaiement_dejaAnnule_leveIllegalStateException() {
        PaiementDTO dto = new PaiementDTO();
        dto.setUtilisateurId(parent.getId());
        dto.setMembreId(enfant.getId());
        dto.setType("UNIQUE");
        dto.setModePaiement("ESPECES");
        dto.setMontantTotal(80.0);
        dto.setDatePaiement(LocalDate.now().toString());

        Paiement paiement = paiementService.ajouterPaiementManuel(dto);

        AnnulationRequestDTO annulation = new AnnulationRequestDTO();
        annulation.setMotif("Premiere annulation");
        paiementService.annulerPaiement(paiement.getId(), annulation);

        assertThrows(IllegalStateException.class,
                () -> paiementService.annulerPaiement(paiement.getId(), annulation));
    }

    @Test
    void testAnnulerPaiement_sansEcheances_montantRestantBascule() {
        PaiementDTO dto = new PaiementDTO();
        dto.setUtilisateurId(parent.getId());
        dto.setMembreId(enfant.getId());
        dto.setType("UNIQUE");
        dto.setModePaiement("ESPECES");
        dto.setMontantTotal(150.0);
        dto.setDatePaiement(LocalDate.now().toString());

        Paiement paiement = paiementService.ajouterPaiementManuel(dto);

        AnnulationRequestDTO annulation = new AnnulationRequestDTO();
        annulation.setMotif("Client absent");
        annulation.setAdminResponsable("admin1");

        PaiementDTO annule = paiementService.annulerPaiement(paiement.getId(), annulation);

        assertEquals("annulé", annule.getStatut());
        Paiement reloaded = paiementService.getById(paiement.getId()).orElseThrow();
        assertEquals(0.0, reloaded.getMontantRestant());
        assertEquals(0.0, reloaded.getMontantPaye());
        assertEquals(0, reloaded.getEcheancesRestantes());
    }

    @Test
    void testAnnulerPaiement_avecEcheancesNonPayees_annuleUniquementLesNonPayees() {
        PaiementDTO dto = new PaiementDTO();
        dto.setUtilisateurId(parent.getId());
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

        AnnulationRequestDTO annulation = new AnnulationRequestDTO();
        annulation.setMotif("Arret cours");

        PaiementDTO annule = paiementService.annulerPaiement(paiement.getId(), annulation);

        assertEquals("annulé", annule.getStatut());
        Paiement reloaded = paiementService.getById(paiement.getId()).orElseThrow();
        assertEquals(0.0, reloaded.getMontantRestant());
        assertEquals(0, reloaded.getEcheancesRestantes());
        assertTrue(reloaded.getEcheances().stream().allMatch(e -> "annulé".equalsIgnoreCase(e.getStatut())));
    }

    @Test
    void testAnnulerPaiement_paiementIntrouvable_leveRuntimeException() {
        AnnulationRequestDTO annulation = new AnnulationRequestDTO();
        annulation.setMotif("N'existe pas");

        assertThrows(RuntimeException.class,
                () -> paiementService.annulerPaiement(999999L, annulation));
    }

    @Test
    void testAjouterPaiementParent_membreNAppartientPasAuParent_leveRuntimeException() {
        Utilisateur autreParent = new Utilisateur();
        autreParent.setNom("Autre");
        autreParent.setPrenom("Parent");
        autreParent.setEmail("autre-parent@test.com");
        autreParent.setPassword("secret");
        autreParent.setRole(Role.PARENT);
        autreParent = utilisateurService.save(autreParent);

        PaiementRequestDTO req = new PaiementRequestDTO();
        req.setMembreId(enfant.getId());
        req.setMontantTotal(100.0);
        req.setTypePaiement("UNIQUE");
        req.setModePaiement("ESPECES");
        req.setDatePaiement(LocalDate.now().toString());

        Long autreParentId = autreParent.getId();
        assertThrows(RuntimeException.class,
                () -> paiementService.ajouterPaiementParent(req, autreParentId));
    }

    @Test
    void testAjouterPaiementParent_clubEnfantDifferentDuClubParent_leveIllegalStateException() {
        Club clubParent = new Club();
        clubParent.setName("Club Parent Mismatch");
        clubParent = clubRepository.save(clubParent);

        Club clubEnfant = new Club();
        clubEnfant.setName("Club Enfant Mismatch");
        clubEnfant = clubRepository.save(clubEnfant);

        Utilisateur managedParent = utilisateurRepository.findById(parent.getId()).orElseThrow();
        managedParent.setClub(clubParent);
        parent = utilisateurRepository.save(managedParent);

        Membre managedEnfant = membreRepository.findById(enfant.getId()).orElseThrow();
        managedEnfant.setClub(clubEnfant);
        enfant = membreRepository.save(managedEnfant);

        PaiementRequestDTO req = new PaiementRequestDTO();
        req.setMembreId(enfant.getId());
        req.setMontantTotal(100.0);
        req.setTypePaiement("UNIQUE");
        req.setModePaiement("ESPECES");
        req.setDatePaiement(LocalDate.now().toString());

        Long parentId = parent.getId();
        assertThrows(IllegalStateException.class,
                () -> paiementService.ajouterPaiementParent(req, parentId));
    }

    @Test
    void testAjouterPaiementParent_membreSansClub_heriteDuClubParent() {
        Club clubParent = new Club();
        clubParent.setName("Club Parent Heritage");
        clubParent = clubRepository.save(clubParent);

        Utilisateur managedParent = utilisateurRepository.findById(parent.getId()).orElseThrow();
        managedParent.setClub(clubParent);
        parent = utilisateurRepository.save(managedParent);
        // enfant.club reste null volontairement

        PaiementRequestDTO req = new PaiementRequestDTO();
        req.setMembreId(enfant.getId());
        req.setMontantTotal(90.0);
        req.setTypePaiement("UNIQUE");
        req.setModePaiement("CB");
        req.setDatePaiement(LocalDate.now().toString());

        Paiement paiement = paiementService.ajouterPaiementParent(req, parent.getId());

        assertNotNull(paiement.getId());
        assertEquals(clubParent.getId(), paiement.getClub().getId());
        assertEquals("en attente", paiement.getStatut());
        assertEquals(90.0, paiement.getMontantTotal());
    }

    @Test
    void testAjouterPaiementParent_montantInvalide_leveIllegalArgumentException() {
        Club club = new Club();
        club.setName("Club Montant Invalide");
        club = clubRepository.save(club);
        Utilisateur managedParent = utilisateurRepository.findById(parent.getId()).orElseThrow();
        managedParent.setClub(club);
        parent = utilisateurRepository.save(managedParent);

        PaiementRequestDTO req = new PaiementRequestDTO();
        req.setMembreId(enfant.getId());
        req.setMontantTotal(0.0);
        req.setTypePaiement("UNIQUE");
        req.setModePaiement("ESPECES");
        req.setDatePaiement(LocalDate.now().toString());

        Long parentId = parent.getId();
        assertThrows(IllegalArgumentException.class,
                () -> paiementService.ajouterPaiementParent(req, parentId));
    }

    @Test
    void testAjouterPaiementParent_echelonneAutoSplit_repartitLesEcheances() {
        Club club = new Club();
        club.setName("Club Echelonne AutoSplit");
        club = clubRepository.save(club);
        Utilisateur managedParent = utilisateurRepository.findById(parent.getId()).orElseThrow();
        managedParent.setClub(club);
        parent = utilisateurRepository.save(managedParent);

        PaiementRequestDTO req = new PaiementRequestDTO();
        req.setMembreId(enfant.getId());
        req.setMontantTotal(100.0);
        req.setTypePaiement("ECHELONNE");
        req.setModePaiement("VIREMENT");
        req.setNombreEcheances(3);
        req.setDatePaiement(LocalDate.now().toString());

        Paiement paiement = paiementService.ajouterPaiementParent(req, parent.getId());

        assertEquals(3, paiement.getEcheancesTotales());
        assertEquals(3, paiement.getEcheancesRestantes());
        assertEquals(100.0, paiement.getMontantRestant());
        double sommeEcheances = paiement.getEcheances().stream()
                .mapToDouble(e -> e.getMontant()).sum();
        assertEquals(100.0, sommeEcheances, 0.001);
    }

    @Test
    void testAjouterPaiementManuel_montantNegatifOuNul_leveIllegalArgumentException() {
        Paiement detache = new Paiement();
        detache.setMontantTotal(-5.0);
        detache.setMembre(enfant);

        assertThrows(IllegalArgumentException.class, () -> paiementService.save(detache));
    }

    @Test
    void testAjouterPaiementManuel_membreIntrouvable_leveRuntimeException() {
        PaiementDTO dto = new PaiementDTO();
        dto.setUtilisateurId(parent.getId());
        dto.setUtilisateurNom(parent.getNom());
        dto.setUtilisateurPrenom(parent.getPrenom());
        dto.setUtilisateurEmail(parent.getEmail());
        dto.setMembreId(999999L);
        dto.setType("UNIQUE");
        dto.setModePaiement("ESPECES");
        dto.setMontantTotal(100.0);
        dto.setDatePaiement(LocalDate.now().toString());

        assertThrows(RuntimeException.class, () -> paiementService.ajouterPaiementManuel(dto));
    }

    @Test
    void testNormalizeTypeHuman_reconnaitEchelonneAvecAccentsEtCasse() {
        assertEquals("ECHELONNE", PaiementService.normalizeTypeHuman("Échelonné"));
        assertEquals("ECHELONNE", PaiementService.normalizeTypeHuman("echelonne"));
        assertEquals("UNIQUE", PaiementService.normalizeTypeHuman("Unique"));
        assertEquals("UNIQUE", PaiementService.normalizeTypeHuman(null));
    }

    @Test
    void testNormalizeModeHuman_reconnaitLesVariantesConnues() {
        assertEquals("CB", PaiementService.normalizeModeHuman("stripe"));
        assertEquals("CB", PaiementService.normalizeModeHuman("Carte Bancaire"));
        assertEquals("VIREMENT", PaiementService.normalizeModeHuman("Virement"));
        assertEquals("CHEQUE", PaiementService.normalizeModeHuman("Chèque"));
        assertEquals("ESPECES", PaiementService.normalizeModeHuman("especes"));
        assertEquals("ESPECES", PaiementService.normalizeModeHuman(null));
        assertEquals("ESPECES", PaiementService.normalizeModeHuman("inconnu"));
    }
}
