package club.taekwondo.config;

import club.taekwondo.entity.jpa.Actualite;
import club.taekwondo.entity.jpa.Avis;
import club.taekwondo.entity.jpa.Club;
import club.taekwondo.entity.jpa.Enseignant;
import club.taekwondo.entity.jpa.Galerie;
import club.taekwondo.repository.jpa.ActualiteRepository;
import club.taekwondo.repository.jpa.AvisRepository;
import club.taekwondo.repository.jpa.ClubRepository;
import club.taekwondo.repository.jpa.EnseignantRepository;
import club.taekwondo.repository.jpa.GalerieRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Seed d'avis, actualites, photos de galerie et enseignants factices, pour
 * donner un apercu visuel realiste du site public (page d'accueil, avis,
 * actualites, galerie, professeurs) sans attendre du vrai contenu.
 *
 * Contenu clairement fictif (noms/textes generiques), images d'illustration
 * libres (Unsplash). A retirer facilement : supprimer ce fichier, ou mettre
 * demo.contenu.seed=false, ou vider les tables avis/actualites/galeries/
 * enseignants pour les clubs concernes.
 *
 * Idempotent : ne rejoue rien si des avis existent deja pour le club.
 */
@Configuration
public class ContenuDemoBootstrap {

    private static final Logger log = LoggerFactory.getLogger(ContenuDemoBootstrap.class);

    @Value("${demo.contenu.seed:true}")
    private boolean seedEnabled;

    @Bean
    @Order(60)
    CommandLineRunner seedContenuDemo(
            ClubRepository clubRepository,
            AvisRepository avisRepository,
            ActualiteRepository actualiteRepository,
            GalerieRepository galerieRepository,
            EnseignantRepository enseignantRepository
    ) {
        return args -> {
            if (!seedEnabled) {
                log.info("[ContenuDemoBootstrap] seed désactivé (demo.contenu.seed=false).");
                return;
            }

            for (String nomClub : new String[]{"Villeurbanne", "Bourg-en-Bresse", "Villars-les-Dombes", "Amberieu-en-Bugey"}) {
                Club club = clubRepository.findByName(nomClub);
                if (club == null) {
                    log.info("[ContenuDemoBootstrap] club {} introuvable, seed ignoré.", nomClub);
                    continue;
                }
                seedAvis(avisRepository, club);
                seedActualites(actualiteRepository, club);
                seedGalerie(galerieRepository, club);
                seedEnseignants(enseignantRepository, club);
            }
        };
    }

    private void seedAvis(AvisRepository avisRepository, Club club) {
        if (!avisRepository.findByClub_Id(club.getId()).isEmpty()) {
            log.info("[ContenuDemoBootstrap] avis déjà présents pour {}, seed ignoré.", club.getName());
            return;
        }

        avisRepository.save(avis(club, "Sophie L.", 5,
                "Excellent club, mes enfants adorent les cours ! Les professeurs sont pédagogues et à l'écoute. Ambiance familiale garantie.",
                LocalDate.now().minusDays(30)));
        avisRepository.save(avis(club, "Karim B.", 5,
                "Je pratique depuis 2 ans, très bonne préparation à la compétition. L'encadrement est sérieux et bienveillant.",
                LocalDate.now().minusDays(18)));
        avisRepository.save(avis(club, "Elodie M.", 4,
                "Très bon club pour débuter en famille. Les horaires sont pratiques et les locaux propres. Je recommande.",
                LocalDate.now().minusDays(9)));

        log.info("[ContenuDemoBootstrap] 3 avis créés pour {}.", club.getName());
    }

    private Avis avis(Club club, String pseudo, int note, String contenu, LocalDate date) {
        Avis a = new Avis();
        a.setClub(club);
        a.setPseudoVisiteur(pseudo);
        a.setNote(note);
        a.setContenu(contenu);
        a.setDatePub(date);
        a.setApprouve(true);
        a.setTypeAvis("general");
        return a;
    }

