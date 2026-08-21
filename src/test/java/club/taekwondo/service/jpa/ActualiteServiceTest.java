package club.taekwondo.service.jpa;

import club.taekwondo.dto.ActualiteDTO;
import club.taekwondo.entity.jpa.Club;
import club.taekwondo.repository.jpa.AboutConfigRepository;
import club.taekwondo.repository.jpa.ActualiteRepository;
import club.taekwondo.repository.jpa.AvisRepository;
import club.taekwondo.repository.jpa.ClubRepository;
import club.taekwondo.repository.jpa.CommandeRepository;
import club.taekwondo.repository.jpa.EcheanceRepository;
import club.taekwondo.repository.jpa.EnseignantRepository;
import club.taekwondo.repository.jpa.EvenementRepository;
import club.taekwondo.repository.jpa.GalerieRepository;
import club.taekwondo.repository.jpa.HoraireRepository;
import club.taekwondo.repository.jpa.InscriptionEvenementRepository;
import club.taekwondo.repository.jpa.LigneCommandeRepository;
import club.taekwondo.repository.jpa.MembreRepository;
import club.taekwondo.repository.jpa.MentionsLegalesConfigRepository;
import club.taekwondo.repository.jpa.NotificationRepository;
import club.taekwondo.repository.jpa.ParametresPaiementRepository;
import club.taekwondo.repository.jpa.PaiementRepository;
import club.taekwondo.repository.jpa.PolitiqueConfidentialiteConfigRepository;
import club.taekwondo.repository.jpa.ProduitRepository;
import club.taekwondo.repository.jpa.ReinitialisationMotDePasseRepository;
import club.taekwondo.repository.jpa.RequiredDocumentRepository;
import club.taekwondo.repository.jpa.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test du vrai ActualiteService : ne peut pas etendre AbstractServiceIntegrationTest
 * car cette derniere mocke ActualiteService pour toutes les autres classes de test.
 * Reproduit donc le meme nettoyage FK complet independamment.
 */
@SpringBootTest
@ActiveProfiles("test")
class ActualiteServiceTest {

    @MockBean
    private GalerieService galerieService;

    @Autowired
    private ActualiteService actualiteService;

    @Autowired
    private ActualiteRepository actualiteRepository;

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private InscriptionEvenementRepository inscriptionRepository;
    @Autowired
    private EvenementRepository evenementRepository;
    @Autowired
    private LigneCommandeRepository ligneCommandeRepository;
    @Autowired
    private EnseignantRepository enseignantRepository;
    @Autowired
    private AvisRepository avisRepository;
    @Autowired
    private AboutConfigRepository aboutConfigRepository;
    @Autowired
    private GalerieRepository galerieRepository;
    @Autowired
    private HoraireRepository horaireRepository;
    @Autowired
    private MentionsLegalesConfigRepository mentionsLegalesConfigRepository;
    @Autowired
    private PolitiqueConfidentialiteConfigRepository politiqueConfidentialiteConfigRepository;
    @Autowired
    private ParametresPaiementRepository parametresPaiementRepository;
    @Autowired
    private RequiredDocumentRepository requiredDocumentRepository;
    @Autowired
    private EcheanceRepository echeanceRepository;
    @Autowired
    private PaiementRepository paiementRepository;
    @Autowired
    private CommandeRepository commandeRepository;
    @Autowired
    private ProduitRepository produitRepository;
    @Autowired
    private MembreRepository membreRepository;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private ReinitialisationMotDePasseRepository reinitialisationMotDePasseRepository;

    private Club club;

    @BeforeEach
    void setup() {
        actualiteRepository.deleteAll();
        inscriptionRepository.deleteAll();
        evenementRepository.deleteAll();
        ligneCommandeRepository.deleteAll();
        enseignantRepository.deleteAll();
        avisRepository.deleteAll();
        aboutConfigRepository.deleteAll();
        galerieRepository.deleteAll();
        horaireRepository.deleteAll();
        mentionsLegalesConfigRepository.deleteAll();
        politiqueConfidentialiteConfigRepository.deleteAll();
        parametresPaiementRepository.deleteAll();
        requiredDocumentRepository.deleteAll();
        echeanceRepository.deleteAll();
        paiementRepository.deleteAll();
        commandeRepository.deleteAll();
        produitRepository.deleteAll();
        membreRepository.deleteAll();
        notificationRepository.deleteAll();
        reinitialisationMotDePasseRepository.deleteAll();
        utilisateurRepository.deleteAll();
        clubRepository.deleteAll();

        club = new Club();
        club.setName("Club Actualite Test");
        club = clubRepository.save(club);
    }

    private ActualiteDTO dto(String titre, boolean featured) {
        ActualiteDTO dto = new ActualiteDTO();
        dto.setTitre(titre);
        dto.setContenu("Contenu de " + titre);
        dto.setDatePublication(LocalDateTime.now());
        dto.setClubId(String.valueOf(club.getId()));
        dto.setFeatured(featured);
        return dto;
    }

