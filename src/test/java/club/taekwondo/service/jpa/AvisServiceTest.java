package club.taekwondo.service.jpa;

import club.taekwondo.dto.AvisDTO;
import club.taekwondo.entity.jpa.Avis;
import club.taekwondo.entity.jpa.Club;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class AvisServiceTest extends AbstractServiceIntegrationTest {

    @Autowired
    private AvisService avisService;

    private Club club;

    @BeforeEach
    void setupAvis() {
        club = new Club();
        club.setName("Club Avis Test");
        club = clubRepository.save(club);
    }

    private AvisDTO dto(String contenu, Integer note, String typeAvis) {
        AvisDTO dto = new AvisDTO();
        dto.setDatePub(LocalDate.now());
        dto.setContenu(contenu);
        dto.setPseudoVisiteur("Visiteur");
        dto.setNote(note);
        dto.setTypeAvis(typeAvis);
        dto.setClubId(club.getId());
        return dto;
    }

    @Test
    void createAvis_typeValide_estConserveEnMinuscules() {
        AvisDTO created = avisService.createAvis(dto("Super club", 5, "Cours"));

        assertEquals("cours", created.getTypeAvis());
    }

    @Test
    void createAvis_typeInconnu_estRejeteVersNull() {
        AvisDTO created = avisService.createAvis(dto("Super club", 5, "n-importe-quoi"));

        assertNull(created.getTypeAvis());
    }

    @Test
    void createAvis_typeNull_resteNull() {
        AvisDTO created = avisService.createAvis(dto("Super club", 5, null));

        assertNull(created.getTypeAvis());
    }

    @Test
    void createAvis_noteSuperieureA5_estClampeeA5() {
        AvisDTO created = avisService.createAvis(dto("Super club", 10, "cours"));

        assertEquals(5, created.getNote());
    }

    @Test
    void createAvis_noteInferieureA1_estClampeeA1() {
        AvisDTO created = avisService.createAvis(dto("Super club", -3, "cours"));

        assertEquals(1, created.getNote());
    }

    @Test
    void createAvis_noteNull_prendLaValeurParDefaut5() {
        AvisDTO created = avisService.createAvis(dto("Super club", null, "cours"));

        assertEquals(5, created.getNote());
    }

    @Test
    void createAvis_nonApprouveParDefaut() {
        AvisDTO created = avisService.createAvis(dto("Super club", 4, "cours"));

        assertFalse(created.getApprouve());
    }

    @Test
    void approuverAvis_passeApprouveATrue() {
        AvisDTO created = avisService.createAvis(dto("Super club", 4, "cours"));

        AvisDTO approuve = avisService.approuverAvis(created.getId());

        assertTrue(approuve.getApprouve());
    }

    @Test
    void approuverAvis_introuvable_leveRuntimeException() {
        assertThrows(RuntimeException.class, () -> avisService.approuverAvis(999999));
    }

    @Test
    void updateAvis_neModifiePasLeStatutApprouve() {
        AvisDTO created = avisService.createAvis(dto("Contenu initial", 3, "cours"));
        avisService.approuverAvis(created.getId());

        AvisDTO update = dto("Contenu modifie", 4, "evenements");
        AvisDTO updated = avisService.updateAvis(created.getId(), update);

        assertEquals("Contenu modifie", updated.getContenu());
        assertEquals("evenements", updated.getTypeAvis());
        assertTrue(updated.getApprouve());
    }

    @Test
    void updateAvis_introuvable_leveRuntimeException() {
        AvisDTO update = dto("X", 3, "cours");
        assertThrows(RuntimeException.class, () -> avisService.updateAvis(999999, update));
    }

    @Test
    void deleteAvis_supprimeLAvis() {
        AvisDTO created = avisService.createAvis(dto("A supprimer", 3, "cours"));

        avisService.deleteAvis(created.getId());

        assertTrue(avisService.getAvisById(created.getId()).isEmpty());
    }

    @Test
    void getAllAvis_filtreParApprouveEtType() {
        AvisDTO a1 = avisService.createAvis(dto("Avis 1", 5, "cours"));
        avisService.approuverAvis(a1.getId());
        avisService.createAvis(dto("Avis 2", 4, "cours"));
        avisService.createAvis(dto("Avis 3", 3, "evenements"));

        assertEquals(1, avisService.getAllAvis(true, "cours").size());
        assertEquals(2, avisService.getAllAvis(false, null).size());
        assertEquals(2, avisService.getAllAvis(null, "cours").size());
    }

    @Test
    void countAvis_filtreParApprouveEtType() {
        AvisDTO a1 = avisService.createAvis(dto("Avis 1", 5, "cours"));
        avisService.approuverAvis(a1.getId());
        avisService.createAvis(dto("Avis 2", 4, "cours"));

        assertEquals(1, avisService.countAvis(true, "cours"));
        assertEquals(2, avisService.countAvis(null, "cours"));
    }

    @Test
    void getAvisByClubId_neRetourneQueLesAvisDuClub() {
        // createAvis(DTO) ne mappe pas le clubId vers l'entite (bug/omission du service) ;
        // le vrai chemin de production (AvisController) construit l'entite directement
        // avec setClub() puis appelle ajouterAvis().
        Avis avisDuClub = new Avis();
        avisDuClub.setDatePub(LocalDate.now());
        avisDuClub.setContenu("Avis club");
        avisDuClub.setNote(5);
        avisDuClub.setClub(club);
        avisService.ajouterAvis(avisDuClub);

        Club autreClub = new Club();
        autreClub.setName("Autre Club Avis");
        autreClub = clubRepository.save(autreClub);

        Avis autreAvis = new Avis();
        autreAvis.setDatePub(LocalDate.now());
        autreAvis.setContenu("Autre avis");
        autreAvis.setNote(4);
        autreAvis.setClub(autreClub);
        avisRepository.save(autreAvis);

        assertEquals(1, avisService.getAvisByClubId(club.getId()).size());
    }

    @Test
    void ajouterAvis_entiteDirecte_normaliseTypeEtNote() {
        Avis avis = new Avis();
        avis.setDatePub(LocalDate.now());
        avis.setContenu("Depuis multipart");
        avis.setNote(0);
        avis.setTypeAvis("COMPETITIONS");
        avis.setClub(club);

        Avis saved = avisService.ajouterAvis(avis);

        assertEquals(1, saved.getNote());
        assertEquals("competitions", saved.getTypeAvis());
        assertFalse(saved.getApprouve());
    }
}