    private void seedActualites(ActualiteRepository actualiteRepository, Club club) {
        if (!actualiteRepository.findByClubIdOrderByDatePublicationDesc(club.getId()).isEmpty()) {
            log.info("[ContenuDemoBootstrap] actualités déjà présentes pour {}, seed ignoré.", club.getName());
            return;
        }

        actualiteRepository.save(actualite(club,
                "Reprise des cours de la saison",
                "La reprise des cours aura lieu la semaine prochaine. Tous les créneaux habituels sont maintenus. "
                        + "N'hésitez pas à contacter le club pour toute question sur les inscriptions.",
                "Reprise des entraînements pour tous les groupes, enfants comme adultes.",
                "info", true,
                "https://images.unsplash.com/photo-1555597673-b21d5c935865?w=1200&q=80",
                LocalDateTime.now().minusDays(20)));

        actualiteRepository.save(actualite(club,
                "Résultats de la dernière compétition",
                "Félicitations à tous les compétiteurs du club pour leurs résultats lors de la dernière compétition régionale. "
                        + "Plusieurs médailles ramenées, bravo à toutes et tous pour votre investissement !",
                "Plusieurs médailles ramenées lors de la compétition régionale.",
                "competition", true,
                "https://images.unsplash.com/photo-1517438476312-10d79c077509?w=1200&q=80",
                LocalDateTime.now().minusDays(10)));

        actualiteRepository.save(actualite(club,
                "Stage de perfectionnement ce week-end",
                "Un stage de perfectionnement ouvert à toutes les ceintures est organisé ce week-end. "
                        + "Places limitées, inscription auprès de votre professeur.",
                "Stage ouvert à toutes les ceintures, places limitées.",
                "evenement", false,
                "https://images.unsplash.com/photo-1544367567-0f2fcb009e0b?w=1200&q=80",
                LocalDateTime.now().minusDays(3)));

        log.info("[ContenuDemoBootstrap] 3 actualités créées pour {}.", club.getName());
    }

    private Actualite actualite(Club club, String titre, String contenu, String extrait, String type,
                                 boolean featured, String imageUrl, LocalDateTime date) {
        Actualite a = new Actualite();
        a.setClubId(club.getId());
        a.setTitre(titre);
        a.setContenu(contenu);
        a.setExtrait(extrait);
        a.setTypeActu(type);
        a.setFeatured(featured);
        a.setImageUrl(imageUrl);
        a.setDatePublication(date);
        return a;
    }

    private void seedGalerie(GalerieRepository galerieRepository, Club club) {
        if (!galerieRepository.findByClubIdOrderByDatePublicationDesc(club.getId()).isEmpty()) {
            log.info("[ContenuDemoBootstrap] galerie déjà présente pour {}, seed ignoré.", club.getName());
            return;
        }

        galerieRepository.save(photo(club, "Entraînement du groupe enfants",
                "https://images.unsplash.com/photo-1555597673-b21d5c935865?w=1200&q=80",
                LocalDateTime.now().minusDays(25)));
        galerieRepository.save(photo(club, "Passage de grades",
                "https://images.unsplash.com/photo-1517438476312-10d79c077509?w=1200&q=80",
                LocalDateTime.now().minusDays(15)));
        galerieRepository.save(photo(club, "Compétition régionale",
                "https://images.unsplash.com/photo-1544367567-0f2fcb009e0b?w=1200&q=80",
                LocalDateTime.now().minusDays(8)));
        galerieRepository.save(photo(club, "Stage de perfectionnement",
                "https://images.unsplash.com/photo-1602827114685-efbb2717da9e?w=1200&q=80",
                LocalDateTime.now().minusDays(2)));

        log.info("[ContenuDemoBootstrap] 4 photos créées pour la galerie de {}.", club.getName());
    }

    private Galerie photo(Club club, String titre, String imageUrl, LocalDateTime date) {
        Galerie g = new Galerie();
        g.setClubId(club.getId());
        g.setTitre(titre);
        g.setImageUrl(imageUrl);
        g.setDatePublication(date);
        return g;
    }

    private void seedEnseignants(EnseignantRepository enseignantRepository, Club club) {
        if (!enseignantRepository.findByClub_Id(club.getId()).isEmpty()) {
            log.info("[ContenuDemoBootstrap] enseignants déjà présents pour {}, seed ignoré.", club.getName());
            return;
        }

        enseignantRepository.save(enseignant(club, "Lefebvre", "Antoine",
                "Ceinture noire 4e Dan, professeur principal",
                "Professeur principal du club depuis plusieurs années, formé à l'encadrement de tous les publics, "
                        + "de l'initiation à la compétition.",
                "https://images.unsplash.com/photo-1517438476312-10d79c077509?w=800&q=80"));

        enseignantRepository.save(enseignant(club, "Rousseau", "Camille",
                "Ceinture noire 2e Dan, spécialiste jeunes pousses",
                "Encadre les cours enfants et débutants, avec une approche pédagogique axée sur le respect et la progression.",
                "https://images.unsplash.com/photo-1544367567-0f2fcb009e0b?w=800&q=80"));

        log.info("[ContenuDemoBootstrap] 2 enseignants créés pour {}.", club.getName());
    }

    private Enseignant enseignant(Club club, String nom, String prenom, String specialite,
                                   String description, String photoUrl) {
        Enseignant e = new Enseignant();
        e.setClub(club);
        e.setNom(nom);
        e.setPrenom(prenom);
        e.setSpecialite(specialite);
        e.setDescription(description);
        e.setPhotoUrl(photoUrl);
        return e;
    }
}