    @Test
    void create_persisteEtRetourneLeDtoAvecIdString() {
        ActualiteDTO created = actualiteService.create(dto("Nouvelle actu", false));

        assertNotNull(created.getId());
        assertEquals("Nouvelle actu", created.getTitre());
        assertEquals(String.valueOf(club.getId()), created.getClubId());
    }

    @Test
    void create_featured_desactiveLesAutresFeaturedDuMemeClub() {
        actualiteService.create(dto("Actu 1", true));
        ActualiteDTO actu2 = actualiteService.create(dto("Actu 2", true));

        List<ActualiteDTO> featured = actualiteService.getFeatured();
        assertEquals(1, featured.size());
        assertEquals(actu2.getId(), featured.get(0).getId());
    }

    @Test
    void create_featured_neDesactivePasLesFeaturedDunAutreClub() {
        Club autreClub = new Club();
        autreClub.setName("Autre Club Actualite");
        autreClub = clubRepository.save(autreClub);

        ActualiteDTO dtoAutreClub = new ActualiteDTO();
        dtoAutreClub.setTitre("Actu autre club");
        dtoAutreClub.setContenu("Contenu");
        dtoAutreClub.setDatePublication(LocalDateTime.now());
        dtoAutreClub.setClubId(String.valueOf(autreClub.getId()));
        dtoAutreClub.setFeatured(true);
        actualiteService.create(dtoAutreClub);

        actualiteService.create(dto("Actu club principal", true));

        assertEquals(2, actualiteService.getFeatured().size());
    }

    @Test
    void update_featured_desactiveLesAutresSaufSoiMeme() {
        ActualiteDTO actu1 = actualiteService.create(dto("Actu 1", true));
        ActualiteDTO actu2 = actualiteService.create(dto("Actu 2", false));

        ActualiteDTO updateActu2 = dto("Actu 2 modifiee", true);
        actualiteService.update(actu2.getId(), updateActu2);

        List<ActualiteDTO> featured = actualiteService.getFeatured();
        assertEquals(1, featured.size());
        assertEquals(actu2.getId(), featured.get(0).getId());
    }

    @Test
    void update_introuvable_retourneNull() {
        ActualiteDTO result = actualiteService.update("999999", dto("X", false));
        assertNull(result);
    }

    @Test
    void update_idInvalide_leveIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> actualiteService.update("pas-un-nombre", dto("X", false)));
    }

    @Test
    void getById_idInvalide_leveIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> actualiteService.getById("abc"));
    }

    @Test
    void getById_introuvable_retourneOptionalVide() {
        assertTrue(actualiteService.getById("999999").isEmpty());
    }

    @Test
    void setFeatured_activeEtDesactiveLesAutres() {
        ActualiteDTO actu1 = actualiteService.create(dto("Actu 1", true));
        ActualiteDTO actu2 = actualiteService.create(dto("Actu 2", false));

        actualiteService.setFeatured(actu2.getId());

        List<ActualiteDTO> featured = actualiteService.getFeatured();
        assertEquals(1, featured.size());
        assertEquals(actu2.getId(), featured.get(0).getId());
    }

    @Test
    void setFeatured_introuvable_leveRuntimeException() {
        assertThrows(RuntimeException.class, () -> actualiteService.setFeatured("999999"));
    }

    @Test
    void delete_supprimeLActualite() {
        ActualiteDTO created = actualiteService.create(dto("A supprimer", false));

        actualiteService.delete(created.getId());

        assertTrue(actualiteService.getById(created.getId()).isEmpty());
    }

    @Test
    void getByClubId_neRetourneQueLesActualitesDuClub() {
        actualiteService.create(dto("Actu club", false));

        Club autreClub = new Club();
        autreClub.setName("Autre Club Actualite 2");
        autreClub = clubRepository.save(autreClub);

        assertEquals(1, actualiteService.getByClubId(String.valueOf(club.getId())).size());
        assertEquals(0, actualiteService.getByClubId(String.valueOf(autreClub.getId())).size());
    }

    @Test
    void getByClubId_clubIdInvalide_leveIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> actualiteService.getByClubId("abc"));
    }

    @Test
    void countActualites_retourneLeNombreTotal() {
        actualiteService.create(dto("Actu 1", false));
        actualiteService.create(dto("Actu 2", false));

        assertEquals(2, actualiteService.countActualites());
    }

    @Test
    void getAll_triéesParDatePublicationDesc() {
        ActualiteDTO ancienne = dto("Ancienne", false);
        ancienne.setDatePublication(LocalDateTime.now().minusDays(5));
        actualiteService.create(ancienne);

        ActualiteDTO recente = dto("Recente", false);
        recente.setDatePublication(LocalDateTime.now());
        actualiteService.create(recente);

        List<ActualiteDTO> all = actualiteService.getAll();
        assertEquals(2, all.size());
        assertEquals("Recente", all.get(0).getTitre());
    }
}
