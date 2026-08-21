package club.taekwondo.service.jpa;

import club.taekwondo.dto.InscriptionEvenementDTO;
import club.taekwondo.entity.jpa.Club;
import club.taekwondo.entity.jpa.Evenement;
import club.taekwondo.entity.jpa.InscriptionEvenement;
import club.taekwondo.entity.jpa.Membre;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.enums.Role;
import club.taekwondo.enums.StatutInscription;
import club.taekwondo.repository.jpa.ClubRepository;
import club.taekwondo.repository.jpa.CommandeRepository;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class InscriptionEvenementServiceTest {

    @MockBean
    private ActualiteService actualiteService;

    @MockBean
    private GalerieService galerieService;

    @Autowired
    private InscriptionEvenementService inscriptionService;

    @Autowired
    private InscriptionEvenementRepository inscriptionRepository;

    @Autowired
    private EvenementRepository evenementRepository;

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private MembreRepository membreRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private PaiementRepository paiementRepository;

    @Autowired
    private CommandeRepository commandeRepository;

    @Autowired
    private LigneCommandeRepository ligneCommandeRepository;

    @Autowired
    private UtilisateurService utilisateurService;

    @Autowired
    private MembreService membreService;

    private Club club;
    private Utilisateur parent;
    private Membre enfant;
    private Evenement evenement;

    @BeforeEach
    void setup() {
        inscriptionRepository.deleteAll();
        evenementRepository.deleteAll();
        ligneCommandeRepository.deleteAll();
        commandeRepository.deleteAll();
        paiementRepository.deleteAll();
        membreRepository.deleteAll();
        notificationRepository.deleteAll();
        utilisateurRepository.deleteAll();
        clubRepository.deleteAll();

        club = new Club();
        club.setName("Club Inscription Test");
        club = clubRepository.save(club);

        parent = new Utilisateur();
        parent.setNom("Testeur");
        parent.setPrenom("Parent");
        parent.setEmail("parent-inscription@test.com");
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

        evenement = new Evenement();
        evenement.setTitre("Stage");
        evenement.setDateDebut(LocalDateTime.now().plusDays(5));
        evenement.setDateFin(LocalDateTime.now().plusDays(5).plusHours(2));
        evenement.setLieu("Dojo");
        evenement.setCapacite(1);
        evenement.setActif(true);
        evenement.setClub(club);
        evenement = evenementRepository.save(evenement);
    }

    @Test
    void inscrireMembres_succes_creeUneInscriptionEnAttente() {
        List<InscriptionEvenementDTO> resultats = inscriptionService.inscrireMembres(
                evenement.getId(), List.of(enfant.getId()), "Commentaire");

        assertEquals(1, resultats.size());
        assertEquals(StatutInscription.EN_ATTENTE, resultats.get(0).getStatut());
        assertEquals(enfant.getId(), resultats.get(0).getMembreId());
    }

    @Test
    void inscrireMembres_evenementComplet_leveRuntimeException() {
        inscriptionService.inscrireMembres(evenement.getId(), List.of(enfant.getId()), null);

        Membre autreEnfant = new Membre();
        autreEnfant.setNom("Enfant2");
        autreEnfant.setPrenom("Demo2");
        autreEnfant.setParent(parent);
        autreEnfant.setClub(club);
        autreEnfant = membreService.save(autreEnfant);

        Long autreEnfantId = autreEnfant.getId();
        assertThrows(RuntimeException.class,
                () -> inscriptionService.inscrireMembres(evenement.getId(), List.of(autreEnfantId), null));
    }

    @Test
    void inscrireMembres_membreDejaInscrit_leveRuntimeException() {
        inscriptionService.inscrireMembres(evenement.getId(), List.of(enfant.getId()), null);

        // capacite=1 mais on teste le message specifique de double inscription:
        // on augmente la capacite pour isoler ce cas du cas "complet"
        evenement.setCapacite(5);
        evenementRepository.save(evenement);

        assertThrows(RuntimeException.class,
                () -> inscriptionService.inscrireMembres(evenement.getId(), List.of(enfant.getId()), null));
    }

    @Test
    void inscrireMembres_membreReinscritApresAnnulation_bloqueParContrainteUniqueEnBase() {
        // Le code service autorise la reinscription (existsBy...StatutNot(ANNULEE) ne bloque
        // pas ce cas), mais la contrainte UNIQUE(membre_id, evenement_id) en base ne distingue
        // pas les statuts : une deuxieme ligne pour le meme couple membre/evenement est toujours
        // rejetee, meme si la premiere est annulee. Divergence code/schema a noter.
        evenement.setCapacite(5);
        evenementRepository.save(evenement);

        List<InscriptionEvenementDTO> premiere = inscriptionService.inscrireMembres(
                evenement.getId(), List.of(enfant.getId()), null);
        inscriptionService.annulerInscription(premiere.get(0).getId());

        assertThrows(RuntimeException.class,
                () -> inscriptionService.inscrireMembres(evenement.getId(), List.of(enfant.getId()), null));
    }

    @Test
    void inscrireMembres_evenementIntrouvable_leveRuntimeException() {
        assertThrows(RuntimeException.class,
                () -> inscriptionService.inscrireMembres(999999L, List.of(enfant.getId()), null));
    }

    @Test
    void inscrireMembres_membreIntrouvable_leveRuntimeException() {
        assertThrows(RuntimeException.class,
                () -> inscriptionService.inscrireMembres(evenement.getId(), List.of(999999L), null));
    }

    @Test
    void annulerInscription_passeAuStatutAnnulee() {
        InscriptionEvenementDTO created = inscriptionService.inscrireMembres(
                evenement.getId(), List.of(enfant.getId()), null).get(0);

        inscriptionService.annulerInscription(created.getId());

        InscriptionEvenementDTO reloaded = inscriptionService.getInscriptionById(created.getId()).orElseThrow();
        assertEquals(StatutInscription.ANNULEE, reloaded.getStatut());
    }

    @Test
    void annulerInscription_introuvable_leveRuntimeException() {
        assertThrows(RuntimeException.class, () -> inscriptionService.annulerInscription(999999L));
    }

    @Test
    void updateStatutInscription_statutValide_estApplique() {
        InscriptionEvenementDTO created = inscriptionService.inscrireMembres(
                evenement.getId(), List.of(enfant.getId()), null).get(0);

        inscriptionService.updateStatutInscription(created.getId(), "VALIDEE");

        InscriptionEvenementDTO reloaded = inscriptionService.getInscriptionById(created.getId()).orElseThrow();
        assertEquals(StatutInscription.VALIDEE, reloaded.getStatut());
    }

    @Test
    void updateStatutInscription_statutInvalide_leveRuntimeException() {
        InscriptionEvenementDTO created = inscriptionService.inscrireMembres(
                evenement.getId(), List.of(enfant.getId()), null).get(0);
        Long id = created.getId();

        assertThrows(RuntimeException.class, () -> inscriptionService.updateStatutInscription(id, "INCONNU"));
    }

    @Test
    void getInscriptionsByEvenementAndStatut_filtreParStatut() {
        InscriptionEvenementDTO created = inscriptionService.inscrireMembres(
                evenement.getId(), List.of(enfant.getId()), null).get(0);
        inscriptionService.updateStatutInscription(created.getId(), "VALIDEE");

        List<InscriptionEvenementDTO> validees =
                inscriptionService.getInscriptionsByEvenementAndStatut(evenement.getId(), "VALIDEE");
        List<InscriptionEvenementDTO> enAttente =
                inscriptionService.getInscriptionsByEvenementAndStatut(evenement.getId(), "EN_ATTENTE");

        assertEquals(1, validees.size());
        assertTrue(enAttente.isEmpty());
    }

    @Test
    void getInscriptionsByParent_retourneLesInscriptionsDesEnfants() {
        inscriptionService.inscrireMembres(evenement.getId(), List.of(enfant.getId()), null);

        List<InscriptionEvenementDTO> inscriptions = inscriptionService.getInscriptionsByParent(parent.getId());

        assertEquals(1, inscriptions.size());
    }

    @Test
    void getInscriptionsByClubId_neRetourneQueLesInscriptionsDuClub() {
        inscriptionService.inscrireMembres(evenement.getId(), List.of(enfant.getId()), null);

        List<InscriptionEvenementDTO> inscriptions = inscriptionService.getInscriptionsByClubId(club.getId());

        assertEquals(1, inscriptions.size());
    }

    @Test
    void convertToDTO_accesParentHorsTransaction_retombeSurEmailNonDisponible() {
        // InscriptionEvenementService n'a pas de @Transactional : l'acces lazy a membre.getParent()
        // dans convertToDTO se fait hors session Hibernate active, leve une exception silencieusement
        // catchee, et retombe sur ce message plutot que sur l'email reel du parent.
        InscriptionEvenementDTO created = inscriptionService.inscrireMembres(
                evenement.getId(), List.of(enfant.getId()), null).get(0);

        assertEquals("Email non disponible", created.getMembreEmail());
    }
}
