package club.taekwondo.service.jpa;

import club.taekwondo.dto.GalerieDTO;
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
import club.taekwondo.service.common.FileUploadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Test du vrai GalerieService : ne peut pas etendre AbstractServiceIntegrationTest
 * car cette derniere le mocke pour toutes les autres classes de test.
 */
@SpringBootTest
@ActiveProfiles("test")
class GalerieServiceTest {

    @MockBean
    private ActualiteService actualiteService;

    @MockBean
    private FileUploadService fileUploadService;

    @Autowired
    private GalerieService galerieService;

    @Autowired
    private GalerieRepository galerieRepository;

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
    private ActualiteRepository actualiteRepository;
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
    private Club autreClub;

    @BeforeEach
    void setup() {
        galerieRepository.deleteAll();
        inscriptionRepository.deleteAll();
        evenementRepository.deleteAll();
        ligneCommandeRepository.deleteAll();
        enseignantRepository.deleteAll();
        avisRepository.deleteAll();
        aboutConfigRepository.deleteAll();
        actualiteRepository.deleteAll();
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
        club.setName("Club Galerie Test");
        club = clubRepository.save(club);

        autreClub = new Club();
        autreClub.setName("Autre Club Galerie");
        autreClub = clubRepository.save(autreClub);
    }

    @Test
    void createMultipart_superAdmin_peutPublierPourNimporteQuelClub() {
        GalerieDTO created = galerieService.createMultipart(
                "Stage", "Description", club.getId(), null, "SUPER_ADMIN", 999L);

        assertNotNull(created.getId());
        assertEquals(club.getId(), created.getClubId());
    }

    @Test
    void createMultipart_adminSurSonPropreClub_estAutorise() {
        GalerieDTO created = galerieService.createMultipart(
                "Stage", "Description", club.getId(), null, "ADMIN", club.getId());

        assertNotNull(created.getId());
    }

    @Test
    void createMultipart_adminSurClubEtranger_leveSecurityException() {
        assertThrows(SecurityException.class, () -> galerieService.createMultipart(
                "Stage", "Description", club.getId(), null, "ADMIN", autreClub.getId()));
    }

    @Test
    void createMultipart_adminSansClubIdConnu_leveSecurityException() {
        assertThrows(SecurityException.class, () -> galerieService.createMultipart(
                "Stage", "Description", club.getId(), null, "ADMIN", null));
    }

    @Test
    void createMultipart_avecImage_persisteLeCheminRetourneParLeFileUploadService() throws Exception {
        MockMultipartFile file = new MockMultipartFile("image", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3});
        when(fileUploadService.uploadFile(any(), anyString())).thenReturn("galerie/photo.jpg");

        GalerieDTO created = galerieService.createMultipart(
                "Stage", "Description", club.getId(), file, "SUPER_ADMIN", null);

        assertEquals("galerie/photo.jpg", created.getImageUrl());
    }

    @Test
    void createMultipart_sansImage_imageUrlResteNull() {
        GalerieDTO created = galerieService.createMultipart(
                "Stage", "Description", club.getId(), null, "SUPER_ADMIN", null);

        assertNull(created.getImageUrl());
    }

    @Test
    void update_adminSurClubEtranger_leveSecurityException() {
        GalerieDTO created = galerieService.createMultipart(
                "Stage", "Description", club.getId(), null, "SUPER_ADMIN", null);

        GalerieDTO update = new GalerieDTO();
        update.setTitre("Nouveau titre");

        assertThrows(SecurityException.class,
                () -> galerieService.update(created.getId(), update, "ADMIN", autreClub.getId()));
    }

    @Test
    void update_adminSurSonPropreClub_estAutorise() {
        GalerieDTO created = galerieService.createMultipart(
                "Stage", "Description", club.getId(), null, "SUPER_ADMIN", null);

        GalerieDTO update = new GalerieDTO();
        update.setTitre("Nouveau titre");
        GalerieDTO updated = galerieService.update(created.getId(), update, "ADMIN", club.getId());

        assertEquals("Nouveau titre", updated.getTitre());
    }

    @Test
    void update_introuvable_retourneNull() {
        GalerieDTO update = new GalerieDTO();
        update.setTitre("X");

        assertNull(galerieService.update("999999", update, "SUPER_ADMIN", null));
    }

    @Test
    void update_idInvalide_leveIllegalArgumentException() {
        GalerieDTO update = new GalerieDTO();
        assertThrows(IllegalArgumentException.class,
                () -> galerieService.update("pas-un-nombre", update, "SUPER_ADMIN", null));
    }

    @Test
    void getById_idInvalide_leveIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> galerieService.getById("abc"));
    }

    @Test
    void getById_introuvable_retourneOptionalVide() {
        assertTrue(galerieService.getById("999999").isEmpty());
    }

    @Test
    void delete_supprimeLaGalerie() {
        GalerieDTO created = galerieService.createMultipart(
                "Stage", "Description", club.getId(), null, "SUPER_ADMIN", null);

        galerieService.delete(created.getId());

        assertTrue(galerieService.getById(created.getId()).isEmpty());
    }

    @Test
    void getByClubId_neRetourneQueLesEntreesDuClub() {
        galerieService.createMultipart("Stage club", "Desc", club.getId(), null, "SUPER_ADMIN", null);
        galerieService.createMultipart("Stage autre club", "Desc", autreClub.getId(), null, "SUPER_ADMIN", null);

        assertEquals(1, galerieService.getByClubId(club.getId()).size());
        assertEquals(1, galerieService.getByClubId(autreClub.getId()).size());
    }

    @Test
    void getAll_trieesParDatePublicationDesc() {
        GalerieDTO ancienne = galerieService.createMultipart("Ancienne", "Desc", club.getId(), null, "SUPER_ADMIN", null);
        GalerieDTO recente = galerieService.createMultipart("Recente", "Desc", club.getId(), null, "SUPER_ADMIN", null);

        // Force un ecart explicite pour eviter la flakiness si les deux creations
        // tombent sur le meme instant LocalDateTime.now().
        galerieRepository.findById(Long.parseLong(ancienne.getId())).ifPresent(g -> {
            g.setDatePublication(LocalDateTime.now().minusDays(1));
            galerieRepository.save(g);
        });

        List<GalerieDTO> all = galerieService.getAll();
        assertEquals(2, all.size());
        assertEquals(recente.getId(), all.get(0).getId());
    }
}
