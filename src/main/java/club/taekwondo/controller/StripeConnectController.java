package club.taekwondo.controller;

import club.taekwondo.entity.jpa.Club;
import club.taekwondo.repository.jpa.ClubRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.model.AccountLink;
import com.stripe.param.AccountCreateParams;
import com.stripe.param.AccountLinkCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Onboarding Stripe Connect (comptes "Standard") par club : chaque club connecte
 * son propre compte Stripe pour recevoir directement ses paiements par carte,
 * sans commission de plateforme (charges directes, voir StripeController).
 * Reserve au SUPER_ADMIN, declenche de facon centralisee depuis /super-admin/clubs.
 */
@RestController
@RequestMapping("/api/stripe/connect")
public class StripeConnectController {

    private static final Logger log = LoggerFactory.getLogger(StripeConnectController.class);

    private final ClubRepository clubRepository;

    @Value("${app.mail.frontend-url}")
    private String frontendUrl;

    public StripeConnectController(ClubRepository clubRepository) {
        this.clubRepository = clubRepository;
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping("/club/{clubId}/onboard")
    public ResponseEntity<?> onboardClub(@PathVariable Long clubId) {
        Club club = clubRepository.findById(clubId).orElse(null);
        if (club == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Club introuvable."));
        }

        try {
            if (club.getStripeAccountId() == null || club.getStripeAccountId().isBlank()) {
                AccountCreateParams accountParams = AccountCreateParams.builder()
                        .setType(AccountCreateParams.Type.STANDARD)
                        .build();
                Account account = Account.create(accountParams);
                club.setStripeAccountId(account.getId());
                clubRepository.save(club);
                log.info("[STRIPE-CONNECT] compte Standard cree pour le club {} : {}", club.getName(), account.getId());
            }

            String returnUrl = frontendUrl + "/super-admin/clubs?stripeConnect=return&clubId=" + clubId;
            String refreshUrl = frontendUrl + "/super-admin/clubs?stripeConnect=refresh&clubId=" + clubId;

            AccountLinkCreateParams linkParams = AccountLinkCreateParams.builder()
                    .setAccount(club.getStripeAccountId())
                    .setType(AccountLinkCreateParams.Type.ACCOUNT_ONBOARDING)
                    .setReturnUrl(returnUrl)
                    .setRefreshUrl(refreshUrl)
                    .build();
            AccountLink accountLink = AccountLink.create(linkParams);

            return ResponseEntity.ok(Map.of("onboardingUrl", accountLink.getUrl()));
        } catch (StripeException e) {
            log.error("[STRIPE-CONNECT] erreur onboarding club {} : {}", clubId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("message", "Erreur Stripe lors de la connexion du club : " + e.getMessage()));
        }
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/club/{clubId}/status")
    public ResponseEntity<?> getStatus(@PathVariable Long clubId) {
        Club club = clubRepository.findById(clubId).orElse(null);
        if (club == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Club introuvable."));
        }
        boolean connected = club.getStripeAccountId() != null && !club.getStripeAccountId().isBlank();
        return ResponseEntity.ok(Map.of(
                "connected", connected,
                "chargesEnabled", club.isStripeChargesEnabled()
        ));
    }
}
