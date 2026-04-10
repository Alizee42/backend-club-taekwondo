package club.taekwondo.service.jpa;

import club.taekwondo.entity.jpa.Membre;
import club.taekwondo.entity.jpa.Paiement;
import club.taekwondo.entity.jpa.Utilisateur;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

@Service
public class PaiementAccessService {

    private final UtilisateurService utilisateurService;
    private final MembreService membreService;

    public PaiementAccessService(UtilisateurService utilisateurService, MembreService membreService) {
        this.utilisateurService = utilisateurService;
        this.membreService = membreService;
    }

    public Utilisateur requireAuthenticatedUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifie");
        }

        return utilisateurService.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non trouve"));
    }

    public boolean hasAnyRole(Authentication authentication, String... roles) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }

        for (String role : roles) {
            String authority = "ROLE_" + role;
            boolean present = authentication.getAuthorities().stream()
                    .anyMatch(grantedAuthority -> authority.equals(grantedAuthority.getAuthority()));
            if (present) {
                return true;
            }
        }
        return false;
    }

    public Long getAttachedMembreId(Utilisateur utilisateur) {
        return membreService.getMembreEntityByIdUtilisateur(utilisateur.getId())
                .map(Membre::getId)
                .orElse(null);
    }

    public List<Long> getChildMemberIds(Utilisateur parent) {
        return membreService.getEnfantsDuParent(parent.getId()).stream()
                .map(Membre::getId)
                .toList();
    }

    public void assertParentOwnsMember(Utilisateur parent, Long membreId) {
        boolean allowed = membreService.getEnfantsDuParent(parent.getId()).stream()
                .anyMatch(membre -> Objects.equals(membre.getId(), membreId));
        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ce membre ne vous est pas rattache");
        }
    }

    public void assertCanAccessPaiement(Authentication authentication, Paiement paiement) {
        if (hasAnyRole(authentication, "ADMIN", "SUPER_ADMIN")) {
            return;
        }

        Utilisateur user = requireAuthenticatedUser(authentication);
        boolean owner = false;

        if (paiement.getUtilisateur() != null && Objects.equals(paiement.getUtilisateur().getId(), user.getId())) {
            owner = true;
        }

        if (!owner) {
            Long membreId = getAttachedMembreId(user);
            owner = membreId != null
                    && paiement.getMembre() != null
                    && Objects.equals(paiement.getMembre().getId(), membreId);
        }

        if (!owner && paiement.getMembre() != null) {
            owner = membreService.getEnfantsDuParent(user.getId()).stream()
                    .anyMatch(membre -> Objects.equals(membre.getId(), paiement.getMembre().getId()));
        }

        if (!owner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acces refuse");
        }
    }
}
