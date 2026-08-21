package club.taekwondo.service.jpa;

import club.taekwondo.dto.EvenementDTO;
import club.taekwondo.entity.jpa.Club;
import club.taekwondo.entity.jpa.Evenement;
import club.taekwondo.entity.jpa.InscriptionEvenement;
import club.taekwondo.entity.jpa.Membre;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.enums.Role;
import club.taekwondo.enums.StatutInscription;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.*;

class EvenementServiceTest extends AbstractServiceIntegrationTest {

    @Autowired
    private EvenementService evenementService;

    @Autowired
    private UtilisateurService utilisateurService;

    @Autowired
    private MembreService membreService;

    private Club club;
    private Utilisateur parent;
    private Membre enfant;

    @BeforeEach
    void setupEvenements() {
        club = new Club();
        club.setName("Club Evenement Test");
        club = clubRepository.save(club);

        parent = new Utilisateur();
        parent.setNom("Testeur");
        parent.setPrenom("Parent");
        parent.setEmail("parent-evenement@test.com");
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

    private EvenementDTO dtoFor(Evenement e) {
        EvenementDTO dto = new EvenementDTO();
        dto.setTitre(e.getTitre());
        dto.setDateDebut(e.getDateDebut());
        dto.setDateFin(e.getDateFin());
        dto.setLieu(e.getLieu());
        dto.setCapacite(e.getCapacite());
        dto.setDescription(e.getDescription());
        return dto;
    }

    private Evenement creerEvenement(boolean actif) {
        Evenement e = new Evenement();
        e.setTitre("Stage d'ete");
        e.setDateDebut(LocalDateTime.now().plusDays(10));
        e.setDateFin(LocalDateTime.now().plusDays(10).plusHours(2));
        e.setLieu("Dojo");
        e.setCapacite(20);
        e.setDescription("Stage de perfectionnement");
        e.setActif(actif);
        e.setClub(club);
        return evenementRepository.save(e);
    }

    @Test
    void getEvenementsActifs_excludLesInactifs() {
        creerEvenement(true);
        creerEvenement(false);

        var actifs = evenementService.getEvenementsActifs();

        assertEquals(1, actifs.size());
    }

    @Test
    void convertToDTO_calculeLeNombreDInscritsActifs() {
        Evenement evenement = creerEvenement(true);

        InscriptionEvenement inscription = new InscriptionEvenement();
        inscription.setEvenement(evenement);
        inscription.setMembre(enfant);
        inscription.setStatut(StatutInscription.VALIDEE);
        inscriptionRepository.save(inscription);

        EvenementDTO dto = evenementService.getEvenementById(evenement.getId()).orElseThrow();

        assertEquals(1, dto.getNbInscrits());
    }

    @Test
    void updateEvenement_champModifie_persisteLaModification() {
        Evenement evenement = creerEvenement(true);
        EvenementDTO dto = dtoFor(evenement);
        dto.setTitre("Titre modifie");

        EvenementDTO updated = evenementService.updateEvenement(evenement.getId(), dto);

        assertEquals("Titre modifie", updated.getTitre());
    }

    @Test
    void updateEvenement_introuvable_leveRuntimeException() {
        EvenementDTO dto = new EvenementDTO();
        dto.setTitre("X");

        assertThrows(RuntimeException.class, () -> evenementService.updateEvenement(999999L, dto));
    }

    @Test
    void changerStatutEvenement_passageInactif_neSupprimeRienEtChangeLeStatut() {
        Evenement evenement = creerEvenement(true);

        EvenementDTO updated = evenementService.changerStatutEvenement(evenement.getId(), false);

        assertEquals(Boolean.FALSE, updated.getActif());
        assertTrue(evenementRepository.existsById(evenement.getId()));
    }

    @Test
    void changerStatutEvenement_introuvable_leveRuntimeException() {
        assertThrows(RuntimeException.class, () -> evenementService.changerStatutEvenement(999999L, false));
    }

    @Test
    void deleteEvenement_supprimeAussiLesInscriptions() {
        Evenement evenement = creerEvenement(true);
        InscriptionEvenement inscription = new InscriptionEvenement();
        inscription.setEvenement(evenement);
        inscription.setMembre(enfant);
        inscription.setStatut(StatutInscription.VALIDEE);
        inscriptionRepository.save(inscription);

        evenementService.deleteEvenement(evenement.getId());

        assertFalse(evenementRepository.existsById(evenement.getId()));
        assertTrue(inscriptionRepository.findByEvenementId(evenement.getId()).isEmpty());
    }

    @Test
    void deleteEvenement_introuvable_leveRuntimeException() {
        assertThrows(RuntimeException.class, () -> evenementService.deleteEvenement(999999L));
    }

    @Test
    void getEvenementsByClubId_neRetourneQueLesEvenementsDuClub() {
        creerEvenement(true);

        Club autreClub = new Club();
        autreClub.setName("Autre Club Evenement");
        autreClub = clubRepository.save(autreClub);

        Evenement autreEvenement = new Evenement();
        autreEvenement.setTitre("Autre stage");
        autreEvenement.setDateDebut(LocalDateTime.now());
        autreEvenement.setDateFin(LocalDateTime.now().plusHours(1));
        autreEvenement.setLieu("Dojo B");
        autreEvenement.setCapacite(5);
        autreEvenement.setClub(autreClub);
        autreEvenement.setActif(true);
        evenementRepository.save(autreEvenement);

        var resultats = evenementService.getEvenementsByClubId(club.getId());

        assertEquals(1, resultats.size());
    }

    @Test
    void ajouterEvenement_clubInexistant_leveRuntimeException() {
        assertThrows(RuntimeException.class, () -> evenementService.ajouterEvenement(
                "Titre", "2026-09-01T10:00:00", "2026-09-01T12:00:00", "Dojo",
                20, "Description", null, 999999L));
    }

    @Test
    void ajouterEvenement_succes_creeEtNotifieTousLesUtilisateurs() {
        EvenementDTO dto = evenementService.ajouterEvenement(
                "Nouveau stage", "2026-09-01T10:00:00", "2026-09-01T12:00:00", "Dojo",
                15, "Description complete", null, club.getId());

        assertNotNull(dto.getId());
        assertEquals("Nouveau stage", dto.getTitre());
        assertEquals(0, dto.getNbInscrits());
        assertNull(dto.getImageUrl());
    }

    @Test
    void ajouterEvenement_avecImage_persisteLeFichierEtExposeLUrl() throws IOException {
        MultipartFile image = new MockMultipartFile("image", "photo.jpg", "image/jpeg", "contenu".getBytes());

        EvenementDTO dto = evenementService.ajouterEvenement(
                "Stage avec photo", "2026-09-01T10:00:00", "2026-09-01T12:00:00", "Dojo",
                15, "Description", image, club.getId());

        assertNotNull(dto.getImageFilename());
        assertTrue(dto.getImageUrl().endsWith(dto.getImageFilename()));
        assertTrue(Files.exists(Paths.get("uploads/evenements/" + dto.getImageFilename())));
    }

    @Test
    void saveImage_fichierNull_retourneNull() {
        assertNull(evenementService.saveImage(null));
    }

    @Test
    void saveImage_fichierVide_retourneNull() {
        MultipartFile empty = new MockMultipartFile("image", "photo.jpg", "image/jpeg", new byte[0]);

        assertNull(evenementService.saveImage(empty));
    }

    @Test
    void getAllEvenements_retourneActifsEtInactifs() {
        creerEvenement(true);
        creerEvenement(false);

        var all = evenementService.getAllEvenements();

        assertEquals(2, all.size());
    }

    @Test
    void createEvenement_sansClubDansLeDto_echoueCarClubIdEstObligatoireEnBase() {
        // Bug/code mort connu : EvenementDTO n'a pas de champ clubId, et
        // convertToEntity() ne l'affecte donc jamais. Comme la colonne club_id est
        // NOT NULL en base, ce chemin ne peut jamais aboutir en pratique. Le vrai
        // point d'entree de creation est ajouterEvenement(...), qui recoit clubId
        // explicitement en parametre.
        EvenementDTO dto = new EvenementDTO();
        dto.setTitre("Cree via DTO");
        dto.setDateDebut(LocalDateTime.now().plusDays(5));
        dto.setDateFin(LocalDateTime.now().plusDays(5).plusHours(1));
        dto.setLieu("Salle B");
        dto.setCapacite(10);
        dto.setDescription("Desc");
        dto.setActif(true);

        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> evenementService.createEvenement(dto));
    }

