package club.taekwondo.controller;

import club.taekwondo.entity.jpa.Club;
import club.taekwondo.repository.jpa.ClubRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.model.AccountLink;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StripeConnectControllerTest {

    @Mock
    private ClubRepository clubRepository;

    private StripeConnectController controller;

    private MockedStatic<Account> accountStatic;
    private MockedStatic<AccountLink> accountLinkStatic;

    @BeforeEach
    void setUp() {
        controller = new StripeConnectController(clubRepository);
        ReflectionTestUtils.setField(controller, "frontendUrl", "https://frontend-club-taekwondo.netlify.app");
        accountStatic = Mockito.mockStatic(Account.class);
        accountLinkStatic = Mockito.mockStatic(AccountLink.class);
    }

    @AfterEach
    void tearDown() {
        accountStatic.close();
        accountLinkStatic.close();
    }

    private Club club(Long id, String stripeAccountId) {
        Club c = new Club();
        c.setId(id);
        c.setName("Villeurbanne");
        c.setStripeAccountId(stripeAccountId);
        return c;
    }

    // ---- onboardClub ----

    @Test
    void onboardClub_clubInexistant_retourne404() {
        when(clubRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.onboardClub(99L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void onboardClub_nouveauCompte_creeUnCompteStandardEtRenvoieUrlOnboarding() throws StripeException {
        Club c = club(1L, null);
        when(clubRepository.findById(1L)).thenReturn(Optional.of(c));

        Account account = mock(Account.class);
        when(account.getId()).thenReturn("acct_new");
        accountStatic.when(() -> Account.create(any(com.stripe.param.AccountCreateParams.class))).thenReturn(account);

        AccountLink link = mock(AccountLink.class);
        when(link.getUrl()).thenReturn("https://connect.stripe.com/setup/abc");
        accountLinkStatic.when(() -> AccountLink.create(any(com.stripe.param.AccountLinkCreateParams.class))).thenReturn(link);

        ResponseEntity<?> response = controller.onboardClub(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("acct_new", c.getStripeAccountId());
        verify(clubRepository).save(c);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("https://connect.stripe.com/setup/abc", body.get("onboardingUrl"));
    }

    @Test
    void onboardClub_compteDejaExistant_neRecreeAucunCompte() throws StripeException {
        Club c = club(1L, "acct_existant");
        when(clubRepository.findById(1L)).thenReturn(Optional.of(c));

        AccountLink link = mock(AccountLink.class);
        when(link.getUrl()).thenReturn("https://connect.stripe.com/setup/def");
        accountLinkStatic.when(() -> AccountLink.create(any(com.stripe.param.AccountLinkCreateParams.class))).thenReturn(link);

        ResponseEntity<?> response = controller.onboardClub(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        accountStatic.verify(() -> Account.create(any(com.stripe.param.AccountCreateParams.class)), never());
        verify(clubRepository, never()).save(any());
    }

    @Test
    void onboardClub_erreurStripe_retourneBadGateway() throws StripeException {
        Club c = club(1L, null);
        when(clubRepository.findById(1L)).thenReturn(Optional.of(c));
        StripeException ex = mock(StripeException.class);
        when(ex.getMessage()).thenReturn("carte refusee");
        accountStatic.when(() -> Account.create(any(com.stripe.param.AccountCreateParams.class))).thenThrow(ex);

        ResponseEntity<?> response = controller.onboardClub(1L);

        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
    }

    // ---- getStatus ----

    @Test
    void getStatus_clubInexistant_retourne404() {
        when(clubRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.getStatus(99L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void getStatus_clubNonConnecte_retourneConnectedFalse() {
        when(clubRepository.findById(1L)).thenReturn(Optional.of(club(1L, null)));

        ResponseEntity<?> response = controller.getStatus(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(false, body.get("connected"));
        assertEquals(false, body.get("chargesEnabled"));
    }

    @Test
    void getStatus_clubConnecteEtActive_retourneConnectedEtChargesEnabledTrue() {
        Club c = club(1L, "acct_1");
        c.setStripeChargesEnabled(true);
        when(clubRepository.findById(1L)).thenReturn(Optional.of(c));

        ResponseEntity<?> response = controller.getStatus(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(true, body.get("connected"));
        assertEquals(true, body.get("chargesEnabled"));
    }
}
