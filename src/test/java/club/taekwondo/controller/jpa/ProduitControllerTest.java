package club.taekwondo.controller.jpa;

import club.taekwondo.dto.ProduitDTO;
import club.taekwondo.entity.jpa.Club;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.service.common.FileUploadService;
import club.taekwondo.service.jpa.ProduitService;
import club.taekwondo.service.jpa.UtilisateurService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProduitControllerTest {

    @Mock
    private ProduitService produitService;

    @Mock
    private UtilisateurService utilisateurService;

    @Mock
    private FileUploadService fileUploadService;

    private ProduitController controller;

    @BeforeEach
    void setUp() {
        controller = new ProduitController();
        ReflectionTestUtils.setField(controller, "produitService", produitService);
        ReflectionTestUtils.setField(controller, "utilisateurService", utilisateurService);
        ReflectionTestUtils.setField(controller, "fileUploadService", fileUploadService);
    }

    private Authentication auth(String email) {
        return new TestingAuthenticationToken(email, null, "ROLE_ADMIN");
    }

    private Utilisateur user(Long clubId) {
        Utilisateur u = new Utilisateur();
        if (clubId != null) {
            Club club = new Club();
            club.setId(clubId);
            u.setClub(club);
        }
        return u;
    }

    @Test
    void getProduitsByClub_delegueAuService() {
        when(produitService.getProduitsByClubId(1L)).thenReturn(List.of(new ProduitDTO()));

        ResponseEntity<List<ProduitDTO>> response = controller.getProduitsByClub(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getAllProduits_delegueAuService() {
        when(produitService.getAllProduits()).thenReturn(List.of(new ProduitDTO(), new ProduitDTO()));

        ResponseEntity<List<ProduitDTO>> response = controller.getAllProduits();

        assertEquals(2, response.getBody().size());
    }

    @Test
    void getProduitById_trouve_retourneOk() {
        when(produitService.getProduitById(1L)).thenReturn(Optional.of(new ProduitDTO()));

        ResponseEntity<ProduitDTO> response = controller.getProduitById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getProduitById_absent_retourneNotFound() {
        when(produitService.getProduitById(1L)).thenReturn(Optional.empty());

        ResponseEntity<ProduitDTO> response = controller.getProduitById(1L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void createProduit_sansClubId_resoutDepuisLAppelant() {
        when(utilisateurService.findByEmail("admin@test.com")).thenReturn(Optional.of(user(5L)));
        when(produitService.createProduit(any(ProduitDTO.class))).thenAnswer(inv -> inv.getArgument(0));

        ProduitDTO dto = new ProduitDTO();
        ResponseEntity<ProduitDTO> response = controller.createProduit(dto, auth("admin@test.com"));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(5L, response.getBody().getClubId());
    }

    @Test
    void createProduit_avecClubIdFourni_neLEcrasePas() {
        ProduitDTO dto = new ProduitDTO();
        dto.setClubId(9L);
        when(produitService.createProduit(any(ProduitDTO.class))).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<ProduitDTO> response = controller.createProduit(dto, auth("admin@test.com"));

        assertEquals(9L, response.getBody().getClubId());
        verify(utilisateurService, org.mockito.Mockito.never()).findByEmail(anyString());
    }

    @Test
    void createProduit_erreurValidation_retourneBadRequest() {
        ProduitDTO dto = new ProduitDTO();
        dto.setClubId(1L);
        when(produitService.createProduit(any(ProduitDTO.class))).thenThrow(new IllegalArgumentException("invalide"));

        ResponseEntity<ProduitDTO> response = controller.createProduit(dto, auth("admin@test.com"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void updateProduit_sansClubId_resoutDepuisLAppelant() {
        when(utilisateurService.findByEmail("admin@test.com")).thenReturn(Optional.of(user(5L)));
        when(produitService.updateProduit(anyLong(), any(ProduitDTO.class))).thenAnswer(inv -> inv.getArgument(1));

        ProduitDTO dto = new ProduitDTO();
        ResponseEntity<ProduitDTO> response = controller.updateProduit(1L, dto, auth("admin@test.com"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(5L, response.getBody().getClubId());
    }

    @Test
    void updateProduit_erreurValidation_retourneNotFound() {
        ProduitDTO dto = new ProduitDTO();
        dto.setClubId(1L);
        when(produitService.updateProduit(anyLong(), any(ProduitDTO.class))).thenThrow(new IllegalArgumentException("absent"));

        ResponseEntity<ProduitDTO> response = controller.updateProduit(1L, dto, auth("admin@test.com"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void uploadImage_succes_retourneUrl() throws Exception {
        when(fileUploadService.uploadFile(any(MultipartFile.class), org.mockito.ArgumentMatchers.eq("produits")))
                .thenReturn("produits/photo.png");
        MockMultipartFile file = new MockMultipartFile("image", "photo.png", "image/png", new byte[]{1});

        ResponseEntity<Map<String, String>> response = controller.uploadImage(file);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("/uploads/produits/photo.png", response.getBody().get("url"));
    }

    @Test
    void uploadImage_erreur_retourneBadRequestAvecMessage() throws Exception {
        when(fileUploadService.uploadFile(any(MultipartFile.class), org.mockito.ArgumentMatchers.eq("produits")))
                .thenThrow(new RuntimeException("Disque plein"));
        MockMultipartFile file = new MockMultipartFile("image", "photo.png", "image/png", new byte[]{1});

        ResponseEntity<Map<String, String>> response = controller.uploadImage(file);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().get("error").contains("Disque plein"));
    }

    @Test
    void deleteProduit_succes_retourneNoContent() {
        ResponseEntity<Void> response = controller.deleteProduit(1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(produitService).deleteProduit(1L);
    }

    @Test
    void deleteProduit_absent_retourneNotFound() {
        org.mockito.Mockito.doThrow(new IllegalArgumentException("absent")).when(produitService).deleteProduit(1L);

        ResponseEntity<Void> response = controller.deleteProduit(1L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    private static String anyString() {
        return org.mockito.ArgumentMatchers.anyString();
    }
}