    @Test
    void updateEvenement_notifieUniquementLesInscrits() {
        Evenement evenement = creerEvenement(true);
        InscriptionEvenement inscription = new InscriptionEvenement();
        inscription.setEvenement(evenement);
        inscription.setMembre(enfant);
        inscription.setStatut(StatutInscription.VALIDEE);
        inscriptionRepository.save(inscription);

        EvenementDTO dto = dtoFor(evenement);
        dto.setLieu("Nouveau lieu");

        EvenementDTO updated = evenementService.updateEvenement(evenement.getId(), dto);

        assertEquals("Nouveau lieu", updated.getLieu());
    }

    @Test
    void updateEvenement_aucunChampModifie_neDeclencheAucuneErreur() {
        Evenement evenement = creerEvenement(true);
        EvenementDTO dto = dtoFor(evenement);

        EvenementDTO updated = evenementService.updateEvenement(evenement.getId(), dto);

        assertEquals(evenement.getTitre(), updated.getTitre());
    }

    @Test
    void changerStatutEvenement_annulationNotifieLesInscrits() {
        Evenement evenement = creerEvenement(true);
        InscriptionEvenement inscription = new InscriptionEvenement();
        inscription.setEvenement(evenement);
        inscription.setMembre(enfant);
        inscription.setStatut(StatutInscription.VALIDEE);
        inscriptionRepository.save(inscription);

        EvenementDTO updated = evenementService.changerStatutEvenement(evenement.getId(), false);

        assertEquals(Boolean.FALSE, updated.getActif());
    }

    @AfterEach
    void cleanupUploads() throws IOException {
        Path dir = Paths.get("uploads/evenements/");
        if (Files.exists(dir)) {
            try (var stream = Files.walk(dir)) {
                stream.sorted(Comparator.reverseOrder())
                        .filter(p -> !p.equals(dir))
                        .forEach(p -> {
                            try {
                                Files.deleteIfExists(p);
                            } catch (IOException ignored) {
                            }
                        });
            }
        }
    }
}
