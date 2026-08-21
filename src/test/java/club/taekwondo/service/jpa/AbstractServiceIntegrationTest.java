package club.taekwondo.service.jpa;

import club.taekwondo.repository.jpa.AvisRepository;
import club.taekwondo.repository.jpa.ClubRepository;
import club.taekwondo.repository.jpa.CommandeRepository;
import club.taekwondo.repository.jpa.EcheanceRepository;
import club.taekwondo.repository.jpa.EnseignantRepository;
import club.taekwondo.repository.jpa.EvenementRepository;
import club.taekwondo.repository.jpa.InscriptionEvenementRepository;
import club.taekwondo.repository.jpa.LigneCommandeRepository;
import club.taekwondo.repository.jpa.MembreRepository;
import club.taekwondo.repository.jpa.NotificationRepository;
import club.taekwondo.repository.jpa.PaiementRepository;
import club.taekwondo.repository.jpa.ProduitRepository;
import club.taekwondo.repository.jpa.ReinitialisationMotDePasseRepository;
import club.taekwondo.repository.jpa.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

/**
 * Nettoyage FK commun a tous les tests d'integration de service.jpa.
 *
 * Toutes ces classes partagent la meme base H2 en memoire sur tout le run Surefire :
 * une classe qui ne purge pas une table (ex: paiement/echeance) peut laisser des lignes
 * qui font echouer le setup d'une classe suivante n'ayant aucun lien fonctionnel avec
 * ces entites. Centraliser le nettoyage ici evite de dupliquer et d'oublier des
 * deleteAll() dans chaque nouvelle classe de test.
 */
@SpringBootTest
@ActiveProfiles("test")
abstract class AbstractServiceIntegrationTest {

    @MockBean
    protected ActualiteService actualiteService;

    @MockBean
    protected GalerieService galerieService;

    @Autowired
    protected InscriptionEvenementRepository inscriptionRepository;
    @Autowired
    protected EvenementRepository evenementRepository;
    @Autowired
    protected LigneCommandeRepository ligneCommandeRepository;
    @Autowired
    protected EnseignantRepository enseignantRepository;
    @Autowired
    protected AvisRepository avisRepository;
    @Autowired
    protected EcheanceRepository echeanceRepository;
    @Autowired
    protected PaiementRepository paiementRepository;
    @Autowired
    protected CommandeRepository commandeRepository;
    @Autowired
    protected ProduitRepository produitRepository;
    @Autowired
    protected MembreRepository membreRepository;
    @Autowired
    protected NotificationRepository notificationRepository;
    @Autowired
    protected ReinitialisationMotDePasseRepository reinitialisationMotDePasseRepository;
    @Autowired
    protected UtilisateurRepository utilisateurRepository;
    @Autowired
    protected ClubRepository clubRepository;

    @BeforeEach
    void cleanDatabase() {
        inscriptionRepository.deleteAll();
        evenementRepository.deleteAll();
        ligneCommandeRepository.deleteAll();
        enseignantRepository.deleteAll();
        avisRepository.deleteAll();
        echeanceRepository.deleteAll();
        paiementRepository.deleteAll();
        commandeRepository.deleteAll();
        produitRepository.deleteAll();
        membreRepository.deleteAll();
        notificationRepository.deleteAll();
        reinitialisationMotDePasseRepository.deleteAll();
        utilisateurRepository.deleteAll();
        clubRepository.deleteAll();
    }
}
