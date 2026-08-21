package club.taekwondo.config;

import club.taekwondo.entity.jpa.Club;
import club.taekwondo.repository.jpa.ClubRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * Seed des 4 clubs "Olympique Taekwondo" avec leurs vraies coordonnees
 * publiques (adresse/telephone/email trouves en ligne, aout 2026).
 * Idempotent : met a jour un club existant par nom, en cree un nouveau sinon.
 *
 * Desactivable via demo.clubs.seed=false (ou en supprimant ce fichier +
 * les lignes correspondantes dans TestDataBootstrap si tu veux tout retirer
 * definitivement plus tard).
 */
@Configuration
public class ClubDataBootstrap {

    private static final Logger log = LoggerFactory.getLogger(ClubDataBootstrap.class);

    @Value("${demo.clubs.seed:true}")
    private boolean seedEnabled;

    @Bean
    @Order(10)
    CommandLineRunner seedClubs(ClubRepository clubRepository) {
        return args -> {
            if (!seedEnabled) {
                log.info("[ClubBootstrap] seed desactive (demo.clubs.seed=false).");
                return;
            }

            upsert(clubRepository, "Villeurbanne",
                    "4 Rue de Bat Yam, 69100 Villeurbanne",
                    "0663978926",
                    "taekwondovilleurbannais@gmail.com");

            upsert(clubRepository, "Bourg-en-Bresse",
                    "Maison de la Culture et de la Citoyennete, 4 allee des Brotteaux, 01000 Bourg-en-Bresse",
                    "0609223778",
                    "taekwondobourgenbresse@gmail.com");

            upsert(clubRepository, "Villars-les-Dombes",
                    "18 rue de la Mantoliere, 01330 Villars-les-Dombes",
                    "",
                    "");

            upsert(clubRepository, "Amberieu-en-Bugey",
                    "78 avenue du General Sarrail, 01500 Amberieu-en-Bugey",
                    "",
                    "");

            log.info("[ClubBootstrap] {} club(s) en base apres seed.", clubRepository.count());
        };
    }

    private void upsert(ClubRepository clubRepository, String name, String adresse, String telephone, String email) {
        Club club = clubRepository.findByName(name);
        boolean isNew = club == null;
        if (isNew) {
            club = new Club();
            club.setName(name);
        }
        club.setAdresse(adresse);
        club.setTelephone(telephone);
        club.setEmail(email);
        clubRepository.save(club);
        log.info("[ClubBootstrap] Club {} {}.", name, isNew ? "cree" : "mis a jour");
    }
}
