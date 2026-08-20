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
