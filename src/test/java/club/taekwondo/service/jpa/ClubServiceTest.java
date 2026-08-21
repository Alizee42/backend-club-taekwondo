package club.taekwondo.service.jpa;

import club.taekwondo.dto.ClubDto;
import club.taekwondo.entity.jpa.Club;
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

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ClubServiceTest {

    @MockBean
    private ActualiteService actualiteService;

    @MockBean
    private GalerieService galerieService;

    @Autowired
    private ClubService clubService;

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private LigneCommandeRepository ligneCommandeRepository;

    @Autowired
    private CommandeRepository commandeRepository;

    @Autowired
    private PaiementRepository paiementRepository;

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

    @BeforeEach
    void setup() {
        // Nettoyage complet : le club "Villeurbanne" du bootstrap a des utilisateurs rattaches
        inscriptionRepository.deleteAll();
        evenementRepository.deleteAll();
        ligneCommandeRepository.deleteAll();
        paiementRepository.deleteAll();
        commandeRepository.deleteAll();
        membreRepository.deleteAll();
        notificationRepository.deleteAll();
        utilisateurRepository.deleteAll();
        clubRepository.deleteAll();
    }

    @Test
    void createClub_nettoieLesChampsTexteEtIgnoreUnIdFourni() {
        ClubDto dto = new ClubDto();
        dto.setId(999L);
        dto.setName("  Villeurbanne  ");
        dto.setAdresse("  1 rue du Dojo  ");
        dto.setTelephone("   ");
        dto.setEmail("contact@club.fr");

        ClubDto created = clubService.createClub(dto);

        assertNotEquals(999L, created.getId());
        assertEquals("Villeurbanne", created.getName());
        assertEquals("1 rue du Dojo", created.getAdresse());
        assertNull(created.getTelephone());
    }

    @Test
    void getClubById_introuvable_retourneNull() {
        assertNull(clubService.getClubById(999999L));
    }

    @Test
    void getClubById_existant_retourneLeDto() {
        Club club = new Club();
        club.setName("Lyon");
        club = clubRepository.save(club);

        ClubDto found = clubService.getClubById(club.getId());

        assertNotNull(found);
        assertEquals("Lyon", found.getName());
    }

    @Test
    void updateClub_introuvable_retourneNull() {
        ClubDto dto = new ClubDto();
        dto.setName("X");

        assertNull(clubService.updateClub(999999L, dto));
    }

    @Test
    void updateClub_existant_appliqueEtNettoieLesChamps() {
        Club club = new Club();
        club.setName("Ancien Nom");
        club = clubRepository.save(club);

        ClubDto dto = new ClubDto();
        dto.setName("  Nouveau Nom  ");
        dto.setEmail("");

        ClubDto updated = clubService.updateClub(club.getId(), dto);

        assertEquals("Nouveau Nom", updated.getName());
        assertNull(updated.getEmail());
    }

    @Test
    void getAllClubs_retourneTousLesClubs() {
        Club a = new Club();
        a.setName("Club A");
        clubRepository.save(a);
        Club b = new Club();
        b.setName("Club B");
        clubRepository.save(b);

        assertEquals(2, clubService.getAllClubs().size());
    }

    @Test
    void deleteClub_existant_leSupprime() {
        Club club = new Club();
        club.setName("A supprimer");
        club = clubRepository.save(club);

        clubService.deleteClub(club.getId());

        assertNull(clubService.getClubById(club.getId()));
    }
}
