package club.taekwondo.service.jpa;

import club.taekwondo.dto.MembreDTO;
import club.taekwondo.entity.jpa.Club;
import club.taekwondo.entity.jpa.Membre;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

class MembreServiceTest extends AbstractServiceIntegrationTest {

    @Autowired
    private MembreService membreService;

    @Autowired
    private UtilisateurService utilisateurService;

    private Club club;
    private Utilisateur parent;

    @BeforeEach
    void setupMembres() {
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

    // ---- lectures diverses ----

    @Test
    void getAllMembres_retourneTousLesMembres() {
        MembreDTO dto = new MembreDTO();
        dto.setNom("Enfant");
        dto.setPrenom("Test");
        dto.setEstAdulte(false);
        dto.setUtilisateurId(parent.getId());
        membreService.createMembre(dto);

        assertEquals(1, membreService.getAllMembres().size());
    }

    @Test
    void getMembreById_trouve_retourneLeDto() {
        MembreDTO createDto = new MembreDTO();
        createDto.setNom("Enfant");
        createDto.setPrenom("Test");
        createDto.setEstAdulte(false);
        createDto.setUtilisateurId(parent.getId());
        MembreDTO created = membreService.createMembre(createDto);

        assertTrue(membreService.getMembreById(created.getId()).isPresent());
    }

    @Test
    void getMembresByClubId_neRetourneQueLesMembresDuClub() {
        MembreDTO dto = new MembreDTO();
        dto.setNom("Enfant");
        dto.setPrenom("Test");
        dto.setEstAdulte(false);
        dto.setUtilisateurId(parent.getId());
        membreService.createMembre(dto);

        Club autreClub = new Club();
        autreClub.setName("Autre Club Filtre");
        clubRepository.save(autreClub);

        assertEquals(1, membreService.getMembresByClubId(club.getId()).size());
    }

    @Test
    void getEnfantsDuParent_retourneLesEntitesEnfants() {
        MembreDTO createDto = new MembreDTO();
        createDto.setNom("Enfant");
        createDto.setPrenom("Test");
        createDto.setEstAdulte(false);
        createDto.setUtilisateurId(parent.getId());
        membreService.createMembre(createDto);

        assertEquals(1, membreService.getEnfantsDuParent(parent.getId()).size());
    }

    @Test
    void getMembreEntityByIdUtilisateur_etAlias_retournentLeMemeResultat() {
        Utilisateur compte = new Utilisateur();
        compte.setNom("Adulte");
        compte.setPrenom("Compte");
        compte.setEmail("adulte-compte@test.com");
        compte.setPassword("secret");
        compte.setRole(Role.MEMBRE);
        compte.setClub(club);
        compte = utilisateurService.save(compte);

        MembreDTO dto = new MembreDTO();
        dto.setNom("Adulte");
        dto.setPrenom("Compte");
        dto.setEstAdulte(true);
        dto.setUtilisateurId(compte.getId());
        MembreDTO created = membreService.createMembre(dto);

        assertEquals(created.getId(), membreService.getMembreEntityByIdUtilisateur(compte.getId()).orElseThrow().getId());
        assertEquals(created.getId(), membreService.findByCompteUtilisateurId(compte.getId()).orElseThrow().getId());
    }

    // ---- updateMembre : branches adulte ----

    @Test
    void updateMembre_adulteAvecClubIdFourni_metAJourLeClub() {
        Utilisateur compte = new Utilisateur();
        compte.setNom("Adulte");
        compte.setPrenom("Compte");
        compte.setEmail("adulte-update@test.com");
        compte.setPassword("secret");
        compte.setRole(Role.MEMBRE);
        compte.setClub(club);
        compte = utilisateurService.save(compte);

        MembreDTO createDto = new MembreDTO();
        createDto.setNom("Adulte");
        createDto.setPrenom("Compte");
        createDto.setEstAdulte(true);
        createDto.setUtilisateurId(compte.getId());
        MembreDTO created = membreService.createMembre(createDto);

        Club nouveauClub = new Club();
        nouveauClub.setName("Nouveau Club Adulte");
        nouveauClub = clubRepository.save(nouveauClub);

        MembreDTO updateDto = new MembreDTO();
        updateDto.setNom("Adulte");
        updateDto.setPrenom("Compte");
        updateDto.setEstAdulte(true);
        updateDto.setClubId(nouveauClub.getId());

        MembreDTO updated = membreService.updateMembre(created.getId(), updateDto);

        assertEquals(nouveauClub.getId(), updated.getClubId());
    }

    @Test
    void updateMembre_adulteSansClubIdMaisAvecCompteUtilisateur_heriteDuClubDuCompte() {
        Utilisateur compte = new Utilisateur();
        compte.setNom("Adulte");
        compte.setPrenom("Compte");
        compte.setEmail("adulte-heritage@test.com");
        compte.setPassword("secret");
        compte.setRole(Role.MEMBRE);
        compte.setClub(club);
        compte = utilisateurService.save(compte);

        MembreDTO createDto = new MembreDTO();
        createDto.setNom("Adulte");
        createDto.setPrenom("Compte");
        createDto.setEstAdulte(true);
        createDto.setUtilisateurId(compte.getId());
        MembreDTO created = membreService.createMembre(createDto);

        MembreDTO updateDto = new MembreDTO();
        updateDto.setNom("Adulte");
        updateDto.setPrenom("ModifieSansClub");
        updateDto.setEstAdulte(true);

        MembreDTO updated = membreService.updateMembre(created.getId(), updateDto);

        assertEquals("ModifieSansClub", updated.getPrenom());
        assertEquals(club.getId(), updated.getClubId());
    }

    // ---- createMembre : club fourni directement via DTO ----

    @Test
    void createMembre_adulteAvecClubIdDto_prevautSurAbsenceDeCompteClub() {
        Utilisateur compte = new Utilisateur();
        compte.setNom("Adulte");
        compte.setPrenom("SansClub");
        compte.setEmail("adulte-sans-club@test.com");
        compte.setPassword("secret");
        compte.setRole(Role.MEMBRE);
        compte = utilisateurService.save(compte);

        MembreDTO dto = new MembreDTO();
        dto.setNom("Adulte");
        dto.setPrenom("SansClub");
        dto.setEstAdulte(true);
        dto.setUtilisateurId(compte.getId());
        dto.setClubId(club.getId());

        MembreDTO created = membreService.createMembre(dto);

        assertEquals(club.getId(), created.getClubId());
    }

    @Test
    void createMembre_clubIdInexistant_leveRuntimeException() {
        Utilisateur compte = new Utilisateur();
        compte.setNom("Adulte");
        compte.setPrenom("Test");
        compte.setEmail("adulte-club-inexistant@test.com");
        compte.setPassword("secret");
        compte.setRole(Role.MEMBRE);
        compte = utilisateurService.save(compte);

        MembreDTO dto = new MembreDTO();
        dto.setNom("Adulte");
        dto.setPrenom("Test");
        dto.setEstAdulte(true);
        dto.setUtilisateurId(compte.getId());
        dto.setClubId(999999L);

        assertThrows(RuntimeException.class, () -> membreService.createMembre(dto));
    }

    @Test
    void createMembre_genreValide_estPersiste() {
        MembreDTO dto = new MembreDTO();
        dto.setNom("Enfant");
        dto.setPrenom("Genre");
        dto.setEstAdulte(false);
        dto.setUtilisateurId(parent.getId());
        dto.setGenre("MASCULIN");

        MembreDTO created = membreService.createMembre(dto);

        assertEquals("MASCULIN", created.getGenre());
    }

    @Test
    void createMembre_genreInvalide_estIgnoreSansException() {
        MembreDTO dto = new MembreDTO();
        dto.setNom("Enfant");
        dto.setPrenom("GenreInvalide");
        dto.setEstAdulte(false);
        dto.setUtilisateurId(parent.getId());
        dto.setGenre("PAS_UN_GENRE");

        MembreDTO created = membreService.createMembre(dto);

        assertNull(created.getGenre());
    }
}
