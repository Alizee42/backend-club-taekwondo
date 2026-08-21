package club.taekwondo.service.jpa;

import club.taekwondo.dto.InscriptionEvenementDTO;
import club.taekwondo.entity.jpa.Club;
import club.taekwondo.entity.jpa.Evenement;
import club.taekwondo.entity.jpa.InscriptionEvenement;
import club.taekwondo.entity.jpa.Membre;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.enums.Role;
import club.taekwondo.enums.StatutInscription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InscriptionEvenementServiceTest extends AbstractServiceIntegrationTest {

    @Autowired
    private InscriptionEvenementService inscriptionService;

    @Autowired
    private UtilisateurService utilisateurService;

    @Autowired
    private MembreService membreService;

    private Club club;
    private Utilisateur parent;
    private Membre enfant;
    private Evenement evenement;

    @BeforeEach
    void setupInscriptions() {
        club = new Club();
        club.setName("Club Inscription Test");
        club = clubRepository.save(club);

        parent = new Utilisateur();
        parent.setNom("Testeur");
        parent.setPrenom("Parent");
        parent.setEmail("parent-inscription@test.com");
        parent.setPassword("secret");
        parent.setRole(Role.PARENT);
        parent.setClub(club);
        parent = utilisateurService.save(parent);

        enfant = new Membre();
        enfant.setNom("Enfant");
        enfant.setPrenom("Demo");
        enfant.setParent(parent);
        enfant.setClub(club);
        enfant = membreService.save(enfant);

        evenement = new Evenement();
        evenement.setTitre("Stage");
        evenement.setDateDebut(LocalDateTime.now().plusDays(5));
        evenement.setDateFin(LocalDateTime.now().plusDays(5).plusHours(2));
        evenement.setLieu("Dojo");
        evenement.setCapacite(1);
        evenement.setActif(true);
        evenement.setClub(club);
        evenement = evenementRepository.save(evenement);
    }

    @Test
    void inscrireMembres_succes_creeUneInscriptionEnAttente() {
        List<InscriptionEvenementDTO> resultats = inscriptionService.inscrireMembres(
                evenement.getId(), List.of(enfant.getId()), "Commentaire");

        assertEquals(1, resultats.size());
        assertEquals(StatutInscription.EN_ATTENTE, resultats.get(0).getStatut());
        assertEquals(enfant.getId(), resultats.get(0).getMembreId());
    }

    @Test
    void inscrireMembres_evenementComplet_leveRuntimeException() {
        inscriptionService.inscrireMembres(evenement.getId(), List.of(enfant.getId()), null);

        Membre autreEnfant = new Membre();
        autreEnfant.setNom("Enfant2");
        autreEnfant.setPrenom("Demo2");
        autreEnfant.setParent(parent);
        autreEnfant.setClub(club);
        autreEnfant = membreService.save(autreEnfant);

        Long autreEnfantId = autreEnfant.getId();
        assertThrows(RuntimeException.class,
                () -> inscriptionService.inscrireMembres(evenement.getId(), List.of(autreEnfantId), null));
    }

    @Test
    void inscrireMembres_membreDejaInscrit_leveRuntimeException() {
        inscriptionService.inscrireMembres(evenement.getId(), List.of(enfant.getId()), null);

        // capacite=1 mais on teste le message specifique de double inscription:
        // on augmente la capacite pour isoler ce cas du cas "complet"
        evenement.setCapacite(5);
        evenementRepository.save(evenement);

        assertThrows(RuntimeException.class,
                () -> inscriptionService.inscrireMembres(evenement.getId(), List.of(enfant.getId()), null));
    }

    @Test
    void inscrireMembres_membreReinscritApresAnnulation_reactiveLaLigneExistante() {
        // La contrainte UNIQUE(membre_id, evenement_id) en base impose une seule ligne par
        // couple : inscrireMembres reutilise donc la ligne annulee au lieu d'en creer une
        // nouvelle, ce qui doit fonctionner sans violer la contrainte.
        evenement.setCapacite(5);
        evenementRepository.save(evenement);

        List<InscriptionEvenementDTO> premiere = inscriptionService.inscrireMembres(
                evenement.getId(), List.of(enfant.getId()), null);
        Long inscriptionId = premiere.get(0).getId();
        inscriptionService.annulerInscription(inscriptionId);

        List<InscriptionEvenementDTO> reinscription = inscriptionService.inscrireMembres(
                evenement.getId(), List.of(enfant.getId()), null);

        assertEquals(1, reinscription.size());
        assertEquals(inscriptionId, reinscription.get(0).getId());
        assertEquals(StatutInscription.EN_ATTENTE, reinscription.get(0).getStatut());
    }

    @Test
    void inscrireMembres_evenementIntrouvable_leveRuntimeException() {
        assertThrows(RuntimeException.class,
                () -> inscriptionService.inscrireMembres(999999L, List.of(enfant.getId()), null));
    }

    @Test
    void inscrireMembres_membreIntrouvable_leveRuntimeException() {
        assertThrows(RuntimeException.class,
                () -> inscriptionService.inscrireMembres(evenement.getId(), List.of(999999L), null));
    }

    @Test
    void annulerInscription_passeAuStatutAnnulee() {
        InscriptionEvenementDTO created = inscriptionService.inscrireMembres(
                evenement.getId(), List.of(enfant.getId()), null).get(0);

        inscriptionService.annulerInscription(created.getId());

        InscriptionEvenementDTO reloaded = inscriptionService.getInscriptionById(created.getId()).orElseThrow();
        assertEquals(StatutInscription.ANNULEE, reloaded.getStatut());
    }

    @Test
    void annulerInscription_introuvable_leveRuntimeException() {
        assertThrows(RuntimeException.class, () -> inscriptionService.annulerInscription(999999L));
    }

    @Test
    void updateStatutInscription_statutValide_estApplique() {
        InscriptionEvenementDTO created = inscriptionService.inscrireMembres(
                evenement.getId(), List.of(enfant.getId()), null).get(0);

        inscriptionService.updateStatutInscription(created.getId(), "VALIDEE");

        InscriptionEvenementDTO reloaded = inscriptionService.getInscriptionById(created.getId()).orElseThrow();
        assertEquals(StatutInscription.VALIDEE, reloaded.getStatut());
    }

    @Test
    void updateStatutInscription_statutInvalide_leveRuntimeException() {
        InscriptionEvenementDTO created = inscriptionService.inscrireMembres(
                evenement.getId(), List.of(enfant.getId()), null).get(0);
        Long id = created.getId();

        assertThrows(RuntimeException.class, () -> inscriptionService.updateStatutInscription(id, "INCONNU"));
    }

    @Test
    void getInscriptionsByEvenementAndStatut_filtreParStatut() {
        InscriptionEvenementDTO created = inscriptionService.inscrireMembres(
                evenement.getId(), List.of(enfant.getId()), null).get(0);
        inscriptionService.updateStatutInscription(created.getId(), "VALIDEE");

        List<InscriptionEvenementDTO> validees =
                inscriptionService.getInscriptionsByEvenementAndStatut(evenement.getId(), "VALIDEE");
        List<InscriptionEvenementDTO> enAttente =
                inscriptionService.getInscriptionsByEvenementAndStatut(evenement.getId(), "EN_ATTENTE");

        assertEquals(1, validees.size());
        assertTrue(enAttente.isEmpty());
    }

    @Test
    void getInscriptionsByParent_retourneLesInscriptionsDesEnfants() {
        inscriptionService.inscrireMembres(evenement.getId(), List.of(enfant.getId()), null);

        List<InscriptionEvenementDTO> inscriptions = inscriptionService.getInscriptionsByParent(parent.getId());

        assertEquals(1, inscriptions.size());
    }

    @Test
    void getInscriptionsByClubId_neRetourneQueLesInscriptionsDuClub() {
        inscriptionService.inscrireMembres(evenement.getId(), List.of(enfant.getId()), null);

        List<InscriptionEvenementDTO> inscriptions = inscriptionService.getInscriptionsByClubId(club.getId());

        assertEquals(1, inscriptions.size());
    }

    @Test
    void convertToDTO_membreSansCompteUtilisateur_utiliseEmailDuParentAvecSuffixe() {
        InscriptionEvenementDTO created = inscriptionService.inscrireMembres(
                evenement.getId(), List.of(enfant.getId()), null).get(0);

        assertTrue(created.getMembreEmail().endsWith("(parent)"));
    }

    @Test
    void convertToDTO_membreAvecCompteUtilisateurDirect_utiliseSonEmail() {
        Utilisateur compte = new Utilisateur();
        compte.setNom("Adulte");
        compte.setPrenom("Pratiquant");
        compte.setEmail("adulte-pratiquant@test.com");
        compte.setPassword("secret");
        compte.setRole(Role.MEMBRE);
        compte.setClub(club);
        compte = utilisateurService.save(compte);

        Membre adulte = new Membre();
        adulte.setNom("Adulte");
        adulte.setPrenom("Pratiquant");
        adulte.setEstAdulte(true);
        adulte.setCompteUtilisateur(compte);
        adulte.setClub(club);
        adulte = membreService.save(adulte);

        InscriptionEvenementDTO created = inscriptionService.inscrireMembres(
                evenement.getId(), List.of(adulte.getId()), null).get(0);

        assertEquals("adulte-pratiquant@test.com", created.getMembreEmail());
    }

    @Test
    void getAllInscriptions_retourneToutesLesInscriptions() {
        inscriptionService.inscrireMembres(evenement.getId(), List.of(enfant.getId()), null);

        List<InscriptionEvenementDTO> all = inscriptionService.getAllInscriptions();

        assertEquals(1, all.size());
    }

    @Test
    void getInscriptionsByEvenementAndStatut_sansStatut_retourneToutes() {
        inscriptionService.inscrireMembres(evenement.getId(), List.of(enfant.getId()), null);

        List<InscriptionEvenementDTO> toutes =
                inscriptionService.getInscriptionsByEvenementAndStatut(evenement.getId(), null);

        assertEquals(1, toutes.size());
    }

    @Test
    void getInscriptionsByMembreId_retourneLesInscriptionsActivesDuMembre() {
        InscriptionEvenementDTO created = inscriptionService.inscrireMembres(
                evenement.getId(), List.of(enfant.getId()), null).get(0);

        List<InscriptionEvenementDTO> inscriptions = inscriptionService.getInscriptionsByMembreId(enfant.getId());

        assertEquals(1, inscriptions.size());
        assertEquals(created.getId(), inscriptions.get(0).getId());
    }

    @Test
    void getInscriptionsByMembreId_neRetournePasLesAnnulees() {
        InscriptionEvenementDTO created = inscriptionService.inscrireMembres(
                evenement.getId(), List.of(enfant.getId()), null).get(0);
        inscriptionService.annulerInscription(created.getId());

        List<InscriptionEvenementDTO> inscriptions = inscriptionService.getInscriptionsByMembreId(enfant.getId());

        assertTrue(inscriptions.isEmpty());
    }

    @Test
    void updateInscription_succes_metAJourLesChamps() {
        InscriptionEvenementDTO created = inscriptionService.inscrireMembres(
                evenement.getId(), List.of(enfant.getId()), null).get(0);

        InscriptionEvenementDTO update = new InscriptionEvenementDTO();
        update.setMembreId(enfant.getId());
        update.setEvenementId(evenement.getId());
        update.setStatut(StatutInscription.VALIDEE);
        update.setCommentaire("Commentaire modifie");
        update.setPresence(true);

        InscriptionEvenementDTO updated = inscriptionService.updateInscription(created.getId(), update);

        assertEquals(StatutInscription.VALIDEE, updated.getStatut());
        assertEquals("Commentaire modifie", updated.getCommentaire());
        assertEquals(Boolean.TRUE, updated.getPresence());
    }

    @Test
    void updateInscription_introuvable_leveRuntimeException() {
        InscriptionEvenementDTO dto = new InscriptionEvenementDTO();
        dto.setMembreId(enfant.getId());
        dto.setEvenementId(evenement.getId());

        assertThrows(RuntimeException.class, () -> inscriptionService.updateInscription(999999L, dto));
    }
}
