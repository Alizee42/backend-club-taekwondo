package club.taekwondo.config;

import club.taekwondo.entity.jpa.Club;
import club.taekwondo.entity.jpa.Membre;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.enums.Genre;
import club.taekwondo.enums.Role;
import club.taekwondo.repository.jpa.ClubRepository;
import club.taekwondo.repository.jpa.MembreRepository;
import club.taekwondo.repository.jpa.UtilisateurRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

/**
 * Crée les comptes de test Cypress au démarrage.
 * Idempotent : vérifie l'existence avant d'insérer.
 * Désactiver en prod : TEST_DATA_SEED=false
 * Order(50) : s'exécute après ClubDataBootstrap(10) et AdminBootstrap.
 */
@Configuration
public class TestDataBootstrap {

    @Value("${test.data.seed:true}")
    private boolean seedEnabled;

    private static final Logger log = LoggerFactory.getLogger(TestDataBootstrap.class);

    private static final String PASSWORD = "Test1234!";

    @Bean
    @Order(50)
    CommandLineRunner seedTestData(
            UtilisateurRepository utilisateurRepo,
            MembreRepository membreRepo,
            ClubRepository clubRepo,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            if (!seedEnabled) {
                log.info("[TestDataBootstrap] seed désactivé (test.data.seed=false).");
                return;
            }
            Club club = clubRepo.findAll().stream().findFirst().orElse(null);
            String encoded = passwordEncoder.encode(PASSWORD);

            // ── Compte SUPER_ADMIN ──────────────────────────────────────
            if (!utilisateurRepo.existsByEmailIgnoreCase("admin@club-taekwondo.com")) {
                Utilisateur sa = new Utilisateur();
                sa.setNom("Super");
                sa.setPrenom("Admin");
                sa.setEmail("admin@club-taekwondo.com");
                sa.setPassword(encoded);
                sa.setRole(Role.SUPER_ADMIN);
                sa.setTelephone("0600000001");
                sa.setClub(club);
                utilisateurRepo.save(sa);
                log.info("[TestDataBootstrap] super-admin créé : admin@club-taekwondo.com");
            }

            // ── Compte ADMIN ────────────────────────────────────────────
            if (!utilisateurRepo.existsByEmailIgnoreCase("admin@test.com")) {
                Utilisateur admin = new Utilisateur();
                admin.setNom("Admin");
                admin.setPrenom("Test");
                admin.setEmail("admin@test.com");
                admin.setPassword(encoded);
                admin.setRole(Role.ADMIN);
                admin.setTelephone("0600000002");
                admin.setClub(club);
                utilisateurRepo.save(admin);
                log.info("[TestDataBootstrap] admin créé : admin@test.com");
            }

            // ── Compte MEMBRE ───────────────────────────────────────────
            Utilisateur membreUser = utilisateurRepo.findByEmailIgnoreCase("membre@test.com").orElse(null);
            if (membreUser == null) {
                membreUser = new Utilisateur();
                membreUser.setNom("Test");
                membreUser.setPrenom("Membre");
                membreUser.setEmail("membre@test.com");
                membreUser.setPassword(encoded);
                membreUser.setRole(Role.MEMBRE);
                membreUser.setTelephone("0600000003");
                membreUser.setClub(club);
                membreUser = utilisateurRepo.save(membreUser);
                log.info("[TestDataBootstrap] membre créé : membre@test.com");
            }

            // Fiche Membre liée au compte utilisateur MEMBRE
            if (membreRepo.findByCompteUtilisateur_Email("membre@test.com").isEmpty()) {
                Membre fiche = new Membre();
                fiche.setNom("Test");
                fiche.setPrenom("Membre");
                fiche.setDateNaissance(LocalDate.of(2000, 6, 15));
                fiche.setCeinture("Blanche");
                fiche.setEstAdulte(true);
                fiche.setGenre(Genre.MASCULIN);
                fiche.setCompteUtilisateur(membreUser);
                fiche.setClub(club);
                membreRepo.save(fiche);
                log.info("[TestDataBootstrap] fiche membre créée pour membre@test.com");
            }

            // ── Compte PARENT ───────────────────────────────────────────
            Utilisateur parentUser = utilisateurRepo.findByEmailIgnoreCase("parent@test.com").orElse(null);
            if (parentUser == null) {
                parentUser = new Utilisateur();
                parentUser.setNom("Parent");
                parentUser.setPrenom("Test");
                parentUser.setEmail("parent@test.com");
                parentUser.setPassword(encoded);
                parentUser.setRole(Role.PARENT);
                parentUser.setTelephone("0600000004");
                parentUser.setClub(club);
                parentUser = utilisateurRepo.save(parentUser);
                log.info("[TestDataBootstrap] parent créé : parent@test.com");
            }

            // ── Enfants liés au PARENT ──────────────────────────────────
            boolean hasEnfants = !membreRepo.findByParentId(parentUser.getId()).isEmpty();
            if (!hasEnfants) {
                Membre enfant1 = new Membre();
                enfant1.setNom("Dupont");
                enfant1.setPrenom("Lucas");
                enfant1.setDateNaissance(LocalDate.of(2015, 3, 10));
                enfant1.setCeinture("Blanche");
                enfant1.setEstAdulte(false);
                enfant1.setGenre(Genre.MASCULIN);
                enfant1.setParent(parentUser);
                enfant1.setClub(club);
                membreRepo.save(enfant1);

                Membre enfant2 = new Membre();
                enfant2.setNom("Dupont");
                enfant2.setPrenom("Emma");
                enfant2.setDateNaissance(LocalDate.of(2017, 9, 22));
                enfant2.setCeinture("Jaune");
                enfant2.setEstAdulte(false);
                enfant2.setGenre(Genre.FEMININ);
                enfant2.setParent(parentUser);
                enfant2.setClub(club);
                membreRepo.save(enfant2);

                log.info("[TestDataBootstrap] 2 enfants créés pour parent@test.com (Lucas & Emma Dupont)");
            } else {
                log.info("[TestDataBootstrap] enfants déjà présents pour parent@test.com, seed ignoré.");
            }
        };
    }
}
