package club.taekwondo.controller.jpa;

import club.taekwondo.dto.CommandeDTO;
import club.taekwondo.entity.jpa.Club;
import club.taekwondo.entity.jpa.Commande;
import club.taekwondo.entity.jpa.LigneCommande;
import club.taekwondo.entity.jpa.Membre;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.service.jpa.CommandeService;
import club.taekwondo.service.jpa.UtilisateurService;
import club.taekwondo.repository.jpa.MembreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommandeControllerTest {

    @Mock
    private CommandeService commandeService;

    @Mock
    private UtilisateurService utilisateurService;

    @Mock
    private MembreRepository membreRepository;

    private CommandeController controller;

    @BeforeEach
    void setUp() {
        controller = new CommandeController(commandeService, utilisateurService, membreRepository);
    }

    @Test
    void getAllCommandes_adminUsesOwnClubScope() {
        Utilisateur admin = user(10L, "admin@club.test", 1L);
        Authentication auth = auth("admin@club.test", "ROLE_ADMIN");
        CommandeDTO dto = new CommandeDTO();
        dto.setId(100L);

        when(utilisateurService.findByEmail("admin@club.test")).thenReturn(Optional.of(admin));
        when(commandeService.getAllCommandesByClubId(1L)).thenReturn(List.of(dto));

        ResponseEntity<List<CommandeDTO>> response = controller.getAllCommandes(auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(100L, response.getBody().get(0).getId());
        verify(commandeService).getAllCommandesByClubId(1L);
        verify(commandeService, never()).getAllCommandes();
    }

    @Test
    void getCommandesParParent_parentCannotReadAnotherFamily() {
        Utilisateur callerParent = user(11L, "parent1@test.com", 1L);
        Utilisateur targetParent = user(22L, "parent2@test.com", 1L);
        Authentication auth = auth("parent1@test.com", "ROLE_PARENT");

        when(utilisateurService.findByEmail("parent1@test.com")).thenReturn(Optional.of(callerParent));
        when(utilisateurService.getUtilisateurEntityById(22L)).thenReturn(Optional.of(targetParent));

        ResponseEntity<List<CommandeDTO>> response = controller.getCommandesParParent(22L, auth);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(commandeService, never()).getCommandesParParent(22L);
    }

    @Test
    void getCommandeById_memberCannotAccessAnotherBeneficiaryCommande() {
        Utilisateur caller = user(30L, "membre@test.com", 1L);
        Utilisateur otherUser = user(31L, "autre@test.com", 1L);
        Membre otherMember = member(301L, otherUser, null, 1L);
        LigneCommande ligne = new LigneCommande();
        ligne.setBeneficiaire(otherMember);

        Commande commande = new Commande();
        commande.setId(77L);
        commande.setUtilisateur(otherUser);
        commande.setLignes(List.of(ligne));

        Authentication auth = auth("membre@test.com", "ROLE_MEMBRE");

        when(commandeService.getCommandeEntityById(77L)).thenReturn(Optional.of(commande));
        when(utilisateurService.findByEmail("membre@test.com")).thenReturn(Optional.of(caller));

        ResponseEntity<CommandeDTO> response = controller.getCommandeById(77L, auth);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(commandeService, never()).toCommandeDTO(commande);
    }

    @Test
    void createCommande_adminCannotCreateForUserFromAnotherClub() {
        Utilisateur admin = user(40L, "admin@test.com", 1L);
        Utilisateur targetUser = user(41L, "target@test.com", 2L);
        Authentication auth = auth("admin@test.com", "ROLE_ADMIN");

        CommandeDTO dto = new CommandeDTO();
        dto.setUtilisateurId(41L);
        dto.setClubId(2L);

        when(utilisateurService.getUtilisateurEntityById(41L)).thenReturn(Optional.of(targetUser));
        when(utilisateurService.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));

        ResponseEntity<CommandeDTO> response = controller.createCommande(dto, auth);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(commandeService, never()).createCommande(dto);
    }

    @Test
    void validerCommande_adminCannotValidateOtherClubCommande() {
        Utilisateur admin = user(1L, "admin@test.com", 1L);
        Commande commande = commandeFor(otherClubUser(2L));
        Authentication auth = auth("admin@test.com", "ROLE_ADMIN");

        when(commandeService.getCommandeEntityById(50L)).thenReturn(Optional.of(commande));
        when(utilisateurService.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));

        ResponseEntity<CommandeDTO> response = controller.validerCommande(50L, java.util.Map.of("modePaiement", "cheque"), auth);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(commandeService, never()).validerCommande(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void validerCommande_missingModePaiement_returnsBadRequest() {
        Utilisateur admin = user(1L, "admin@test.com", 1L);
        Commande commande = commandeFor(user(2L, "client@test.com", 1L));
        Authentication auth = auth("admin@test.com", "ROLE_ADMIN");

        when(commandeService.getCommandeEntityById(50L)).thenReturn(Optional.of(commande));
        when(utilisateurService.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));

        ResponseEntity<CommandeDTO> response = controller.validerCommande(50L, java.util.Map.of(), auth);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void validerCommande_ownerCanValidate() {
        Utilisateur admin = user(1L, "admin@test.com", 1L);
        Commande commande = commandeFor(user(2L, "client@test.com", 1L));
        Authentication auth = auth("admin@test.com", "ROLE_ADMIN");
        CommandeDTO result = new CommandeDTO();
        result.setId(50L);

        when(commandeService.getCommandeEntityById(50L)).thenReturn(Optional.of(commande));
        when(utilisateurService.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(commandeService.validerCommande(50L, "cheque")).thenReturn(result);

        ResponseEntity<CommandeDTO> response = controller.validerCommande(50L, java.util.Map.of("modePaiement", "cheque"), auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void annulerCommande_ownerCanCancelWithDefaultMotif() {
        Utilisateur admin = user(1L, "admin@test.com", 1L);
        Commande commande = commandeFor(user(2L, "client@test.com", 1L));
        Authentication auth = auth("admin@test.com", "ROLE_ADMIN");
        CommandeDTO result = new CommandeDTO();
        result.setId(50L);

        when(commandeService.getCommandeEntityById(50L)).thenReturn(Optional.of(commande));
        when(utilisateurService.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(commandeService.annulerCommande(50L, "Motif non renseigne")).thenReturn(result);

        ResponseEntity<CommandeDTO> response = controller.annulerCommande(50L, java.util.Map.of(), auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(commandeService).annulerCommande(50L, "Motif non renseigne");
    }

    @Test
    void marquerCommandeARetirer_adminOfOtherClubIsForbidden() {
        Utilisateur admin = user(1L, "admin@test.com", 1L);
        Commande commande = commandeFor(otherClubUser(2L));
        Authentication auth = auth("admin@test.com", "ROLE_ADMIN");

        when(commandeService.getCommandeEntityById(50L)).thenReturn(Optional.of(commande));
        when(utilisateurService.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));

        ResponseEntity<CommandeDTO> response = controller.marquerCommandeARetirer(50L, auth);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(commandeService, never()).marquerCommandeARetirer(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void deleteCommande_superAdminCanDeleteAnyClub() {
        Utilisateur superAdmin = user(1L, "super@test.com", 9L);
        Commande commande = commandeFor(user(2L, "client@test.com", 1L));
        Authentication auth = auth("super@test.com", "ROLE_SUPER_ADMIN");

        when(commandeService.getCommandeEntityById(50L)).thenReturn(Optional.of(commande));
        when(utilisateurService.findByEmail("super@test.com")).thenReturn(Optional.of(superAdmin));

        ResponseEntity<Void> response = controller.deleteCommande(50L, auth);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(commandeService).deleteCommande(50L);
    }

    @Test
    void deleteCommande_notFound_returns404() {
        when(commandeService.getCommandeEntityById(999L)).thenReturn(Optional.empty());
        Authentication auth = auth("admin@test.com", "ROLE_ADMIN");

        ResponseEntity<Void> response = controller.deleteCommande(999L, auth);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void passerCommandeDepuisPanier_withoutClub_returnsForbidden() {
        Utilisateur caller = user(1L, "membre@test.com", null);
        Authentication auth = auth("membre@test.com", "ROLE_MEMBRE");
        when(utilisateurService.findByEmail("membre@test.com")).thenReturn(Optional.of(caller));

        ResponseEntity<?> response = controller.passerCommandeDepuisPanier(
                new club.taekwondo.dto.CartCheckoutRequestDTO(), auth);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(commandeService, never()).createCommandeFromCart(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void getCommandesParMembre_notFound_returns404() {
        when(membreRepository.findById(404L)).thenReturn(Optional.empty());
        Authentication auth = auth("admin@test.com", "ROLE_ADMIN");

        ResponseEntity<List<CommandeDTO>> response = controller.getCommandesParMembre(404L, auth);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    private Commande commandeFor(Utilisateur owner) {
        Commande commande = new Commande();
        commande.setId(50L);
        commande.setUtilisateur(owner);
        return commande;
    }

    private Utilisateur otherClubUser(Long clubId) {
        return user(2L, "client@other.com", clubId);
    }

    private Authentication auth(String email, String authority) {
        TestingAuthenticationToken token = new TestingAuthenticationToken(email, null, authority);
        token.setAuthenticated(true);
        return token;
    }

    private Utilisateur user(Long id, String email, Long clubId) {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(id);
        utilisateur.setEmail(email);
        utilisateur.setClub(club(clubId));
        return utilisateur;
    }

    private Membre member(Long id, Utilisateur compte, Utilisateur parent, Long clubId) {
        Membre membre = new Membre();
        membre.setId(id);
        membre.setCompteUtilisateur(compte);
        membre.setParent(parent);
        membre.setClub(club(clubId));
        membre.setNom("Nom");
        membre.setPrenom("Prenom");
        return membre;
    }

    private Club club(Long id) {
        Club club = new Club();
        club.setId(id);
        club.setName("Club " + id);
        return club;
    }
}
