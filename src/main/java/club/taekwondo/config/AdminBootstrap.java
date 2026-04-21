package club.taekwondo.config;

import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.enums.Role;
import club.taekwondo.repository.jpa.UtilisateurRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminBootstrap {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    @Bean
    CommandLineRunner initAdmin(UtilisateurRepository utilisateurRepository,
                                PasswordEncoder passwordEncoder) {
        return args -> {
            String superAdminEmail = System.getenv("BOOTSTRAP_SUPER_ADMIN_EMAIL");
            String superAdminPassword = System.getenv("BOOTSTRAP_SUPER_ADMIN_PASSWORD");

            String adminEmail = System.getenv("BOOTSTRAP_ADMIN_EMAIL");
            String adminPassword = System.getenv("BOOTSTRAP_ADMIN_PASSWORD");

            if (superAdminEmail != null && !superAdminEmail.isBlank()
                    && superAdminPassword != null && !superAdminPassword.isBlank()) {
                if (utilisateurRepository.findByEmail(superAdminEmail).isEmpty()) {
                    Utilisateur superAdmin = new Utilisateur();
                    superAdmin.setNom("Super");
                    superAdmin.setPrenom("Admin");
                    superAdmin.setEmail(superAdminEmail);
                    superAdmin.setPassword(passwordEncoder.encode(superAdminPassword));
                    superAdmin.setRole(Role.SUPER_ADMIN);
                    utilisateurRepository.save(superAdmin);
                    log.info("[BOOTSTRAP] compte super-admin cree: {}", superAdminEmail);
                } else {
                    log.info("[BOOTSTRAP] compte super-admin deja present: {}", superAdminEmail);
                }
            } else {
                log.info("[BOOTSTRAP] variables super-admin absentes, bootstrap ignore.");
            }

            if (adminEmail != null && !adminEmail.isBlank()
                    && adminPassword != null && !adminPassword.isBlank()) {
                if (utilisateurRepository.findByEmail(adminEmail).isEmpty()) {
                    Utilisateur admin = new Utilisateur();
                    admin.setNom("Admin");
                    admin.setPrenom("System");
                    admin.setEmail(adminEmail);
                    admin.setPassword(passwordEncoder.encode(adminPassword));
                    admin.setRole(Role.ADMIN);
                    utilisateurRepository.save(admin);
                    log.info("[BOOTSTRAP] compte admin cree: {}", adminEmail);
                } else {
                    log.info("[BOOTSTRAP] compte admin deja present: {}", adminEmail);
                }
            } else {
                log.info("[BOOTSTRAP] variables admin absentes, bootstrap ignore.");
            }
        };
    }
}
