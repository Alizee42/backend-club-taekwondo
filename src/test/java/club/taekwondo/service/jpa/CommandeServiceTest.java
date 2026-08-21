package club.taekwondo.service.jpa;

import club.taekwondo.dto.CartCheckoutRequestDTO;
import club.taekwondo.dto.CommandeDTO;
import club.taekwondo.dto.CommandeUpdateDTO;
import club.taekwondo.dto.LigneCommandeDTO;
import club.taekwondo.entity.jpa.Club;
import club.taekwondo.entity.jpa.Membre;
import club.taekwondo.entity.jpa.Produit;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.enums.Role;
import club.taekwondo.repository.jpa.ClubRepository;
import club.taekwondo.repository.jpa.CommandeRepository;
import club.taekwondo.repository.jpa.LigneCommandeRepository;
import club.taekwondo.repository.jpa.MembreRepository;
import club.taekwondo.repository.jpa.NotificationRepository;
import club.taekwondo.repository.jpa.PaiementRepository;
import club.taekwondo.repository.jpa.ProduitRepository;
import club.taekwondo.repository.jpa.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class CommandeServiceTest {

    @MockBean
    private ActualiteService actualiteService;

    @MockBean
    private GalerieService galerieService;

    @Autowired
    private CommandeService commandeService;

    @Autowired
    private CommandeRepository commandeRepository;

    @Autowired
    private LigneCommandeRepository ligneCommandeRepository;

    @Autowired
    private ProduitRepository produitRepository;

    @Autowired
    private MembreRepository membreRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private PaiementRepository paiementRepository;

    @Autowired
    private UtilisateurService utilisateurService;

    @Autowired
    private MembreService membreService;

    private Utilisateur parent;
    private Membre enfant;
    private Produit produit;
    private Club club;

    @BeforeEach
    void setup() {
        ligneCommandeRepository.deleteAll();
        paiementRepository.deleteAll();
        commandeRepository.deleteAll();
        produitRepository.deleteAll();
        membreRepository.deleteAll();
        notificationRepository.deleteAll();
        utilisateurRepository.deleteAll();
        clubRepository.deleteAll();

        club = new Club();
        club.setName("Club Boutique");
        club = clubRepository.save(club);

        parent = new Utilisateur();
        parent.setNom("Testeur");
        parent.setPrenom("Parent");
        parent.setEmail("parent-commande@test.com");
        parent.setPassword("secret");
        parent.setRole(Role.PARENT);
        parent.setClub(club);
        parent = utilisateurService.save(parent);

        enfant = new Membre();
        enfant.setNom("Enfant");
        enfant.setPrenom("Demo");
        enfant.setParent(parent);
        enfant = membreService.save(enfant);

        produit = new Produit();
        produit.setNom("Kimono");
        produit.setPrix(BigDecimal.valueOf(40.0));
        produit.setStock(10);
        produit = produitRepository.save(produit);
    }

    private LigneCommandeDTO ligne(Long produitId, int quantite) {
        LigneCommandeDTO ligne = new LigneCommandeDTO();
        ligne.setProduitId(produitId);
        ligne.setQuantite(quantite);
        return ligne;
    }

    @Test
    void createCommandeWithLignes_modeCB_estAutoValideeEtTotalCalcule() {
        CommandeDTO dto = new CommandeDTO();
        dto.setUtilisateurId(parent.getId());
        dto.setClubId(club.getId());
        dto.setModePaiement("cb");
        dto.setLignesCommande(List.of(ligne(produit.getId(), 2)));

        CommandeDTO created = commandeService.createCommandeWithLignes(dto);

        assertEquals("CB", created.getModePaiement());
        assertEquals("PAYEE", created.getStatut());
        assertNotNull(created.getDatePaiement());
        assertEquals(0, BigDecimal.valueOf(80.0).compareTo(created.getMontantTotal()));
        assertEquals(1, created.getLignesCommande().size());
    }

    @Test
    void createCommandeWithLignes_modeEspeces_resteEnAttenteSansDatePaiement() {
        CommandeDTO dto = new CommandeDTO();
        dto.setUtilisateurId(parent.getId());
        dto.setClubId(club.getId());
        dto.setModePaiement("especes");
        dto.setLignesCommande(List.of(ligne(produit.getId(), 1)));

        CommandeDTO created = commandeService.createCommandeWithLignes(dto);

        assertEquals("ESPECES", created.getModePaiement());
        assertEquals("EN_ATTENTE", created.getStatut());
        assertNull(created.getDatePaiement());
    }

    @Test
    void createCommandeWithLignes_flocageAjouteLeCoutSupplementaire() {
        LigneCommandeDTO ligneDTO = ligne(produit.getId(), 1);
        ligneDTO.setFlocage("Nom Prenom");

        CommandeDTO dto = new CommandeDTO();
        dto.setUtilisateurId(parent.getId());
        dto.setClubId(club.getId());
        dto.setModePaiement("especes");
        dto.setLignesCommande(List.of(ligneDTO));

        CommandeDTO created = commandeService.createCommandeWithLignes(dto);

        // 40€ produit + 10€ flocage = 50€
        assertEquals(0, BigDecimal.valueOf(50.0).compareTo(created.getMontantTotal()));
        assertEquals(0, BigDecimal.valueOf(50.0).compareTo(BigDecimal.valueOf(created.getLignesCommande().get(0).getSousTotal())));
    }

    @Test
    void createCommandeWithLignes_prixUnitaireFourni_neRecalculPasLeFlocage() {
        LigneCommandeDTO ligneDTO = ligne(produit.getId(), 1);
        ligneDTO.setFlocage("Nom Prenom");
        ligneDTO.setPrixUnitaire(45.0); // deja calcule cote appelant

        CommandeDTO dto = new CommandeDTO();
        dto.setUtilisateurId(parent.getId());
        dto.setClubId(club.getId());
        dto.setModePaiement("especes");
        dto.setLignesCommande(List.of(ligneDTO));

        CommandeDTO created = commandeService.createCommandeWithLignes(dto);

        assertEquals(0, BigDecimal.valueOf(45.0).compareTo(created.getMontantTotal()));
    }

    @Test
    void createCommandeWithLignes_beneficiaireExplicite_estUtilise() {
        LigneCommandeDTO ligneDTO = ligne(produit.getId(), 1);
        ligneDTO.setBeneficiaireId(enfant.getId());

        CommandeDTO dto = new CommandeDTO();
        dto.setUtilisateurId(parent.getId());
        dto.setClubId(club.getId());
        dto.setModePaiement("especes");
        dto.setLignesCommande(List.of(ligneDTO));

        CommandeDTO created = commandeService.createCommandeWithLignes(dto);

        assertEquals(enfant.getId(), created.getLignesCommande().get(0).getBeneficiaireId());
    }

    @Test
    void createCommandeWithLignes_parentAvecUnSeulEnfant_beneficiaireParDefaut() {
        CommandeDTO dto = new CommandeDTO();
        dto.setUtilisateurId(parent.getId());
        dto.setClubId(club.getId());
        dto.setModePaiement("especes");
        dto.setLignesCommande(List.of(ligne(produit.getId(), 1)));

        CommandeDTO created = commandeService.createCommandeWithLignes(dto);

        assertEquals(enfant.getId(), created.getLignesCommande().get(0).getBeneficiaireId());
    }

    @Test
    void createCommandeWithLignes_parentAvecPlusieursEnfants_pasDeBeneficiaireParDefaut() {
        Membre autreEnfant = new Membre();
        autreEnfant.setNom("Enfant2");
        autreEnfant.setPrenom("Demo2");
        autreEnfant.setParent(parent);
        membreService.save(autreEnfant);

        CommandeDTO dto = new CommandeDTO();
        dto.setUtilisateurId(parent.getId());
        dto.setClubId(club.getId());
        dto.setModePaiement("especes");
        dto.setLignesCommande(List.of(ligne(produit.getId(), 1)));

        CommandeDTO created = commandeService.createCommandeWithLignes(dto);

        assertNull(created.getLignesCommande().get(0).getBeneficiaireId());
    }

    @Test
    void createCommandeWithLignes_produitIntrouvable_leveRuntimeException() {
        CommandeDTO dto = new CommandeDTO();
        dto.setUtilisateurId(parent.getId());
        dto.setClubId(club.getId());
        dto.setModePaiement("especes");
        dto.setLignesCommande(List.of(ligne(999999L, 1)));

        assertThrows(RuntimeException.class, () -> commandeService.createCommandeWithLignes(dto));
    }

    @Test
    void createCommandeFromCart_panierVide_leveIllegalArgumentException() {
        CartCheckoutRequestDTO req = new CartCheckoutRequestDTO();
        req.setModePaiement("especes");
        req.setLignes(List.of());

        assertThrows(IllegalArgumentException.class, () -> commandeService.createCommandeFromCart(req, parent));
    }

    @Test
    void createCommandeFromCart_creeCommandeAvecClubDeLUtilisateur() {
        CartCheckoutRequestDTO req = new CartCheckoutRequestDTO();
        req.setModePaiement("cb");
        req.setLignes(List.of(ligne(produit.getId(), 3)));

        CommandeDTO created = commandeService.createCommandeFromCart(req, parent);

        assertEquals(club.getId(), created.getClubId());
        assertEquals("PAYEE", created.getStatut());
        assertEquals(0, BigDecimal.valueOf(120.0).compareTo(created.getMontantTotal()));
    }

    @Test
    void validerCommande_passeAuStatutPayeeEtFixeLaDate() {
        CommandeDTO dto = new CommandeDTO();
        dto.setUtilisateurId(parent.getId());
        dto.setClubId(club.getId());
        dto.setModePaiement("especes");
        dto.setLignesCommande(List.of(ligne(produit.getId(), 1)));
        CommandeDTO created = commandeService.createCommandeWithLignes(dto);

        CommandeDTO validee = commandeService.validerCommande(created.getId(), "virement");

        assertEquals("PAYEE", validee.getStatut());
        assertEquals("VIREMENT", validee.getModePaiement());
        assertNotNull(validee.getDatePaiement());
    }

    @Test
    void validerCommande_introuvable_leveRuntimeException() {
        assertThrows(RuntimeException.class, () -> commandeService.validerCommande(999999L, "especes"));
    }

    @Test
    void annulerCommande_passeAuStatutAnnuleeEtEffaceLaDatePaiement() {
        CommandeDTO dto = new CommandeDTO();
        dto.setUtilisateurId(parent.getId());
        dto.setClubId(club.getId());
        dto.setModePaiement("cb");
        dto.setLignesCommande(List.of(ligne(produit.getId(), 1)));
        CommandeDTO created = commandeService.createCommandeWithLignes(dto);

        CommandeDTO annulee = commandeService.annulerCommande(created.getId(), "Rupture de stock");

        assertEquals("ANNULEE", annulee.getStatut());
        assertNull(annulee.getDatePaiement());
    }

    @Test
    void marquerCommandeARetirer_passeAuStatutARetirer() {
        CommandeDTO dto = new CommandeDTO();
        dto.setUtilisateurId(parent.getId());
        dto.setClubId(club.getId());
        dto.setModePaiement("especes");
        dto.setLignesCommande(List.of(ligne(produit.getId(), 1)));
        CommandeDTO created = commandeService.createCommandeWithLignes(dto);

        CommandeDTO retrait = commandeService.marquerCommandeARetirer(created.getId());

        assertEquals("A_RETIRER", retrait.getStatut());
    }

    @Test
    void deleteCommande_inexistante_leveIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> commandeService.deleteCommande(999999L));
    }

    @Test
    void deleteCommande_existante_laSupprime() {
        CommandeDTO dto = new CommandeDTO();
        dto.setUtilisateurId(parent.getId());
        dto.setClubId(club.getId());
        dto.setModePaiement("especes");
        dto.setLignesCommande(List.of(ligne(produit.getId(), 1)));
        CommandeDTO created = commandeService.createCommandeWithLignes(dto);

        commandeService.deleteCommande(created.getId());

        assertTrue(commandeService.getCommandeById(created.getId()).isEmpty());
    }

    @Test
    void mettreAJourCommande_appliqueLesChampsFournis() {
        CommandeDTO dto = new CommandeDTO();
        dto.setUtilisateurId(parent.getId());
        dto.setClubId(club.getId());
        dto.setModePaiement("especes");
        dto.setLignesCommande(List.of(ligne(produit.getId(), 1)));
        CommandeDTO created = commandeService.createCommandeWithLignes(dto);

        CommandeUpdateDTO update = new CommandeUpdateDTO();
        update.setStatut("payee");
        commandeService.mettreAJourCommande(created.getId(), update);

        CommandeDTO reloaded = commandeService.getCommandeById(created.getId()).orElseThrow();
        assertEquals("PAYEE", reloaded.getStatut());
    }

    @Test
    void getCommandesPaiementClubByClubId_filtreParStatutEtModePaiement() {
        CommandeDTO especes = new CommandeDTO();
        especes.setUtilisateurId(parent.getId());
        especes.setClubId(club.getId());
        especes.setModePaiement("especes");
        especes.setLignesCommande(List.of(ligne(produit.getId(), 1)));
        commandeService.createCommandeWithLignes(especes);

        CommandeDTO cb = new CommandeDTO();
        cb.setUtilisateurId(parent.getId());
        cb.setClubId(club.getId());
        cb.setModePaiement("cb");
        cb.setLignesCommande(List.of(ligne(produit.getId(), 1)));
        commandeService.createCommandeWithLignes(cb);

        List<CommandeDTO> aTraiter = commandeService.getCommandesPaiementClubByClubId(club.getId());

        assertEquals(1, aTraiter.size());
        assertEquals("ESPECES", aTraiter.get(0).getModePaiement());
    }

    @Test
    void getCommandesParMembre_fusionneCommandesDirectesEtCommandesDuCompteUtilisateur() {
        Utilisateur compteMembre = new Utilisateur();
        compteMembre.setNom("Membre");
        compteMembre.setPrenom("Compte");
        compteMembre.setEmail("membre-compte@test.com");
        compteMembre.setPassword("secret");
        compteMembre.setRole(Role.MEMBRE);
        compteMembre.setClub(club);
        compteMembre = utilisateurService.save(compteMembre);

        Membre membreAvecCompte = new Membre();
        membreAvecCompte.setNom("Avec");
        membreAvecCompte.setPrenom("Compte");
        membreAvecCompte.setCompteUtilisateur(compteMembre);
        membreAvecCompte = membreService.save(membreAvecCompte);

        CommandeDTO viaCompte = new CommandeDTO();
        viaCompte.setUtilisateurId(compteMembre.getId());
        viaCompte.setClubId(club.getId());
        viaCompte.setModePaiement("especes");
        viaCompte.setLignesCommande(List.of(ligne(produit.getId(), 1)));
        commandeService.createCommandeWithLignes(viaCompte);

        List<CommandeDTO> commandes = commandeService.getCommandesParMembre(membreAvecCompte.getId());

        assertEquals(1, commandes.size());
    }
}
