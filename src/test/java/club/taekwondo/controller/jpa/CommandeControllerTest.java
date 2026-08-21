package club.taekwondo.controller.jpa;

import club.taekwondo.dto.CartCheckoutRequestDTO;
import club.taekwondo.dto.CommandeDTO;
import club.taekwondo.dto.CommandeUpdateDTO;
import club.taekwondo.entity.jpa.Club;
import club.taekwondo.entity.jpa.Commande;
import club.taekwondo.entity.jpa.LigneCommande;
import club.taekwondo.entity.jpa.Membre;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.repository.jpa.MembreRepository;
import club.taekwondo.service.jpa.CommandeService;
import club.taekwondo.service.jpa.UtilisateurService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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

    private Authentication auth(String email, String role) {
        return new TestingAuthenticationToken(email, null, "ROLE_" + role);
    }

    private Utilisateur user(Long id, Long clubId) {
        Utilisateur u = new Utilisateur();
        u.setId(id);
        if (clubId != null) {
            Club club = new Club();
            club.setId(clubId);
            u.setClub(club);
        }
        return u;
    }

    private Commande commande(Long id, Long clubId, Utilisateur utilisateur) {
        Commande c = new Commande();
        c.setId(id);
        if (clubId != null) {
            Club club = new Club();
            club.setId(clubId);
            c.setClub(club);
        }
        c.setUtilisateur(utilisateur);
        return c;
    }

    private CommandeDTO commandeDTO(Long utilisateurId, Long clubId) {
        CommandeDTO dto = new CommandeDTO();
        dto.setUtilisateurId(utilisateurId);
        dto.setClubId(clubId);
        return dto;
    }

    @Test
    void getAllCommandes_superAdmin_retourneToutes() {
        when(utilisateurService.findByEmail("super@test.com")).thenReturn(Optional.of(user(1L, null)));
        when(commandeService.getAllCommandes()).thenReturn(List.of(new CommandeDTO()));

        ResponseEntity<List<CommandeDTO>> response = controller.getAllCommandes(auth("super@test.com", "SUPER_ADMIN"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getAllCommandes_adminAvecClub_filtreParClub() {
        when(utilisateurService.findByEmail("admin@test.com")).thenReturn(Optional.of(user(1L, 10L)));
        when(commandeService.getAllCommandesByClubId(10L)).thenReturn(List.of(new CommandeDTO()));

        ResponseEntity<List<CommandeDTO>> response = controller.getAllCommandes(auth("admin@test.com", "ADMIN"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getAllCommandes_adminSansClub_retourneForbidden() {
        when(utilisateurService.findByEmail("admin@test.com")).thenReturn(Optional.of(user(1L, null)));

        ResponseEntity<List<CommandeDTO>> response = controller.getAllCommandes(auth("admin@test.com", "ADMIN"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void getCommandeById_absent_retourneNotFound() {
        when(commandeService.getCommandeEntityById(1L)).thenReturn(Optional.empty());

        ResponseEntity<CommandeDTO> response = controller.getCommandeById(1L, auth("super@test.com", "SUPER_ADMIN"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void getCommandeById_proprietaire_retourneOk() {
        Utilisateur owner = user(3L, null);
        Commande c = commande(1L, null, owner);
        when(commandeService.getCommandeEntityById(1L)).thenReturn(Optional.of(c));
        when(utilisateurService.findByEmail("membre@test.com")).thenReturn(Optional.of(user(3L, null)));
        when(commandeService.toCommandeDTO(c)).thenReturn(new CommandeDTO());

        ResponseEntity<CommandeDTO> response = controller.getCommandeById(1L, auth("membre@test.com", "MEMBRE"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getCommandeById_nonAutorise_retourneForbidden() {
        Utilisateur owner = user(3L, null);
        Commande c = commande(1L, null, owner);
        when(commandeService.getCommandeEntityById(1L)).thenReturn(Optional.of(c));
        when(utilisateurService.findByEmail("autre@test.com")).thenReturn(Optional.of(user(99L, null)));

        ResponseEntity<CommandeDTO> response = controller.getCommandeById(1L, auth("autre@test.com", "MEMBRE"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void getCommandesParMembre_membreIntrouvable_retourneNotFound() {
        when(membreRepository.findById(1L)).thenReturn(Optional.empty());

        ResponseEntity<List<CommandeDTO>> response = controller.getCommandesParMembre(1L, auth("super@test.com", "SUPER_ADMIN"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void getCommandesParMembre_parentProprietaire_retourneOk() {
        Utilisateur parent = user(5L, null);
        Membre m = new Membre();
        m.setId(1L);
        m.setParent(parent);
        when(membreRepository.findById(1L)).thenReturn(Optional.of(m));
        when(utilisateurService.findByEmail("parent@test.com")).thenReturn(Optional.of(user(5L, null)));
        when(commandeService.getCommandesParMembre(1L)).thenReturn(List.of(new CommandeDTO()));

        ResponseEntity<List<CommandeDTO>> response = controller.getCommandesParMembre(1L, auth("parent@test.com", "PARENT"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getCommandesParParent_parentIntrouvable_retourneNotFound() {
        when(utilisateurService.getUtilisateurEntityById(1L)).thenReturn(Optional.empty());

        ResponseEntity<List<CommandeDTO>> response = controller.getCommandesParParent(1L, auth("super@test.com", "SUPER_ADMIN"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void getCommandesParParent_lui_meme_retourneOk() {
        Utilisateur parent = user(5L, null);
        when(utilisateurService.getUtilisateurEntityById(5L)).thenReturn(Optional.of(parent));
        when(utilisateurService.findByEmail("parent@test.com")).thenReturn(Optional.of(parent));
        when(commandeService.getCommandesParParent(5L)).thenReturn(List.of(new CommandeDTO()));

        ResponseEntity<List<CommandeDTO>> response = controller.getCommandesParParent(5L, auth("parent@test.com", "PARENT"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void passerCommandeDepuisPanier_sansClub_retourneForbidden() {
        when(utilisateurService.findByEmail("membre@test.com")).thenReturn(Optional.of(user(3L, null)));

        ResponseEntity<?> response = controller.passerCommandeDepuisPanier(new CartCheckoutRequestDTO(), auth("membre@test.com", "MEMBRE"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void passerCommandeDepuisPanier_succes_retourneCreated() {
        when(utilisateurService.findByEmail("membre@test.com")).thenReturn(Optional.of(user(3L, 10L)));
        when(commandeService.createCommandeFromCart(any(CartCheckoutRequestDTO.class), any(Utilisateur.class)))
                .thenReturn(new CommandeDTO());

        ResponseEntity<?> response = controller.passerCommandeDepuisPanier(new CartCheckoutRequestDTO(), auth("membre@test.com", "MEMBRE"));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    void passerCommandeDepuisPanier_erreurValidation_retourneBadRequest() {
        when(utilisateurService.findByEmail("membre@test.com")).thenReturn(Optional.of(user(3L, 10L)));
        when(commandeService.createCommandeFromCart(any(CartCheckoutRequestDTO.class), any(Utilisateur.class)))
                .thenThrow(new IllegalArgumentException("panier vide"));

        ResponseEntity<?> response = controller.passerCommandeDepuisPanier(new CartCheckoutRequestDTO(), auth("membre@test.com", "MEMBRE"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void createCommande_utilisateurCibleIntrouvable_retourneBadRequest() {
        CommandeDTO dto = commandeDTO(99L, null);
        when(utilisateurService.getUtilisateurEntityById(99L)).thenReturn(Optional.empty());

        ResponseEntity<CommandeDTO> response = controller.createCommande(dto, auth("super@test.com", "SUPER_ADMIN"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void createCommande_adminMemeClub_retourneCreated() {
        Utilisateur target = user(2L, 10L);
        CommandeDTO dto = commandeDTO(2L, null);
        when(utilisateurService.getUtilisateurEntityById(2L)).thenReturn(Optional.of(target));
        when(utilisateurService.findByEmail("admin@test.com")).thenReturn(Optional.of(user(1L, 10L)));
        when(commandeService.createCommande(any(CommandeDTO.class))).thenReturn(dto);

        ResponseEntity<CommandeDTO> response = controller.createCommande(dto, auth("admin@test.com", "ADMIN"));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    void createCommande_adminAutreClub_retourneForbidden() {
        Utilisateur target = user(2L, 99L);
        CommandeDTO dto = commandeDTO(2L, null);
        when(utilisateurService.getUtilisateurEntityById(2L)).thenReturn(Optional.of(target));
        when(utilisateurService.findByEmail("admin@test.com")).thenReturn(Optional.of(user(1L, 10L)));

        ResponseEntity<CommandeDTO> response = controller.createCommande(dto, auth("admin@test.com", "ADMIN"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void createCommande_erreurService_retourneBadRequest() {
        Utilisateur target = user(2L, 10L);
        CommandeDTO dto = commandeDTO(2L, null);
        when(utilisateurService.getUtilisateurEntityById(2L)).thenReturn(Optional.of(target));
        when(utilisateurService.findByEmail("admin@test.com")).thenReturn(Optional.of(user(1L, 10L)));
        when(commandeService.createCommande(any(CommandeDTO.class))).thenThrow(new RuntimeException("erreur"));

        ResponseEntity<CommandeDTO> response = controller.createCommande(dto, auth("admin@test.com", "ADMIN"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void createCommandeAvecLignes_succes_retourneCreated() {
        Utilisateur target = user(2L, 10L);
        CommandeDTO dto = commandeDTO(2L, null);
        when(utilisateurService.getUtilisateurEntityById(2L)).thenReturn(Optional.of(target));
        when(utilisateurService.findByEmail("super@test.com")).thenReturn(Optional.of(user(1L, null)));
        when(commandeService.createCommandeWithLignes(any(CommandeDTO.class))).thenReturn(dto);

        ResponseEntity<CommandeDTO> response = controller.createCommandeAvecLignes(dto, auth("super@test.com", "SUPER_ADMIN"));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    void mettreAJourCommande_succes_retourneOk() {
        Commande c = commande(1L, 10L, null);
        when(commandeService.getCommandeEntityById(1L)).thenReturn(Optional.of(c));
        when(utilisateurService.findByEmail("admin@test.com")).thenReturn(Optional.of(user(1L, 10L)));

        ResponseEntity<Void> response = controller.mettreAJourCommande(1L, new CommandeUpdateDTO(), auth("admin@test.com", "ADMIN"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void mettreAJourCommande_absente_retourneNotFound() {
        when(commandeService.getCommandeEntityById(1L)).thenReturn(Optional.empty());

        ResponseEntity<Void> response = controller.mettreAJourCommande(1L, new CommandeUpdateDTO(), auth("admin@test.com", "ADMIN"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void mettreAJourCommande_adminAutreClub_retourneForbidden() {
        Commande c = commande(1L, 99L, null);
        when(commandeService.getCommandeEntityById(1L)).thenReturn(Optional.of(c));
        when(utilisateurService.findByEmail("admin@test.com")).thenReturn(Optional.of(user(1L, 10L)));

        ResponseEntity<Void> response = controller.mettreAJourCommande(1L, new CommandeUpdateDTO(), auth("admin@test.com", "ADMIN"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void deleteCommande_succes_retourneNoContent() {
        Commande c = commande(1L, 10L, null);
        when(commandeService.getCommandeEntityById(1L)).thenReturn(Optional.of(c));
        when(utilisateurService.findByEmail("admin@test.com")).thenReturn(Optional.of(user(1L, 10L)));

        ResponseEntity<Void> response = controller.deleteCommande(1L, auth("admin@test.com", "ADMIN"));

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void deleteCommande_erreurService_retourneNotFound() {
        Commande c = commande(1L, 10L, null);
        when(commandeService.getCommandeEntityById(1L)).thenReturn(Optional.of(c));
        when(utilisateurService.findByEmail("admin@test.com")).thenReturn(Optional.of(user(1L, 10L)));
        org.mockito.Mockito.doThrow(new RuntimeException("erreur")).when(commandeService).deleteCommande(1L);

        ResponseEntity<Void> response = controller.deleteCommande(1L, auth("admin@test.com", "ADMIN"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void getCommandesPaiementClub_superAdmin_retourneToutes() {
        when(utilisateurService.findByEmail("super@test.com")).thenReturn(Optional.of(user(1L, null)));
        when(commandeService.getCommandesPaiementClub()).thenReturn(List.of(new CommandeDTO()));

        ResponseEntity<List<CommandeDTO>> response = controller.getCommandesPaiementClub(auth("super@test.com", "SUPER_ADMIN"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getCommandesPaiementClub_adminSansClub_retourneForbidden() {
        when(utilisateurService.findByEmail("admin@test.com")).thenReturn(Optional.of(user(1L, null)));

        ResponseEntity<List<CommandeDTO>> response = controller.getCommandesPaiementClub(auth("admin@test.com", "ADMIN"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void validerCommande_sansModePaiement_retourneBadRequest() {
        Commande c = commande(1L, 10L, null);
        when(commandeService.getCommandeEntityById(1L)).thenReturn(Optional.of(c));
        when(utilisateurService.findByEmail("admin@test.com")).thenReturn(Optional.of(user(1L, 10L)));

        ResponseEntity<CommandeDTO> response = controller.validerCommande(1L, Map.of(), auth("admin@test.com", "ADMIN"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void validerCommande_succes_retourneOk() {
        Commande c = commande(1L, 10L, null);
        when(commandeService.getCommandeEntityById(1L)).thenReturn(Optional.of(c));
        when(utilisateurService.findByEmail("admin@test.com")).thenReturn(Optional.of(user(1L, 10L)));
        when(commandeService.validerCommande(1L, "cb")).thenReturn(new CommandeDTO());

        ResponseEntity<CommandeDTO> response = controller.validerCommande(1L, Map.of("modePaiement", "cb"), auth("admin@test.com", "ADMIN"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void validerCommande_erreurService_retourneBadRequest() {
        Commande c = commande(1L, 10L, null);
        when(commandeService.getCommandeEntityById(1L)).thenReturn(Optional.of(c));
        when(utilisateurService.findByEmail("admin@test.com")).thenReturn(Optional.of(user(1L, 10L)));
        when(commandeService.validerCommande(1L, "cb")).thenThrow(new RuntimeException("erreur"));

        ResponseEntity<CommandeDTO> response = controller.validerCommande(1L, Map.of("modePaiement", "cb"), auth("admin@test.com", "ADMIN"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void annulerCommande_succes_retourneOk() {
        Commande c = commande(1L, 10L, null);
        when(commandeService.getCommandeEntityById(1L)).thenReturn(Optional.of(c));
        when(utilisateurService.findByEmail("admin@test.com")).thenReturn(Optional.of(user(1L, 10L)));
        when(commandeService.annulerCommande(1L, "raison")).thenReturn(new CommandeDTO());

        ResponseEntity<CommandeDTO> response = controller.annulerCommande(1L, Map.of("motif", "raison"), auth("admin@test.com", "ADMIN"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void annulerCommande_sansMotif_utiliseValeurParDefaut() {
        Commande c = commande(1L, 10L, null);
        when(commandeService.getCommandeEntityById(1L)).thenReturn(Optional.of(c));
        when(utilisateurService.findByEmail("admin@test.com")).thenReturn(Optional.of(user(1L, 10L)));
        when(commandeService.annulerCommande(1L, "Motif non renseigne")).thenReturn(new CommandeDTO());

        ResponseEntity<CommandeDTO> response = controller.annulerCommande(1L, Map.of(), auth("admin@test.com", "ADMIN"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void marquerCommandeARetirer_succes_retourneOk() {
        Commande c = commande(1L, 10L, null);
        when(commandeService.getCommandeEntityById(1L)).thenReturn(Optional.of(c));
        when(utilisateurService.findByEmail("admin@test.com")).thenReturn(Optional.of(user(1L, 10L)));
        when(commandeService.marquerCommandeARetirer(1L)).thenReturn(new CommandeDTO());

        ResponseEntity<CommandeDTO> response = controller.marquerCommandeARetirer(1L, auth("admin@test.com", "ADMIN"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void marquerCommandeARetirer_absente_retourneNotFound() {
        when(commandeService.getCommandeEntityById(1L)).thenReturn(Optional.empty());

        ResponseEntity<CommandeDTO> response = controller.marquerCommandeARetirer(1L, auth("admin@test.com", "ADMIN"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
