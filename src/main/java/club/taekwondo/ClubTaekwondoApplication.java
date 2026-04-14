package club.taekwondo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EntityScan(basePackages = "club.taekwondo.entity.jpa")
public class ClubTaekwondoApplication {

    private static final Logger log = LoggerFactory.getLogger(ClubTaekwondoApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(ClubTaekwondoApplication.class, args);
        log.info("ClubTaekwondoApplication demarre.");
    }
}
