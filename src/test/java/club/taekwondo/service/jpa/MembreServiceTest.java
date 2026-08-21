package club.taekwondo.service.jpa;

import club.taekwondo.dto.MembreDTO;
import club.taekwondo.entity.jpa.Club;
import club.taekwondo.entity.jpa.Membre;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.enums.Role;
import club.taekwondo.repository.jpa.ClubRepository;
import club.taekwondo.repository.jpa.CommandeRepository;
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
class MembreServiceTest {

    @MockBean
    private ActualiteService actualiteService;

    @MockBean
    private GalerieService galerieService;

    @Autowired
    private MembreService membreService;

    @Autowired
    private MembreRepository membreRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private PaiementRepository paiementRepository;

    @Autowired
    private CommandeRepository commandeRepository;

    @Autowired
    private LigneCommandeRepository ligneCommandeRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UtilisateurService utilisateurService;

    private Club club;
    private Utilisateur parent;

    @BeforeEach
    void setup() {
        ligneCommandeRepository.deleteAll();
        commandeRepository.deleteAll();
        paiementRepository.deleteAll();
        membreRepository.deleteAll();
        notificationRepository.deleteAll();
        utilisateurRepository.deleteAll();
        clubRepository.deleteAll();

        club = new Club();
        club.setName("Club Membre Test");
        club = clubRepository.save(club);

        parent = new Utilisateur();
        parent.setNom("Testeur");
        parent.setPrenom("Parent");
        parent.setEmail("parent-membre@test.com");
        parent.setPassword("secret");
        parent.setRole(Role.PARENT);
        parent.setClub(club);
        parent = utilisateurService.save(parent);
    }

    @Test
    void createMembre_enfantHeriteDuClubDuParent() {
        MembreDTO dto = new MembreDTO();
        dto.setNom("Enfant");
        dto.setPrenom("Test");
        dto.setEstAdulte(false);
        dto.setUtilisateurId(parent.getId());

        MembreDTO created = membreService.createMembre(dto);

        assertNotNull(created.getId());
        assertEquals(club.getId(), created.getClubId());
        assertEquals(parent.getId(), created.getParentId());
    }

    @Test
    void createMembre_enfantSansParent_leveRuntimeException() {
        MembreDTO dto = new MembreDTO();
        dto.setNom("Enfant");
        dto.setPrenom("Test");
        dto.setEstAdulte(false);
        dto.setUtilisateurId(null);

        assertThrows(RuntimeException.class, () -> membreService.createMembre(dto));
    }

    @Test
    void createMembre_enfantAvecParentSansClub_leveRuntimeException() {
        Utilisateur parentSansClub = new Utilisateur();
        parentSansClub.setNom("Sans");
        parentSansClub.setPrenom("Club");
        parentSansClub.setEmail("sans-club@test.com");
        parentSansClub.setPassword("secret");
        parentSansClub.setRole(Role.PARENT);
        parentSansClub = utilisateurService.save(parentSansClub);

        MembreDTO dto = new MembreDTO();
        dto.setNom("Enfant");
        dto.setPrenom("Test");
        dto.setEstAdulte(false);
        dto.setUtilisateurId(parentSansClub.getId());

        assertThrows(RuntimeException.class, () -> membreService.createMembre(dto));
    }

    @Test
    void createMembre_enfantAvecClubDivergentDuDto_estForceAuClubDuParent() {
        Club autreClub = new Club();
        autreClub.setName("Autre Club");
        autreClub = clubRepository.save(autreClub);

        MembreDTO dto = new MembreDTO();
        dto.setNom("Enfant");
        dto.setPrenom("Test");
        dto.setEstAdulte(false);
        dto.setUtilisateurId(parent.getId());
        dto.setClubId(autreClub.getId());

        MembreDTO created = membreService.createMembre(dto);

        assertEquals(club.getId(), created.getClubId());
    }

    @Test
    void createMembre_adulteSansUtilisateur_leveRuntimeException() {
        MembreDTO dto = new MembreDTO();
        dto.setNom("Adulte");
        dto.setPrenom("Test");
        dto.setEstAdulte(true);
        dto.setUtilisateurId(null);

        assertThrows(RuntimeException.class, () -> membreService.createMembre(dto));
    }

