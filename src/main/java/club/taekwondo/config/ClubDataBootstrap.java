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

            Club club = buildClub(
                    "Villeurbanne",
                    "",
                    "",
                    ""
            );

            clubRepository.save(club);
            log.info("[ClubBootstrap] Club Villeurbanne cree.");
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
