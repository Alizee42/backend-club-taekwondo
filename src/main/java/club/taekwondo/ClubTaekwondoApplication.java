package club.taekwondo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.core.env.Environment;

@SpringBootApplication
@EntityScan(basePackages = "club.taekwondo.entity.jpa")
public class ClubTaekwondoApplication implements ApplicationListener<ApplicationReadyEvent> {

    @Value("${spring.data.mongodb.uri:non défini}")
    private String mongoUri;

    public static void main(String[] args) {
        SpringApplication.run(ClubTaekwondoApplication.class, args);
        System.out.println("ClubTaekwondoApplication demarre...");
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        Environment env = event.getApplicationContext().getEnvironment();
        String[] profiles = env.getActiveProfiles();
        System.out.println("[DEBUG] Profils Spring actifs : " + String.join(", ", profiles));
        System.out.println("[DEBUG] URI MongoDB utilisée : " + mongoUri);
    }
}
