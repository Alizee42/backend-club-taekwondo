package club.taekwondo.service.jpa;

import club.taekwondo.dto.EvenementDTO;
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

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class EvenementServiceTest {

    @MockBean
    private ActualiteService actualiteService;

    @MockBean
    private GalerieService galerieService;

    @Autowired
    private EvenementService evenementService;

    @Autowired
    private EvenementRepository evenementRepository;

    @Autowired
    private InscriptionEvenementRepository inscriptionRepository;

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
}
