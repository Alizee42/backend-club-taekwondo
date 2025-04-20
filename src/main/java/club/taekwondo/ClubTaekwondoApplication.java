package club.taekwondo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@EntityScan(basePackages = "club.taekwondo.entity.jpa")
@EnableJpaRepositories(basePackages = "club.taekwondo.repository.jpa")
@EnableMongoRepositories(basePackages = "club.taekwondo.repository.mongo")
public class ClubTaekwondoApplication {
    public static void main(String[] args) {
        SpringApplication.run(ClubTaekwondoApplication.class, args);
    }
}
