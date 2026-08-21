package club.taekwondo.config;

import club.taekwondo.entity.jpa.Club;
import club.taekwondo.entity.jpa.Horaire;
import club.taekwondo.repository.jpa.ClubRepository;
import club.taekwondo.repository.jpa.HoraireRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * Seed des horaires du club de Villeurbanne (source publique reelle, aout 2026)
 * et de creneaux generiques pour les 3 autres clubs, qui n'ont pas d'horaires
 * publics fiables trouves (contenu clairement fictif, a corriger via l'admin
 * quand l'info reelle sera connue).
 *
 * Idempotent : ne recree rien si le club a deja au moins un horaire.
 * Desactivable via demo.horaires.seed=false.
 */
@Configuration
public class HoraireDataBootstrap {

    private static final Logger log = LoggerFactory.getLogger(HoraireDataBootstrap.class);

    @Value("${demo.horaires.seed:true}")
    private boolean seedEnabled;

    @Bean
    @Order(11)
    CommandLineRunner seedHoraires(ClubRepository clubRepository, HoraireRepository horaireRepository) {
        return args -> {
            if (!seedEnabled) {
                log.info("[HoraireBootstrap] seed desactive (demo.horaires.seed=false).");
                return;
            }

            seedVilleurbanne(clubRepository, horaireRepository);

            seedGenerique(clubRepository, horaireRepository, "Bourg-en-Bresse",
                    "4 allee des Brotteaux", "01000", "Bourg-en-Bresse");
            seedGenerique(clubRepository, horaireRepository, "Villars-les-Dombes",
                    "18 rue de la Mantoliere", "01330", "Villars-les-Dombes");
            seedGenerique(clubRepository, horaireRepository, "Amberieu-en-Bugey",
                    "78 avenue du General Sarrail", "01500", "Amberieu-en-Bugey");
        };
    }

    private void seedVilleurbanne(ClubRepository clubRepository, HoraireRepository horaireRepository) {
        Club villeurbanne = clubRepository.findByName("Villeurbanne");
        if (villeurbanne == null) {
            log.info("[HoraireBootstrap] club Villeurbanne introuvable, seed ignore.");
            return;
        }

        if (!horaireRepository.findByClubId(villeurbanne.getId()).isEmpty()) {
            log.info("[HoraireBootstrap] horaires deja presents pour Villeurbanne, seed ignore.");
            return;
        }

        horaireRepository.save(horaire(villeurbanne, "Mardi", "18:00", "19:00", "Enfants",
                "16 rue du Progres", "69100", "Villeurbanne"));
        horaireRepository.save(horaire(villeurbanne, "Mardi", "19:00", "20:00", "Adultes",
                "16 rue du Progres", "69100", "Villeurbanne"));
        horaireRepository.save(horaire(villeurbanne, "Mercredi", "18:00", "19:00", "Enfants",
                "6 rue Berthelot", "69100", "Villeurbanne"));
        horaireRepository.save(horaire(villeurbanne, "Mercredi", "19:00", "20:00", "Adultes",
                "6 rue Berthelot", "69100", "Villeurbanne"));

        log.info("[HoraireBootstrap] 4 creneaux crees pour Villeurbanne.");
    }

    private void seedGenerique(ClubRepository clubRepository, HoraireRepository horaireRepository,
                                String nomClub, String adresse, String codePostal, String ville) {
        Club club = clubRepository.findByName(nomClub);
        if (club == null) {
            log.info("[HoraireBootstrap] club {} introuvable, seed ignore.", nomClub);
            return;
        }

        if (!horaireRepository.findByClubId(club.getId()).isEmpty()) {
            log.info("[HoraireBootstrap] horaires deja presents pour {}, seed ignore.", nomClub);
            return;
        }

        horaireRepository.save(horaire(club, "Lundi", "18:00", "19:00", "Enfants", adresse, codePostal, ville));
        horaireRepository.save(horaire(club, "Lundi", "19:00", "20:30", "Adultes", adresse, codePostal, ville));
        horaireRepository.save(horaire(club, "Jeudi", "18:00", "19:00", "Enfants", adresse, codePostal, ville));
        horaireRepository.save(horaire(club, "Jeudi", "19:00", "20:30", "Adultes", adresse, codePostal, ville));

        log.info("[HoraireBootstrap] 4 creneaux generiques crees pour {}.", nomClub);
    }

    private Horaire horaire(Club club, String jour, String heureDebut, String heureFin, String groupe,
                             String adresse, String codePostal, String ville) {
        Horaire h = new Horaire();
        h.setClub(club);
        h.setJour(jour);
        h.setHeureDebut(heureDebut);
        h.setHeureFin(heureFin);
        h.setGroupe(groupe);
        h.setAdresse(adresse);
        h.setCodePostal(codePostal);
        h.setVille(ville);
        return h;
    }
}
