package club.taekwondo.service.jpa;

import club.taekwondo.dto.RequiredDocumentDTO;
import club.taekwondo.entity.jpa.Club;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RequiredDocumentServiceTest extends AbstractServiceIntegrationTest {

    @Autowired
    private RequiredDocumentService requiredDocumentService;

    private Club club;

    @BeforeEach
    void setupRequiredDocuments() {
        club = new Club();
        club.setName("Club Required Document Test");
        club = clubRepository.save(club);
    }

    private RequiredDocumentDTO dto(String code, String label, Integer orderIndex) {
        RequiredDocumentDTO dto = new RequiredDocumentDTO();
        dto.setClubId(club.getId());
        dto.setCode(code);
        dto.setLabel(label);
        dto.setOrderIndex(orderIndex);
        return dto;
    }

    @Test
    void create_succes_appliqueLesDefautsRequiredEtActive() {
        RequiredDocumentDTO created = requiredDocumentService.create(dto("licence", "Licence FFTDA", 1));

        assertNotNull(created.getId());
        assertTrue(created.getRequired());
        assertTrue(created.getActive());
        assertEquals(club.getId(), created.getClubId());
    }

    @Test
    void create_clubIdInvalide_leveIllegalArgumentException() {
        RequiredDocumentDTO dto = dto("licence", "Licence", 1);
        dto.setClubId(null);
        assertThrows(IllegalArgumentException.class, () -> requiredDocumentService.create(dto));

        RequiredDocumentDTO dto2 = dto("licence", "Licence", 1);
        dto2.setClubId(0L);
        assertThrows(IllegalArgumentException.class, () -> requiredDocumentService.create(dto2));
    }

    @Test
    void create_clubInexistant_leveIllegalArgumentException() {
        RequiredDocumentDTO dto = dto("licence", "Licence", 1);
        dto.setClubId(999999L);
        assertThrows(IllegalArgumentException.class, () -> requiredDocumentService.create(dto));
    }

    @Test
    void create_codeManquant_leveIllegalArgumentException() {
        RequiredDocumentDTO dto = dto("", "Licence", 1);
        assertThrows(IllegalArgumentException.class, () -> requiredDocumentService.create(dto));
    }

    @Test
    void create_labelManquant_leveIllegalArgumentException() {
        RequiredDocumentDTO dto = dto("licence", null, 1);
        assertThrows(IllegalArgumentException.class, () -> requiredDocumentService.create(dto));
    }

    @Test
    void create_memeCodeMemeClub_mettAJourLExistantAuLieuDeDupliquer() {
        RequiredDocumentDTO premier = requiredDocumentService.create(dto("licence", "Licence V1", 1));

        RequiredDocumentDTO second = requiredDocumentService.create(dto("licence", "Licence V2", 2));

        assertEquals(premier.getId(), second.getId());
        assertEquals("Licence V2", second.getLabel());
        assertEquals(1, requiredDocumentService.getByClub(club.getId()).size());
    }

    @Test
    void create_memeCodeAutreClub_creeUneEntreeDistincte() {
        requiredDocumentService.create(dto("licence", "Licence", 1));

        Club autreClub = new Club();
        autreClub.setName("Autre Club Required Document");
        autreClub = clubRepository.save(autreClub);

        RequiredDocumentDTO dtoAutreClub = new RequiredDocumentDTO();
        dtoAutreClub.setClubId(autreClub.getId());
        dtoAutreClub.setCode("licence");
        dtoAutreClub.setLabel("Licence Autre Club");
        requiredDocumentService.create(dtoAutreClub);

        assertEquals(1, requiredDocumentService.getByClub(club.getId()).size());
        assertEquals(1, requiredDocumentService.getByClub(autreClub.getId()).size());
    }

    @Test
    void update_champNonFourni_neReinitialisePasLaValeurExistante() {
        RequiredDocumentDTO created = requiredDocumentService.create(dto("licence", "Licence", 1));

        RequiredDocumentDTO update = new RequiredDocumentDTO();
        update.setActive(false);
        RequiredDocumentDTO updated = requiredDocumentService.update(created.getId(), update);

        assertEquals("Licence", updated.getLabel());
        assertFalse(updated.getActive());
    }

    @Test
    void update_idInvalide_leveIllegalArgumentException() {
        RequiredDocumentDTO update = new RequiredDocumentDTO();
        assertThrows(IllegalArgumentException.class, () -> requiredDocumentService.update(0L, update));
        assertThrows(IllegalArgumentException.class, () -> requiredDocumentService.update(null, update));
    }

    @Test
    void update_introuvable_leveIllegalArgumentException() {
        RequiredDocumentDTO update = new RequiredDocumentDTO();
        assertThrows(IllegalArgumentException.class, () -> requiredDocumentService.update(999999L, update));
    }

    @Test
    void delete_idInvalide_leveIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> requiredDocumentService.delete(0L));
        assertThrows(IllegalArgumentException.class, () -> requiredDocumentService.delete(null));
    }

    @Test
    void delete_existant_leSupprime() {
        RequiredDocumentDTO created = requiredDocumentService.create(dto("licence", "Licence", 1));

        requiredDocumentService.delete(created.getId());

        assertTrue(requiredDocumentService.getByClub(club.getId()).isEmpty());
    }

    @Test
    void getByClub_trieParOrderIndex() {
        requiredDocumentService.create(dto("photo", "Photo", 2));
        requiredDocumentService.create(dto("licence", "Licence", 1));

        List<RequiredDocumentDTO> docs = requiredDocumentService.getByClub(club.getId());

        assertEquals(2, docs.size());
        assertEquals("licence", docs.get(0).getCode());
        assertEquals("photo", docs.get(1).getCode());
    }

    @Test
    void getByClub_clubInvalide_leveIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> requiredDocumentService.getByClub(999999L));
    }
}
