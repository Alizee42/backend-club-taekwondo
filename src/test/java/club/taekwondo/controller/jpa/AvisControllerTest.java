package club.taekwondo.controller.jpa;

import club.taekwondo.dto.AvisDTO;
import club.taekwondo.entity.jpa.Avis;
import club.taekwondo.entity.jpa.Club;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.repository.jpa.ClubRepository;
import club.taekwondo.repository.jpa.UtilisateurRepository;
import club.taekwondo.service.jpa.AvisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvisControllerTest {

    @Mock
    private AvisService avisService;

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private UtilisateurRepository utilisateurRepository;

    private AvisController controller;

    @BeforeEach
    void setUp() {
        controller = new AvisController();
        ReflectionTestUtils.setField(controller, "avisService", avisService);
        ReflectionTestUtils.setField(controller, "clubRepository", clubRepository);
        ReflectionTestUtils.setField(controller, "utilisateurRepository", utilisateurRepository);
    }

    private AvisDTO avis(Long id, Boolean approuve, String typeAvis) {
        AvisDTO dto = new AvisDTO();
        dto.setId(id != null ? id.intValue() : null);
        dto.setApprouve(approuve);
        dto.setTypeAvis(typeAvis);
        return dto;
    }

    @Test
    void getAvisByClub_listeVide_retourneNoContent() {
        when(avisService.getAvisByClubId(1L)).thenReturn(List.of());

        ResponseEntity<List<AvisDTO>> response = controller.getAvisByClub(1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void getAvisByClub_listeNonVide_retourneOk() {
        when(avisService.getAvisByClubId(1L)).thenReturn(List.of(avis(1L, true, "cours")));

        ResponseEntity<List<AvisDTO>> response = controller.getAvisByClub(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getAllAvis_filtreParApprouveEtType() {
        when(avisService.getAllAvis()).thenReturn(List.of(
                avis(1L, true, "cours"),
                avis(2L, false, "cours"),
                avis(3L, true, "evenements")
        ));

        ResponseEntity<List<AvisDTO>> response = controller.getAllAvis(true, "cours");

        assertEquals(1, response.getBody().size());
        assertEquals(1, response.getBody().get(0).getId());
    }

    @Test
    void getAllAvis_typeInconnu_ignoreLeFiltreType() {
        when(avisService.getAllAvis()).thenReturn(List.of(avis(1L, true, "cours")));

        ResponseEntity<List<AvisDTO>> response = controller.getAllAvis(null, "type-invalide");

        assertEquals(1, response.getBody().size());
    }

    @Test
    void getAllAvis_serviceRetourneNull_neLevePasException() {
        when(avisService.getAllAvis()).thenReturn(null);

        ResponseEntity<List<AvisDTO>> response = controller.getAllAvis(null, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0, response.getBody().size());
    }

    @Test
    void countAvis_retourneLeNombreApresFiltrage() {
        when(avisService.getAllAvis()).thenReturn(List.of(
                avis(1L, true, "cours"),
                avis(2L, false, "cours")
        ));

        ResponseEntity<Map<String, Long>> response = controller.countAvis(true, null);

        assertEquals(1L, response.getBody().get("count"));
    }

    @Test
    void createAvisAvecFichier_contenuTropCourt_retourneBadRequest() {
        ResponseEntity<?> response = controller.createAvisAvecFichier(
                "ab", 5, "Visiteur", "cours", null, null, null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(avisService, never()).ajouterAvis(any());
    }

    @Test
    void createAvisAvecFichier_pseudoManquant_retourneBadRequest() {
        ResponseEntity<?> response = controller.createAvisAvecFichier(
                "Contenu valide", 5, "  ", "cours", null, null, null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void createAvisAvecFichier_clubIdFourniIntrouvable_retourneBadRequest() {
        when(clubRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.createAvisAvecFichier(
                "Contenu valide", 5, "Visiteur", "cours", null, 999L, null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(avisService, never()).ajouterAvis(any());
    }

    @Test
    void createAvisAvecFichier_succes_retourneCreated() {
        Avis saved = new Avis();
        saved.setId(10);
        when(avisService.ajouterAvis(any())).thenReturn(saved);
        when(avisService.getAvisById(10)).thenReturn(Optional.of(avis(10L, false, "cours")));

        ResponseEntity<?> response = controller.createAvisAvecFichier(
                "Contenu valide de l'avis", 5, "Visiteur", "cours", null, null, null);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(avisService).ajouterAvis(any());
    }

    @Test
    void createAvisAvecFichier_noteHorsBornes_estClampee() {
        Avis saved = new Avis();
        saved.setId(11);
        when(avisService.ajouterAvis(any())).thenReturn(saved);
        when(avisService.getAvisById(11)).thenReturn(Optional.of(avis(11L, false, "cours")));

        controller.createAvisAvecFichier("Contenu valide de l'avis", 99, "Visiteur", "cours", null, null, null);

        org.mockito.ArgumentCaptor<Avis> captor = org.mockito.ArgumentCaptor.forClass(Avis.class);
        verify(avisService).ajouterAvis(captor.capture());
        assertEquals(5, captor.getValue().getNote());
    }

    @Test
    void createAvisAvecFichier_avecClubValide_associeLeClub() {
        Club club = new Club();
        club.setId(1L);
        when(clubRepository.findById(1L)).thenReturn(Optional.of(club));
        Avis saved = new Avis();
        saved.setId(12);
        when(avisService.ajouterAvis(any())).thenReturn(saved);
        when(avisService.getAvisById(12)).thenReturn(Optional.of(avis(12L, false, "cours")));

        controller.createAvisAvecFichier("Contenu valide de l'avis", 5, "Visiteur", "cours", null, 1L, null);

        org.mockito.ArgumentCaptor<Avis> captor = org.mockito.ArgumentCaptor.forClass(Avis.class);
        verify(avisService).ajouterAvis(captor.capture());
        assertEquals(club, captor.getValue().getClub());
    }

    @Test
    void createAvisAvecFichier_avecUtilisateurIdIntrouvable_neLAssociePas() {
        when(utilisateurRepository.findById(999L)).thenReturn(Optional.empty());
        Avis saved = new Avis();
        saved.setId(13);
        when(avisService.ajouterAvis(any())).thenReturn(saved);
        when(avisService.getAvisById(13)).thenReturn(Optional.of(avis(13L, false, "cours")));

        controller.createAvisAvecFichier("Contenu valide de l'avis", 5, "Visiteur", "cours", null, null, 999L);

        org.mockito.ArgumentCaptor<Avis> captor = org.mockito.ArgumentCaptor.forClass(Avis.class);
        verify(avisService).ajouterAvis(captor.capture());
        assertNull(captor.getValue().getUtilisateur());
    }

    @Test
    void createAvisAvecFichier_avecUtilisateurIdValide_lAssocie() {
        Utilisateur user = new Utilisateur();
        user.setId(5L);
        when(utilisateurRepository.findById(5L)).thenReturn(Optional.of(user));
        Avis saved = new Avis();
        saved.setId(14);
        when(avisService.ajouterAvis(any())).thenReturn(saved);
        when(avisService.getAvisById(14)).thenReturn(Optional.of(avis(14L, false, "cours")));

        controller.createAvisAvecFichier("Contenu valide de l'avis", 5, "Visiteur", "cours", null, null, 5L);

        org.mockito.ArgumentCaptor<Avis> captor = org.mockito.ArgumentCaptor.forClass(Avis.class);
        verify(avisService).ajouterAvis(captor.capture());
        assertEquals(user, captor.getValue().getUtilisateur());
    }

    @Test
    void createAvisAvecFichier_photoTypeInvalide_retourneBadRequest() {
        MockMultipartFile file = new MockMultipartFile("photo", "doc.pdf", "application/pdf", new byte[]{1});

        ResponseEntity<?> response = controller.createAvisAvecFichier(
                "Contenu valide de l'avis", 5, "Visiteur", "cours", file, null, null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(avisService, never()).ajouterAvis(any());
    }

    @Test
    void createAvisAvecFichier_photoTropVolumineuse_retourneBadRequest() {
        byte[] gros = new byte[3_000_001];
        MockMultipartFile file = new MockMultipartFile("photo", "img.jpg", "image/jpeg", gros);

        ResponseEntity<?> response = controller.createAvisAvecFichier(
                "Contenu valide de l'avis", 5, "Visiteur", "cours", file, null, null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void updateAvis_succes_retourneOk() {
        AvisDTO update = avis(1L, null, "Evenements");
        when(avisService.updateAvis(anyInt(), any())).thenReturn(avis(1L, true, "evenements"));

        ResponseEntity<AvisDTO> response = controller.updateAvis(1, update);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void updateAvis_introuvable_retourneNotFound() {
        when(avisService.updateAvis(anyInt(), any())).thenThrow(new RuntimeException("introuvable"));

        ResponseEntity<AvisDTO> response = controller.updateAvis(999, avis(999L, null, null));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void approuverAvis_succes_retourneOk() {
        when(avisService.approuverAvis(1)).thenReturn(avis(1L, true, "cours"));

        ResponseEntity<AvisDTO> response = controller.approuverAvis(1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void approuverAvis_introuvable_retourneNotFound() {
        when(avisService.approuverAvis(999)).thenThrow(new RuntimeException("introuvable"));

        ResponseEntity<AvisDTO> response = controller.approuverAvis(999);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void deleteAvis_appelleLeServiceEtRetourneNoContent() {
        ResponseEntity<Void> response = controller.deleteAvis(1);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(avisService).deleteAvis(1);
    }
}
