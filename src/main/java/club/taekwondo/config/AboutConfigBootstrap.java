package club.taekwondo.config;

import club.taekwondo.entity.jpa.AboutConfig;
import club.taekwondo.entity.jpa.AboutValue;
import club.taekwondo.repository.jpa.AboutConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.List;

@Configuration
public class AboutConfigBootstrap {

    private static final Logger log = LoggerFactory.getLogger(AboutConfigBootstrap.class);

    @Bean
    @Order(21)
    CommandLineRunner seedAboutConfig(AboutConfigRepository repo) {
        return args -> {
            if (repo.existsById(1L)) {
                log.info("[AboutConfigBootstrap] Config À propos déjà présente, seed ignoré.");
                return;
            }

            AboutConfig c = new AboutConfig();
            c.setId(1L);
            c.setHeadingLine1("Un club fondé sur");
            c.setHeadingLine2("l'excellence et le respect");
            c.setLeadText("fondé en 1995, accueille des pratiquants de tous niveaux dans un environnement de respect et de discipline.");
            c.setDescriptionText("Notre mission est de former des athlètes sur le plan physique et mental, en mettant l'accent sur le développement personnel et la maîtrise de soi.");
            c.setFoundedYear("1995");
            c.setBadgeLabel("Fondé en");
            c.setChips(List.of("Respect", "Discipline", "Excellence", "Esprit d'équipe"));

            c.setMissionTitle("Notre Mission");
            c.setMissionText("Accompagner chaque pratiquant dans un environnement sûr et stimulant, pour progresser et s'épanouir à travers le Taekwondo.");
            c.setVisionTitle("Notre Vision");
            c.setVisionText("Inspirer et transmettre les valeurs du Taekwondo, pour bâtir une communauté forte et ouverte à toutes les générations.");
            c.setValuesTitle("Nos Valeurs");

            AboutValue v1 = new AboutValue(); v1.setBold("Respect"); v1.setDescription("chaque individu est valorisé");
            AboutValue v2 = new AboutValue(); v2.setBold("Discipline"); v2.setDescription("excellence par persévérance");
            AboutValue v3 = new AboutValue(); v3.setBold("Maîtrise"); v3.setDescription("aller plus loin chaque jour");
            c.setValues(List.of(v1, v2, v3));

            repo.save(c);
            log.info("[AboutConfigBootstrap] Config À propos par défaut créée.");
        };
    }
}
