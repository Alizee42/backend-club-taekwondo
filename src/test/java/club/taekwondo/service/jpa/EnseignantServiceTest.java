package club.taekwondo.service.jpa;

import club.taekwondo.dto.EnseignantDTO;
import club.taekwondo.entity.jpa.Club;
import club.taekwondo.repository.jpa.EnseignantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class EnseignantServiceTest extends AbstractServiceIntegrationTest {

    @Autowired
    private EnseignantService enseignantService;

    @Autowired
    private EnseignantRepository enseignantRepository;

    private Club club;
    private Club autreClub;

    @BeforeEach
    void setupEnseignants() {
        club = new Club();
        club.setName("Club Enseignant Test");
        club = clubRepository.save(club);

        autreClub = new Club();
        autreClub.setName("Autre Club Enseignant");
        autreClub = clubRepository.save(autreClub);
    }

    private EnseignantDTO dto(Long clubId, String nom) {
        EnseignantDTO dto = new EnseignantDTO();
        dto.setClubId(clubId);
        dto.setNom(nom);
        dto.setPrenom("Prenom");
        dto.setSpecialite("Kumite");
        return dto;
    }

    @Test
    void create_superAdmin_peutCreerPourNimporteQuelClub() {
        EnseignantDTO created = enseignantService.create(dto(club.getId(), "Dupont"), "SUPER_ADMIN", 999L);

        assertNotNull(created.getId());
        assertEquals(club.getId(), created.getClubId());
    }

    @Test
    void create_adminSurSonPropreClub_estAutorise() {
        EnseignantDTO created = enseignantService.create(dto(club.getId(), "Dupont"), "ADMIN", club.getId());

        assertNotNull(created.getId());
    }

    @Test
    void create_adminSurClubEtranger_leveSecurityException() {
        EnseignantDTO dto = dto(club.getId(), "Dupont");
        assertThrows(SecurityException.class,
                () -> enseignantService.create(dto, "ADMIN", autreClub.getId()));
    }

    @Test
    void create_adminSansClubIdConnu_leveSecurityException() {
        EnseignantDTO dto = dto(club.getId(), "Dupont");
        assertThrows(SecurityException.class,
                () -> enseignantService.create(dto, "ADMIN", null));
    }

    @Test
    void create_roleNull_bypassLaVerification() {
        EnseignantDTO created = enseignantService.create(dto(club.getId(), "Dupont"), null, null);

        assertNotNull(created.getId());
    }

    @Test
    void create_roleInconnu_bypassLaVerification() {
        EnseignantDTO created = enseignantService.create(dto(club.getId(), "Dupont"), "ROLE_BIZARRE", null);

        assertNotNull(created.getId());
    }

    @Test
    void create_clubInexistant_leveIllegalArgumentException() {
        EnseignantDTO dto = dto(999999L, "Dupont");
        assertThrows(IllegalArgumentException.class,
                () -> enseignantService.create(dto, "SUPER_ADMIN", null));
    }

    @Test
    void create_clubIdNull_leveIllegalArgumentException() {
        EnseignantDTO dto = dto(null, "Dupont");
        assertThrows(IllegalArgumentException.class,
                () -> enseignantService.create(dto, "SUPER_ADMIN", null));
    }

    @Test
    void update_adminSurClubEtranger_leveSecurityException() {
        EnseignantDTO created = enseignantService.create(dto(club.getId(), "Dupont"), "SUPER_ADMIN", null);
        Long id = created.getId();

        EnseignantDTO update = dto(club.getId(), "Nouveau Nom");
        assertThrows(SecurityException.class,
                () -> enseignantService.update(id, update, "ADMIN", autreClub.getId()));
    }

    @Test
    void update_conserveLeClubExistantSiDtoNeLeFournitPas() {
        EnseignantDTO created = enseignantService.create(dto(club.getId(), "Dupont"), "SUPER_ADMIN", null);

        EnseignantDTO updateSansClub = new EnseignantDTO();
        updateSansClub.setNom("Nom Modifie");

        Optional<EnseignantDTO> updated = enseignantService.update(created.getId(), updateSansClub, "ADMIN", club.getId());

        assertTrue(updated.isPresent());
        assertEquals("Nom Modifie", updated.get().getNom());
        assertEquals(club.getId(), updated.get().getClubId());
    }

    @Test
    void update_enseignantIntrouvable_retourneOptionalVide() {
        EnseignantDTO dto = dto(club.getId(), "X");
        assertTrue(enseignantService.update(999999L, dto, "SUPER_ADMIN", null).isEmpty());
    }

    @Test
    void delete_adminSurClubEtranger_leveSecurityException() {
        EnseignantDTO created = enseignantService.create(dto(club.getId(), "Dupont"), "SUPER_ADMIN", null);
        Long id = created.getId();

        assertThrows(SecurityException.class,
                () -> enseignantService.delete(id, "ADMIN", autreClub.getId()));
    }

    @Test
    void delete_adminSurSonPropreClub_supprimeEtRetourneTrue() {
        EnseignantDTO created = enseignantService.create(dto(club.getId(), "Dupont"), "SUPER_ADMIN", null);

        boolean deleted = enseignantService.delete(created.getId(), "ADMIN", club.getId());

        assertTrue(deleted);
        assertTrue(enseignantRepository.findById(created.getId()).isEmpty());
    }

    @Test
    void delete_introuvable_retourneFalse() {
        assertFalse(enseignantService.delete(999999L, "SUPER_ADMIN", null));
    }

    @Test
    void getByClub_neRetourneQueLesEnseignantsDuClub() {
        enseignantService.create(dto(club.getId(), "Dupont"), "SUPER_ADMIN", null);
        enseignantService.create(dto(autreClub.getId(), "Martin"), "SUPER_ADMIN", null);

        assertEquals(1, enseignantService.getByClub(club.getId()).size());
    }
}
