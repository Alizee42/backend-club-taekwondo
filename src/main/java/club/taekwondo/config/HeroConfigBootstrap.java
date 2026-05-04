package club.taekwondo.config;

import club.taekwondo.entity.jpa.HeroConfig;
import club.taekwondo.entity.jpa.HeroStat;
import club.taekwondo.repository.jpa.HeroConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.List;

@Configuration
public class HeroConfigBootstrap {

    private static final Logger log = LoggerFactory.getLogger(HeroConfigBootstrap.class);

    @Bean
    @Order(20)
    CommandLineRunner seedHeroConfig(HeroConfigRepository repo) {
        return args -> {
            if (repo.existsById(1L)) {
                log.info("[HeroConfigBootstrap] Config hero deja presente, seed ignore.");
                return;
            }

            HeroConfig config = new HeroConfig();
            config.setId(1L);
            config.setEyebrowText("Club de Taekwondo · Lyon");
            config.setIdentityStrong("Olympique");
            config.setIdentityMid("Taekwondo");
            config.setSlogans(List.of(
                "Discipline", "Respect", "Dépassement de soi",
                "Esprit d'équipe", "Performance", "Confiance"
            ));

            HeroStat s1 = new HeroStat(); s1.setValue("30+"); s1.setLabel("ans d'expérience");
            HeroStat s2 = new HeroStat(); s2.setValue("200+"); s2.setLabel("membres actifs");
            HeroStat s3 = new HeroStat(); s3.setValue("50+"); s3.setLabel("médailles");
            HeroStat s4 = new HeroStat(); s4.setIcon("ri-shield-star-line"); s4.setLabel("Club FFT affilié");
            config.setStats(List.of(s1, s2, s3, s4));

            repo.save(config);
            log.info("[HeroConfigBootstrap] Config hero par défaut créée.");
        };
    }
}
