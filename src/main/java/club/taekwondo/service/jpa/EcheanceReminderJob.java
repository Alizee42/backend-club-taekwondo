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
import java.time.LocalDateTime;
import java.util.List;

/**
 * Job quotidien qui détecte les échéances en retard et envoie
 * une notification in-app + un email au membre / parent concerné.
 * Anti-spam : une échéance déjà relancée il y a moins de RELANCE_INTERVALLE_JOURS
 * n'est pas re-traitée, pour éviter une notification/email identique chaque jour
 * tant que l'échéance reste impayée.
 */
@Component
public class EcheanceReminderJob {

    private static final Logger log = LoggerFactory.getLogger(EcheanceReminderJob.class);
    private static final int RELANCE_INTERVALLE_JOURS = 3;

    @Autowired
    private EcheanceRepository echeanceRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private EmailService emailService;

    /**
     * Exécuté chaque jour à 8h00.
     * Pour chaque échéance "en attente" dont la date est dépassée et qui n'a pas
     * été relancée depuis RELANCE_INTERVALLE_JOURS jours, on envoie une notification
     * in-app au membre (et/ou au parent).
     */
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void rappelerEcheancesEnRetard() {
        LocalDate aujourd_hui = LocalDate.now();
        List<Echeance> toutesEnRetard = echeanceRepository.findByStatutAndDateEcheanceBefore("en attente", aujourd_hui);

        LocalDateTime seuil = LocalDateTime.now().minusDays(RELANCE_INTERVALLE_JOURS);
        List<Echeance> enRetard = toutesEnRetard.stream()
                .filter(e -> e.getDerniereRelance() == null || e.getDerniereRelance().isBefore(seuil))
                .toList();

        log.info("[CRON] Echéances en retard : {} au total, {} à relancer (deja relancees recemment: {}).",
                toutesEnRetard.size(), enRetard.size(), toutesEnRetard.size() - enRetard.size());

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
                        emailService.envoyerRappelPaiement(
                                utilisateur.getClub(),
                                utilisateur.getEmail(),
                                utilisateur.getPrenom(),
                                message
                        );
                    } catch (Exception emailEx) {
                        log.warn("[CRON] Échec envoi email à {} : {}", utilisateur.getEmail(), emailEx.getMessage());
                    }
                }

                echeance.setDerniereRelance(LocalDateTime.now());
                echeanceRepository.save(echeance);

                log.info("[CRON] Rappel envoyé userId={} echeanceId={}", utilisateur.getId(), echeance.getId());

            } catch (Exception ex) {
                log.warn("[CRON] Erreur traitement echeanceId={} : {}", echeance.getId(), ex.getMessage());
            }
        }
    }
}
