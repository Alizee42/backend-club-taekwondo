package club.taekwondo.service.jpa;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.mail.frontend-url}")
    private String frontendUrl;

    @Value("${app.mail.contact-to:${app.mail.from}}")
    private String contactTo;

    /* ============================================
       📩 EMAIL - Réinitialisation de mot de passe
       ============================================ */
    public void envoyerEmailReinitialisationMotDePasse(String email, String token) {
        String sujet = "Réinitialisation de votre mot de passe - Club de Taekwondo";
        String lienReinitialisation = frontendUrl + "/reinitialiser-mot-de-passe?token=" + token;

        String contenuHtml = baseTemplate("Réinitialisation du mot de passe",
                """
                <p>Bonjour,</p>
                <p>Vous avez demandé à réinitialiser votre mot de passe pour votre compte du <strong>Club de Taekwondo</strong>.</p>
                <p>Cliquez sur le bouton ci-dessous pour créer un nouveau mot de passe :</p>
                <div style="text-align:center;margin:24px 0;">
                    <a href="%s" class="button">Réinitialiser mon mot de passe</a>
                </div>
                <div class="warning">
                    ⚠️ <strong>Important :</strong>
                    <ul>
                        <li>Ce lien est valide pendant <strong>1 heure</strong></li>
                        <li>Il ne peut être utilisé qu'une seule fois</li>
                        <li>Si vous n'êtes pas à l'origine de cette demande, ignorez simplement cet email</li>
                    </ul>
                </div>
                <p style="margin-top:24px;">Cordialement,<br><strong>L'équipe du Club de Taekwondo</strong></p>
                """.formatted(lienReinitialisation)
        );

        envoyerEmailHtml(email, sujet, contenuHtml);
    }

    /* ============================================
       👋 EMAIL - Confirmation d’inscription
       ============================================ */
    public void envoyerEmailConfirmationInscription(String email, String nomUtilisateur) {
        String sujet = "Bienvenue au Club de Taekwondo !";

        String contenuHtml = baseTemplate("Bienvenue parmi nous !",
                """
                <p>Bonjour %s,</p>
                <p>Votre inscription au <strong>Club de Taekwondo</strong> a bien été enregistrée.</p>
                <p>Vous pouvez dès maintenant accéder à votre espace membre :</p>
                <div style="text-align:center;margin:24px 0;">
                    <a href="%s" class="button">Accéder à mon espace</a>
                </div>
                <p style="margin-top:12px;">Si le bouton ne fonctionne pas, copiez ce lien dans votre navigateur :</p>
                <p><a href="%s">%s</a></p>
                <p style="margin-top:24px;">À très bientôt,<br><strong>L'équipe du Club de Taekwondo</strong></p>
                """.formatted(nomUtilisateur, frontendUrl, frontendUrl, frontendUrl)
        );

        envoyerEmailHtml(email, sujet, contenuHtml);
    }

    /* ============================================
       💬 EMAIL - Message de contact
       ============================================ */
    public void envoyerMessageContact(String nom, String email, String objet, String contenu) {
        String sujet = "[Contact Site] " + objet;

        // message reçu côté club
        String htmlContact = baseTemplate("Nouveau message de contact",
                """
                <p><strong>Nom :</strong> %s</p>
                <p><strong>Email :</strong> %s</p>
                <p><strong>Objet :</strong> %s</p>
                <hr style="margin:24px 0;border:none;border-top:1px solid #e2e8f0;">
                <p style="white-space:pre-line;">%s</p>
                <p style="margin-top:24px;font-size:13px;color:#6C7581;">Message généré automatiquement depuis le site.</p>
                """.formatted(escape(nom), escape(email), escape(objet), escape(contenu))
        );

        envoyerEmailHtml(contactTo != null && !contactTo.isBlank() ? contactTo : fromEmail, sujet, htmlContact);

        // accusé de réception côté utilisateur
        try {
            String ackSujet = "Votre message au Club de Taekwondo";
            String ackHtml = baseTemplate("Message bien reçu ✅",
                    """
                    <p>Bonjour %s,</p>
                    <p>Nous avons bien reçu votre message :</p>
                    <blockquote style="margin:12px 0;padding:12px 16px;border-left:4px solid #1E6091;background:#f8fafc;">
                        <strong>Objet :</strong> %s
                    </blockquote>
                    <p>Nous reviendrons vers vous rapidement.</p>
                    <p style="margin-top:24px;">Cordialement,<br><strong>L'équipe du Club de Taekwondo</strong></p>
                    """.formatted(escape(nom), escape(objet))
            );
            envoyerEmailHtml(email, ackSujet, ackHtml);
        } catch (Exception ignored) {}
    }

    /* ============================================
       � EMAIL - Reçu de paiement (fallback club)
       ============================================ */
    public void envoyerRecuPaiement(String destinataire, double montant, String description, String receiptUrl) {
        String sujet = "Votre reçu de paiement - Club de Taekwondo";
        String body = baseTemplate("Reçu de paiement",
                ("""
                <p>Bonjour,</p>
                <p>Nous confirmons la réception de votre paiement au <strong>Club de Taekwondo</strong>.</p>
                <ul>
                    <li><strong>Montant :</strong> %s</li>
                    <li><strong>Détails :</strong> %s</li>
                </ul>
                <div style=\"text-align:center;margin:24px 0;\">
                    <a href=\"%s\" class=\"button\">Voir mon reçu Stripe</a>
                </div>
                <p style=\"margin-top:24px;\">Cordialement,<br><strong>L'équipe du Club de Taekwondo</strong></p>
                """
                        ).formatted(String.format("%.2f €", montant), escape(description == null ? "Paiement" : description), receiptUrl)
        );
        envoyerEmailHtml(destinataire, sujet, body);
    }

    /* ============================================
       �🧩 Template de base pour tous les emails
       ============================================ */
    private String baseTemplate(String titre, String contenuHtml) {
        return """
        <!DOCTYPE html>
        <html lang="fr">
        <head>
            <meta charset="UTF-8">
            <style>
                body {
                    font-family: 'Roboto', Arial, sans-serif;
                    background-color: #f9fafb;
                    color: #1B2533;
                    margin: 0;
                    padding: 0;
                }
                .container {
                    max-width: 600px;
                    margin: 32px auto;
                    background: #fff;
                    border-radius: 12px;
                    box-shadow: 0 6px 18px rgba(0,0,0,0.08);
                    overflow: hidden;
                }
                .header {
                    background: linear-gradient(135deg, #1E6091, #283A5A);
                    color: #fff;
                    text-align: center;
                    padding: 24px 32px;
                }
                .header h1 {
                    margin: 0;
                    font-size: 1.5rem;
                    font-weight: 700;
                }
                .content {
                    padding: 32px;
                    line-height: 1.6;
                }
                a.button {
                    display: inline-block;
                    background: linear-gradient(90deg, #1E6091, #E63946);
                    color: #fff !important;
                    padding: 12px 28px;
                    border-radius: 8px;
                    text-decoration: none;
                    font-weight: 600;
                    transition: background 0.3s ease;
                }
                a.button:hover {
                    background: #283A5A;
                }
                .warning {
                    background: #fff8e1;
                    border-left: 4px solid #F1A208;
                    padding: 16px;
                    border-radius: 8px;
                    margin-top: 24px;
                    font-size: 0.95rem;
                    color: #7a4f00;
                }
                .footer {
                    text-align: center;
                    padding: 16px;
                    font-size: 12px;
                    color: #6C7581;
                    background: #f8fafc;
                    border-top: 1px solid #e2e8f0;
                }
                @media (max-width: 600px) {
                    .content { padding: 24px 16px; }
                    .header h1 { font-size: 1.3rem; }
                }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <h1>%s</h1>
                </div>
                <div class="content">
                    %s
                </div>
                <div class="footer">
                    <p>Cet email a été envoyé automatiquement. Merci de ne pas y répondre.</p>
                </div>
            </div>
        </body>
        </html>
        """.formatted(titre, contenuHtml);
    }

    /* ============================================
       ⚙️ Méthode d’envoi d’email générique
       ============================================ */
    private void envoyerEmailHtml(String destinataire, String sujet, String contenuHtml) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(destinataire);
            helper.setSubject(sujet);
            helper.setText(contenuHtml, true);

            mailSender.send(message);
            System.out.println("✅ Email envoyé à : " + destinataire);

        } catch (MessagingException e) {
            System.err.println("❌ Erreur envoi email à " + destinataire + " : " + e.getMessage());
            throw new RuntimeException("Erreur lors de l'envoi de l'email", e);
        }
    }

    /* ============================================
       🧼 Fonction utilitaire
       ============================================ */
    private String escape(String v) {
        if (v == null) return "";
        return v.replace("<", "&lt;").replace(">", "&gt;");
    }
}
