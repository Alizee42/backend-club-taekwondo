package club.taekwondo.service.jpa;

import club.taekwondo.entity.jpa.Club;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Construit et met en cache un JavaMailSender dedie par club, a partir de
 * 2 variables d'environnement derivees du nom du club :
 * EMAIL_<NOM_NORMALISE>_USERNAME / EMAIL_<NOM_NORMALISE>_PASSWORD.
 *
 * Un club sans ces 2 variables (ou avec une seule) n'est pas considere comme
 * configure : l'appelant doit retomber sur le compte Gmail partage global.
 * Aucun changement de code necessaire pour un nouveau club, seulement 2
 * nouvelles variables d'env dont le nom decoule automatiquement de son nom.
 */
@Component
public class ClubMailSenderFactory {

    private static final Pattern NON_ALNUM = Pattern.compile("[^A-Z0-9]+");
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}");

    private final Environment environment;
    private final ConcurrentHashMap<Long, Optional<ClubMailContext>> cache = new ConcurrentHashMap<>();

    public ClubMailSenderFactory(Environment environment) {
        this.environment = environment;
    }

    public record ClubMailContext(JavaMailSender sender, String fromEmail) {
    }

    public Optional<ClubMailContext> forClub(Club club) {
        if (club == null || club.getId() == null) {
            return Optional.empty();
        }
        return cache.computeIfAbsent(club.getId(), id -> build(club));
    }

    private Optional<ClubMailContext> build(Club club) {
        String key = normalize(club.getName());
        String username = environment.getProperty("EMAIL_" + key + "_USERNAME");
        String password = environment.getProperty("EMAIL_" + key + "_PASSWORD");

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return Optional.empty();
        }

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(environment.getProperty("spring.mail.host", "smtp.gmail.com"));
        sender.setPort(Integer.parseInt(environment.getProperty("spring.mail.port", "587")));
        sender.setUsername(username);
        sender.setPassword(password);

        Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");

        return Optional.of(new ClubMailContext(sender, username));
    }

    private String normalize(String nomClub) {
        if (nomClub == null) return "";
        String sansAccents = Normalizer.normalize(nomClub, Normalizer.Form.NFD);
        sansAccents = DIACRITICS.matcher(sansAccents).replaceAll("");
        String normalise = NON_ALNUM.matcher(sansAccents.toUpperCase()).replaceAll("_");
        return normalise.replaceAll("^_+|_+$", "");
    }
}
