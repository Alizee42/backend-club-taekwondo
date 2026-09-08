package club.taekwondo.config;

import club.taekwondo.entity.jpa.Echeance;
import club.taekwondo.entity.jpa.Membre;
import club.taekwondo.entity.jpa.Paiement;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.repository.jpa.MembreRepository;
import club.taekwondo.repository.jpa.PaiementRepository;
import club.taekwondo.repository.jpa.UtilisateurRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Quelques paiements de démo sur le club Villeurbanne (parent.villeurbanne),
 * pour visualiser en local le suivi/tracabilite/relance/vue caisse : un payé,
 * un en attente, un en retard (unique), un échelonné avec échéance en retard,
 * un annulé. Idempotent (basé sur la présence d'au moins un paiement pour ce
 * parent). Désactiver avec TEST_DATA_SEED=false (même flag que TestDataBootstrap).
 * Order(60) : après TestDataBootstrap(50) qui crée le parent et ses enfants.
 */
@Configuration
public class PaiementDemoBootstrap {

    private static final Logger log = LoggerFactory.getLogger(PaiementDemoBootstrap.class);

    @Value("${test.data.seed:true}")
    private boolean seedEnabled;

    @Bean
    @Order(60)
    CommandLineRunner seedPaiementsDemo(UtilisateurRepository utilisateurRepo,
                                        MembreRepository membreRepo,
                                        PaiementRepository paiementRepo) {
        return args -> {
            if (!seedEnabled) {
                return;
            }

            Optional<Utilisateur> parentOpt = utilisateurRepo.findByEmailIgnoreCase("parent.villeurbanne@club-taekwondo.com");
            if (parentOpt.isEmpty()) {
                log.info("[PaiementDemoBootstrap] parent de démo introuvable, seed paiements ignoré.");
                return;
            }
            Utilisateur parent = parentOpt.get();

            if (!paiementRepo.findByUtilisateurId(parent.getId()).isEmpty()) {
                log.info("[PaiementDemoBootstrap] paiements de démo déjà présents, seed ignoré.");
                return;
            }

            List<Membre> enfants = membreRepo.findByParentId(parent.getId());
            if (enfants.isEmpty()) {
                log.info("[PaiementDemoBootstrap] aucun enfant pour le parent de démo, seed paiements ignoré.");
                return;
            }
            Membre lucas = enfants.get(0);
            Membre emma = enfants.size() > 1 ? enfants.get(1) : enfants.get(0);

            LocalDate today = LocalDate.now();

            // 1) Cotisation payée en CB (mois dernier) — apparait dans la vue caisse.
            Paiement paye = new Paiement();
            paye.setType("UNIQUE");
            paye.setUtilisateur(parent);
            paye.setMembre(lucas);
            paye.setDatePaiement(today.minusDays(20));
            paye.setModePaiement("CB");
            paye.setMontantTotal(120.0);
            paye.setMontantPaye(120.0);
            paye.setMontantRestant(0.0);
            paye.setStatut("payé");
            paiementRepo.save(paye);

            // 2) Cotisation en attente, pas encore échue — rien à faire, juste visible.
            Paiement enAttente = new Paiement();
            enAttente.setType("UNIQUE");
            enAttente.setUtilisateur(parent);
            enAttente.setMembre(emma);
            enAttente.setDatePaiement(today.plusDays(10));
            enAttente.setModePaiement("VIREMENT");
            enAttente.setMontantTotal(120.0);
            enAttente.setMontantPaye(0.0);
            enAttente.setMontantRestant(120.0);
            enAttente.setStatut("en attente");
            paiementRepo.save(enAttente);

            // 3) Cotisation unique en retard — doit faire apparaitre le bouton "Relancer".
            Paiement enRetard = new Paiement();
            enRetard.setType("UNIQUE");
            enRetard.setUtilisateur(parent);
            enRetard.setMembre(lucas);
            enRetard.setDatePaiement(today.minusDays(15));
            enRetard.setModePaiement("ESPECES");
            enRetard.setMontantTotal(150.0);
            enRetard.setMontantPaye(0.0);
            enRetard.setMontantRestant(150.0);
            enRetard.setStatut("en attente");
            paiementRepo.save(enRetard);

            // 4) Paiement échelonné (3 échéances), une payée, une en retard, une future.
            Paiement echelonne = new Paiement();
            echelonne.setType("ECHELONNE");
            echelonne.setUtilisateur(parent);
            echelonne.setMembre(emma);
            echelonne.setDatePaiement(today.minusDays(40));
            echelonne.setModePaiement("CB");
            echelonne.setMontantTotal(180.0);

            List<Echeance> echeances = new ArrayList<>();
            Echeance e1 = new Echeance();
            e1.setNumero(1);
            e1.setDateEcheance(today.minusDays(40));
            e1.setMontant(60.0);
            e1.setStatut("payé");
            e1.setModePaiement("CB");
            e1.setDatePaiementReel(today.minusDays(40));
            e1.setPaiement(echelonne);
            echeances.add(e1);

            Echeance e2 = new Echeance();
            e2.setNumero(2);
            e2.setDateEcheance(today.minusDays(10));
            e2.setMontant(60.0);
            e2.setStatut("en attente");
            e2.setPaiement(echelonne);
            echeances.add(e2);

            Echeance e3 = new Echeance();
            e3.setNumero(3);
            e3.setDateEcheance(today.plusDays(20));
            e3.setMontant(60.0);
            e3.setStatut("en attente");
            e3.setPaiement(echelonne);
            echeances.add(e3);

            echelonne.setEcheances(echeances);
            echelonne.setMontantPaye(60.0);
            echelonne.setMontantRestant(120.0);
            echelonne.setEcheancesTotales(3);
            echelonne.setEcheancesRestantes(2);
            echelonne.setStatut("en attente");
            paiementRepo.save(echelonne);

            // 5) Cotisation annulée — vérifie l'affichage du motif/traçabilité d'annulation.
            Paiement annule = new Paiement();
            annule.setType("UNIQUE");
            annule.setUtilisateur(parent);
            annule.setMembre(lucas);
            annule.setDatePaiement(today.minusDays(5));
            annule.setModePaiement("CHEQUE");
            annule.setMontantTotal(90.0);
            annule.setMontantPaye(0.0);
            annule.setMontantRestant(0.0);
            annule.setStatut("annulé");
            annule.setMotifAnnulation("Doublon avec un autre paiement");
            annule.setDateAnnulation(today.minusDays(4).atStartOfDay());
            annule.setAdminResponsable("Sophie Martin");
            paiementRepo.save(annule);

            log.info("[PaiementDemoBootstrap] 5 paiements de démo créés pour {} (club Villeurbanne).", parent.getEmail());
        };
    }
}