    @Test
    void createMembre_adulteHeriteDuClubDeSonCompteUtilisateur() {
        Utilisateur compte = new Utilisateur();
        compte.setNom("Adulte");
        compte.setPrenom("Membre");
        compte.setEmail("adulte-membre@test.com");
        compte.setPassword("secret");
        compte.setRole(Role.MEMBRE);
        compte.setClub(club);
        compte = utilisateurService.save(compte);

        MembreDTO dto = new MembreDTO();
        dto.setNom("Adulte");
        dto.setPrenom("Membre");
        dto.setEstAdulte(true);
        dto.setUtilisateurId(compte.getId());

        MembreDTO created = membreService.createMembre(dto);

        assertEquals(club.getId(), created.getClubId());
        assertEquals(compte.getId(), created.getUtilisateurId());
    }

    @Test
    void updateMembre_enfantSansParent_leveRuntimeException() {
        Membre orphelin = new Membre();
        orphelin.setNom("Orphelin");
        orphelin.setPrenom("Test");
        orphelin.setEstAdulte(false);
        orphelin = membreService.save(orphelin);

        MembreDTO dto = new MembreDTO();
        dto.setNom("Orphelin");
        dto.setPrenom("Test");
        dto.setEstAdulte(false);

        Long id = orphelin.getId();
        assertThrows(RuntimeException.class, () -> membreService.updateMembre(id, dto));
    }

    @Test
    void updateMembre_enfant_reforceLeClubDuParentMemeSiDtoEnvoieAutreChose() {
        MembreDTO createDto = new MembreDTO();
        createDto.setNom("Enfant");
        createDto.setPrenom("Test");
        createDto.setEstAdulte(false);
        createDto.setUtilisateurId(parent.getId());
        MembreDTO created = membreService.createMembre(createDto);

        Club autreClub = new Club();
        autreClub.setName("Autre Club Update");
        autreClub = clubRepository.save(autreClub);

        MembreDTO updateDto = new MembreDTO();
        updateDto.setNom("Enfant");
        updateDto.setPrenom("Modifie");
        updateDto.setEstAdulte(false);
        updateDto.setClubId(autreClub.getId());

        MembreDTO updated = membreService.updateMembre(created.getId(), updateDto);

        assertEquals("Modifie", updated.getPrenom());
        assertEquals(club.getId(), updated.getClubId());
    }

    @Test
    void updateMembre_introuvable_leveRuntimeException() {
        MembreDTO dto = new MembreDTO();
        dto.setNom("X");
        dto.setEstAdulte(true);

        assertThrows(RuntimeException.class, () -> membreService.updateMembre(999999L, dto));
    }

    @Test
    void deleteMembre_introuvable_leveRuntimeException() {
        assertThrows(RuntimeException.class, () -> membreService.deleteMembre(999999L));
    }

    @Test
    void deleteMembre_existant_leSupprime() {
        MembreDTO createDto = new MembreDTO();
        createDto.setNom("Enfant");
        createDto.setPrenom("Test");
        createDto.setEstAdulte(false);
        createDto.setUtilisateurId(parent.getId());
        MembreDTO created = membreService.createMembre(createDto);

        membreService.deleteMembre(created.getId());

        assertTrue(membreService.getMembreById(created.getId()).isEmpty());
    }

    @Test
    void getMembresByParentEmail_emailInconnu_retourneListeVide() {
        assertTrue(membreService.getMembresByParentEmail("inconnu@test.com").isEmpty());
    }

    @Test
    void getMembresByParentEmail_retourneLesEnfantsDuParent() {
        MembreDTO createDto = new MembreDTO();
        createDto.setNom("Enfant");
        createDto.setPrenom("Test");
        createDto.setEstAdulte(false);
        createDto.setUtilisateurId(parent.getId());
        membreService.createMembre(createDto);

        assertEquals(1, membreService.getMembresByParentEmail(parent.getEmail()).size());
    }
}
