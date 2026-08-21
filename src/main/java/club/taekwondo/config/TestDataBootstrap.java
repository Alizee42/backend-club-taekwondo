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
 * Crée des comptes de démo répartis sur les 4 clubs, pour montrer le
 * fonctionnement multi-club de l'application.
 * Idempotent : vérifie l'existence par email avant d'insérer.
 * Désactiver : TEST_DATA_SEED=false
 * Order(50) : s'exécute après ClubDataBootstrap(10)/HoraireDataBootstrap(11)
 * et AdminBootstrap.
 */
@Configuration
public class TestDataBootstrap {

    @Value("${test.data.seed:true}")
    private boolean seedEnabled;

    private static final Logger log = LoggerFactory.getLogger(TestDataBootstrap.class);

    private static final String PASSWORD = "Test1234!";

    private record ClubDemoSeed(String clubName, String slug, String adminNom, String adminPrenom,
                                 String parentNom, String parentPrenom,
                                 String enfant1Prenom, String enfant2Prenom) {
    }

    private static final ClubDemoSeed[] CLUBS_SEED = {
            new ClubDemoSeed("Villeurbanne", "villeurbanne", "Martin", "Sophie", "Dupont", "Julien", "Lucas", "Emma"),
            new ClubDemoSeed("Bourg-en-Bresse", "bourg", "Bernard", "Claire", "Girard", "Marie", "Nolan", "Chloe"),
            new ClubDemoSeed("Villars-les-Dombes", "villars", "Petit", "Thomas", "Roux", "Camille", "Enzo", "Lea"),
            new ClubDemoSeed("Amberieu-en-Bugey", "amberieu", "Moreau", "Julie", "Fontaine", "Nicolas", "Mattis", "Ines"),
    };

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
            String encoded = passwordEncoder.encode(PASSWORD);

            // ── Compte SUPER_ADMIN (global, sans club) ───────────────────
            if (!utilisateurRepo.existsByEmailIgnoreCase("admin@club-taekwondo.com")) {
                Utilisateur sa = new Utilisateur();
                sa.setNom("Super");
                sa.setPrenom("Admin");
                sa.setEmail("admin@club-taekwondo.com");
                sa.setPassword(encoded);
                sa.setRole(Role.SUPER_ADMIN);
                sa.setTelephone("0600000001");
                utilisateurRepo.save(sa);
                log.info("[TestDataBootstrap] super-admin créé : admin@club-taekwondo.com");
            }

            // ── Un admin + un parent (2 enfants) par club ────────────────
            for (ClubDemoSeed seed : CLUBS_SEED) {
                Club club = clubRepo.findByName(seed.clubName());
                if (club == null) {
                    log.info("[TestDataBootstrap] club {} introuvable, seed de démo ignoré pour ce club.", seed.clubName());
                    continue;
                }
                seedAdmin(utilisateurRepo, club, seed, encoded);
                Utilisateur parentUser = seedParent(utilisateurRepo, club, seed, encoded);
                seedEnfants(membreRepo, club, parentUser, seed);
            }
        };
    }

    private void seedAdmin(UtilisateurRepository utilisateurRepo, Club club, ClubDemoSeed seed, String encoded) {
        String email = "admin." + seed.slug() + "@club-taekwondo.com";
        if (utilisateurRepo.existsByEmailIgnoreCase(email)) {
            return;
        }
        Utilisateur admin = new Utilisateur();
        admin.setNom(seed.adminNom());
        admin.setPrenom(seed.adminPrenom());
        admin.setEmail(email);
        admin.setPassword(encoded);
        admin.setRole(Role.ADMIN);
        admin.setTelephone("0600000010");
        admin.setClub(club);
        utilisateurRepo.save(admin);
        log.info("[TestDataBootstrap] admin créé : {} (club {})", email, club.getName());
    }

    private Utilisateur seedParent(UtilisateurRepository utilisateurRepo, Club club, ClubDemoSeed seed, String encoded) {
        String email = "parent." + seed.slug() + "@club-taekwondo.com";
        Utilisateur parentUser = utilisateurRepo.findByEmailIgnoreCase(email).orElse(null);
        if (parentUser == null) {
            parentUser = new Utilisateur();
            parentUser.setNom(seed.parentNom());
            parentUser.setPrenom(seed.parentPrenom());
            parentUser.setEmail(email);
            parentUser.setPassword(encoded);
            parentUser.setRole(Role.PARENT);
            parentUser.setTelephone("0600000020");
            parentUser.setClub(club);
            parentUser = utilisateurRepo.save(parentUser);
            log.info("[TestDataBootstrap] parent créé : {} (club {})", email, club.getName());
        }
        return parentUser;
    }

    private void seedEnfants(MembreRepository membreRepo, Club club, Utilisateur parentUser, ClubDemoSeed seed) {
        if (!membreRepo.findByParentId(parentUser.getId()).isEmpty()) {
            return;
        }
        Membre enfant1 = new Membre();
        enfant1.setNom(seed.parentNom());
        enfant1.setPrenom(seed.enfant1Prenom());
        enfant1.setDateNaissance(LocalDate.of(2015, 3, 10));
        enfant1.setCeinture("Blanche");
        enfant1.setEstAdulte(false);
        enfant1.setGenre(Genre.MASCULIN);
        enfant1.setParent(parentUser);
        enfant1.setClub(club);
        membreRepo.save(enfant1);

        Membre enfant2 = new Membre();
        enfant2.setNom(seed.parentNom());
        enfant2.setPrenom(seed.enfant2Prenom());
        enfant2.setDateNaissance(LocalDate.of(2017, 9, 22));
        enfant2.setCeinture("Jaune");
        enfant2.setEstAdulte(false);
        enfant2.setGenre(Genre.FEMININ);
        enfant2.setParent(parentUser);
        enfant2.setClub(club);
        membreRepo.save(enfant2);

        log.info("[TestDataBootstrap] 2 enfants créés pour {} (club {})", parentUser.getEmail(), club.getName());
    }
}
