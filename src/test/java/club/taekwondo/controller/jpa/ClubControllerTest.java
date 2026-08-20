package club.taekwondo.controller.jpa;

import club.taekwondo.dto.ClubDto;
import club.taekwondo.service.common.FileUploadService;
import club.taekwondo.service.jpa.ClubService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClubControllerTest {

    @Mock
    private ClubService clubService;

    @Mock
    private FileUploadService fileUploadService;

    private ClubController controller;

    @BeforeEach
    void setUp() {
        controller = new ClubController();
        ReflectionTestUtils.setField(controller, "clubService", clubService);
        ReflectionTestUtils.setField(controller, "fileUploadService", fileUploadService);
    }

    @Test
    void getClubById_notFound_returns404() {
        when(clubService.getClubById(99L)).thenReturn(null);

        ResponseEntity<ClubDto> response = controller.getClubById(99L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void getClubById_found_returnsClub() {
        ClubDto club = new ClubDto();
        club.setId(1L);
        club.setName("Villeurbanne");
        when(clubService.getClubById(1L)).thenReturn(club);

        ResponseEntity<ClubDto> response = controller.getClubById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Villeurbanne", response.getBody().getName());
    }

    @Test
    void getAllClubs_delegatesToService() {
        when(clubService.getAllClubs()).thenReturn(List.of(new ClubDto(), new ClubDto()));

        List<ClubDto> result = controller.getAllClubs();

        assertEquals(2, result.size());
    }

    @Test
    void updateClub_notFound_returns404() {
        ClubDto dto = new ClubDto();
        dto.setName("Nouveau");
        when(clubService.updateClub(99L, dto)).thenReturn(null);

        ResponseEntity<ClubDto> response = controller.updateClub(99L, dto);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void uploadLogo_success_returnsPath() throws IOException {
        MockMultipartFile file = new MockMultipartFile("logo", "logo.png", "image/png", "data".getBytes());
        when(fileUploadService.uploadFile(any(), anyString())).thenReturn("clubs/logo.png");

        ResponseEntity<Map<String, String>> response = controller.uploadLogo(file);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("clubs/logo.png", response.getBody().get("path"));
    }

    @Test
    void uploadLogo_serviceThrows_returnsBadRequest() throws IOException {
        MockMultipartFile file = new MockMultipartFile("logo", "logo.png", "image/png", "data".getBytes());
        when(fileUploadService.uploadFile(any(), anyString())).thenThrow(new IOException("disque plein"));

        ResponseEntity<Map<String, String>> response = controller.uploadLogo(file);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNull(response.getBody());
    }
}
