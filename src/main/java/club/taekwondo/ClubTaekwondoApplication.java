package club.taekwondo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EntityScan(basePackages = "club.taekwondo.entity.jpa")
public class ClubTaekwondoApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClubTaekwondoApplication.class, args);
        System.out.println("ClubTaekwondoApplication demarre...");
    }
}
