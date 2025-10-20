package club.taekwondo.config;

import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.enums.Role;
import club.taekwondo.repository.jpa.UtilisateurRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminBootstrap {

    @Bean
    CommandLineRunner initAdmin(UtilisateurRepository utilisateurRepository,
                                PasswordEncoder passwordEncoder) {
        return args -> {
            final String adminEmail = "admin@club.local";

            // Création d'un compte SUPER_ADMIN pour la gestion globale
            final String superAdminEmail = "superadmin@club.local";
            if (utilisateurRepository.findByEmail(superAdminEmail).isEmpty()) {
                Utilisateur superAdmin = new Utilisateur();
                superAdmin.setNom("Super");
                superAdmin.setPrenom("Admin");
                superAdmin.setEmail(superAdminEmail);
                superAdmin.setPassword(passwordEncoder.encode("superadmin123")); // changer le mot de passe après premier démarrage
                superAdmin.setRole(Role.SUPER_ADMIN);

                utilisateurRepository.save(superAdmin);
                System.out.println("[BOOTSTRAP] Compte super-admin créé: " + superAdminEmail + " / superadmin123");
            } else {
                System.out.println("[BOOTSTRAP] Compte super-admin déjà présent: " + superAdminEmail);
            }

            // Compte admin historique (comportement existant)
            if (utilisateurRepository.findByEmail(adminEmail).isEmpty()) {
                Utilisateur admin = new Utilisateur();
                admin.setNom("Admin");
                admin.setPrenom("System");
                admin.setEmail(adminEmail);
                admin.setPassword(passwordEncoder.encode("admin123")); // change-le ensuite
                admin.setRole(Role.ADMIN);

                utilisateurRepository.save(admin);
                System.out.println("[BOOTSTRAP] Compte admin créé: " + adminEmail + " / admin123");
            } else {
                System.out.println("[BOOTSTRAP] Compte admin déjà présent: " + adminEmail);
            }
        };
    }
}


