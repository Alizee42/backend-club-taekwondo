package club.taekwondo.config;

import club.taekwondo.entity.jpa.Club;
import club.taekwondo.repository.jpa.ClubRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.List;

@Configuration
public class ClubDataBootstrap {

    private static final Logger log = LoggerFactory.getLogger(ClubDataBootstrap.class);

    @Bean
    @Order(10)
    CommandLineRunner seedClubs(ClubRepository clubRepository) {
        return args -> {
            if (clubRepository.count() > 0) {
                log.info("[ClubBootstrap] {} club(s) deja presents, seed ignore.", clubRepository.count());
                return;
            }

            List<Club> clubs = List.of(
                    buildClub(
                            "Olympique TKD Villeurbannais",
                            "12 rue des Sports, 69100 Villeurbanne",
                            "04 72 00 00 01",
                            "contact@otk-villeurbanne.fr"
                    ),
                    buildClub(
                            "Club TKD Grenoble",
                            "5 avenue du Stade, 38000 Grenoble",
                            "04 76 00 00 02",
                            "contact@tkd-grenoble.fr"
                    ),
                    buildClub(
                            "Acad\u00e9mie TKD Lyon 3",
                            "47 cours Gambetta, 69003 Lyon",
                            "04 78 00 00 03",
                            "contact@academie-tkd-lyon.fr"
                    )
            );

            clubRepository.saveAll(clubs);
            log.info("[ClubBootstrap] {} clubs de test crees.", clubs.size());
        };
    }

    private Club buildClub(String name, String adresse, String telephone, String email) {
        Club c = new Club();
        c.setName(name);
        c.setAdresse(adresse);
        c.setTelephone(telephone);
        c.setEmail(email);
        return c;
    }
}
