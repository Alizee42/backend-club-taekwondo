package club.taekwondo.controller;

import club.taekwondo.dto.ContactMessageDTO;
import club.taekwondo.entity.jpa.Club;
import club.taekwondo.repository.jpa.ClubRepository;
import club.taekwondo.service.jpa.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicContactControllerTest {

    @Mock
    private EmailService emailService;

    @Mock
    private ClubRepository clubRepository;

    private PublicContactController controller;

    @BeforeEach
    void setUp() {
        controller = new PublicContactController(emailService, clubRepository);
    }

    @Test
    void envoyer_sansClubId_delegueAuServiceSansClubEtRetourneOk() {
        ContactMessageDTO dto = new ContactMessageDTO();
        dto.setName("Jean");
        dto.setEmail("jean@test.com");
        dto.setObjet("Question");
        dto.setMessage("Bonjour");

        ResponseEntity<?> response = controller.envoyer(dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(emailService).envoyerMessageContact(isNull(), eq("Jean"), eq("jean@test.com"), eq("Question"), eq("Bonjour"));
    }

    @Test
    void envoyer_avecClubIdConnu_resoutLeClubEtLeTransmet() {
        Club club = new Club();
        club.setId(10L);
        when(clubRepository.findById(10L)).thenReturn(Optional.of(club));

        ContactMessageDTO dto = new ContactMessageDTO();
        dto.setClubId(10L);
        dto.setName("Jean");
        dto.setEmail("jean@test.com");
        dto.setObjet("Question");
        dto.setMessage("Bonjour");

        ResponseEntity<?> response = controller.envoyer(dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(emailService).envoyerMessageContact(eq(club), eq("Jean"), eq("jean@test.com"), eq("Question"), eq("Bonjour"));
    }

    @Test
    void envoyer_avecClubIdInconnu_transmetNullSansEchouer() {
        when(clubRepository.findById(999L)).thenReturn(Optional.empty());

        ContactMessageDTO dto = new ContactMessageDTO();
        dto.setClubId(999L);
        dto.setName("Jean");
        dto.setEmail("jean@test.com");
        dto.setObjet("Question");
        dto.setMessage("Bonjour");

        ResponseEntity<?> response = controller.envoyer(dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(emailService).envoyerMessageContact(isNull(), eq("Jean"), eq("jean@test.com"), eq("Question"), eq("Bonjour"));
    }

    private static <T> T eq(T value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }
}
