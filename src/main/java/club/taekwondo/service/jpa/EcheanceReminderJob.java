package club.taekwondo.service.jpa;

import club.taekwondo.entity.jpa.Echeance;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.repository.jpa.EcheanceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Job quotidien qui détecte les échéances en retard et envoie
 * une notification in-app + un email au membre / parent concerné.
 */
@Component
public class EcheanceReminderJob {

    private static final Logger log = LoggerFactory.getLogger(EcheanceReminderJob.class);

    @Autowired
    private EcheanceRepository echeanceRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private EmailService emailService;

    /**
     * Exécuté chaque jour à 8h00.
     * Pour chaque échéance "en attente" dont la date est dépassée,
     * on envoie une notification in-app au membre (et/ou au parent).
     */
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional(readOnly = true)
    public void rappelerEcheancesEnRetard() {
        LocalDate aujourd_hui = LocalDate.now();
        List<Echeance> enRetard = echeanceRepository.findByStatutAndDateEcheanceBefore("en attente", aujourd_hui);

        log.info("[CRON] Echéances en retard à traiter : {}", enRetard.size());

        for (Echeance echeance : enRetard) {
            if (echeance.getPaiement() == null) continue;

            try {
                // Destinataire : utilisateur attaché au paiement (parent ou membre adulte)
                Utilisateur utilisateur = echeance.getPaiement().getUtilisateur();
                if (utilisateur == null && echeance.getPaiement().getMembre() != null) {
                    utilisateur = echeance.getPaiement().getMembre().getCompteUtilisateur();
                    if (utilisateur == null) {
                        utilisateur = echeance.getPaiement().getMembre().getParent();
                    }
                }
                if (utilisateur == null) continue;

                String role = utilisateur.getRole() != null ? utilisateur.getRole().name() : "MEMBRE";
                String lien = role.equals("PARENT") ? "/parent/paiements" : "/membre/paiements";

                String message = String.format(
                        "Votre échéance de %.2f € prévue le %s est en retard de paiement.",
                        echeance.getMontant() != null ? echeance.getMontant() : 0.0,
                        echeance.getDateEcheance()
                );

                // Notification in-app
                notificationService.envoyerNotification(
                        utilisateur.getId(),
                        "Échéance en retard",
                        message,
                        "paiement",
                        lien
                );

                // Email de rappel
                if (utilisateur.getEmail() != null && !utilisateur.getEmail().isBlank()) {
                    try {
                        emailService.envoyerEmailHtml(
                                utilisateur.getClub(),
                                utilisateur.getEmail(),
                                "Rappel : échéance de paiement en retard",
                                buildEmailHtml(utilisateur.getPrenom(), message)
                        );
                    } catch (Exception emailEx) {
                        log.warn("[CRON] Échec envoi email à {} : {}", utilisateur.getEmail(), emailEx.getMessage());
                    }
                }

                log.info("[CRON] Rappel envoyé userId={} echeanceId={}", utilisateur.getId(), echeance.getId());

            } catch (Exception ex) {
                log.warn("[CRON] Erreur traitement echeanceId={} : {}", echeance.getId(), ex.getMessage());
            }
        }
    }

    private String buildEmailHtml(String prenom, String message) {
        return """
                <html><body style="font-family:Arial,sans-serif;color:#333;">
                <div style="max-width:600px;margin:0 auto;padding:24px;">
                    <h2 style="color:#e53e3e;">⚠ Rappel de paiement</h2>
                    <p>Bonjour %s,</p>
                    <p>%s</p>
                    <p>Merci de régulariser votre situation dans les plus brefs délais en vous connectant à votre espace membre.</p>
                    <p>Cordialement,<br><strong>L'équipe du Club de Taekwondo</strong></p>
                </div>
                </body></html>
                """.formatted(prenom != null ? prenom : "", message);
    }
}
