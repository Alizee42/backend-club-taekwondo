package club.taekwondo.controller;

import club.taekwondo.dto.ContactMessageDTO;
import club.taekwondo.service.jpa.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PublicContactControllerTest {

    @Mock
    private EmailService emailService;

    @Test
    void envoyer_delegueAuServiceEtRetourneOk() {
        PublicContactController controller = new PublicContactController(emailService);
        ContactMessageDTO dto = new ContactMessageDTO();
        dto.setName("Jean");
        dto.setEmail("jean@test.com");
        dto.setObjet("Question");
        dto.setMessage("Bonjour");

        ResponseEntity<?> response = controller.envoyer(dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(emailService).envoyerMessageContact("Jean", "jean@test.com", "Question", "Bonjour");
    }
}
