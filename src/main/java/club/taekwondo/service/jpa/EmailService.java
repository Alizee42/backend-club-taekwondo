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

    /**
     * Envoie un email de réinitialisation de mot de passe avec template HTML
     */
    public void envoyerEmailReinitialisationMotDePasse(String email, String token) {
        String sujet = "Réinitialisation de votre mot de passe - Club de Taekwondo";
        String lienReinitialisation = frontendUrl + "/reinitialiser-mot-de-passe?token=" + token;
        
        String contenuHtml = creerTemplateReinitialisation(lienReinitialisation);
        
        envoyerEmailHtml(email, sujet, contenuHtml);
    }

    /**
     * Crée le template HTML pour la réinitialisation
     */
    private String creerTemplateReinitialisation(String lien) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <style>
                    body { font-family: 'Roboto', Arial, sans-serif; line-height: 1.6; color: #1e1e1e; max-width: 600px; margin: 0 auto; background: #fdfdfd; }
                    .container { padding: 20px; }
                    .header { 
                        background: linear-gradient(135deg, #1E6091, #283a5a); 
                        color: #ffffff !important; 
                        padding: 30px; 
                        text-align: center; 
                        border-radius: 12px 12px 0 0;
                        box-shadow: 0 4px 12px rgba(30, 96, 145, 0.15);
                    }
                    .content { 
                        background: #ffffff; 
                        padding: 30px; 
                        border-radius: 0 0 12px 12px;
                        box-shadow: 0 8px 20px rgba(0,0,0,.1);
                        border: 1px solid #e5e7eb;
                        border-top: none;
                    }
                    .button { 
                        display: inline-block; 
                        background: linear-gradient(135deg, #1E6091, #283a5a); 
                        color: #ffffff !important; 
                        padding: 15px 30px; 
                        text-decoration: none; 
                        border-radius: 8px; 
                        font-weight: bold; 
                        margin: 20px 0;
                        box-shadow: 0 4px 12px rgba(30, 96, 145, 0.25);
                        transition: transform 0.2s ease;
                        border: none;
                        outline: none;
                    }
                    .button:hover { 
                        background: linear-gradient(135deg, #283a5a, #1B263B); 
                        color: #ffffff !important;
                        transform: translateY(-1px);
                    }
                    .button:visited { color: #ffffff !important; }
                    .button:active { color: #ffffff !important; }
                    .footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }
                    .warning { 
                        background: linear-gradient(135deg, #fef3cd, #fde68a); 
                        border: 1px solid #F1A208; 
                        padding: 15px; 
                        border-radius: 8px; 
                        margin: 20px 0;
                        border-left: 4px solid #F1A208;
                    }
                    .logo { font-size: 2.5rem; margin-bottom: 0.5rem; }
                    .brand-title { 
                        font-size: 1.8rem; 
                        font-weight: bold; 
                        margin: 0; 
                        color: #ffffff !important;
                    }
                    .brand-subtitle { 
                        font-size: 1rem; 
                        opacity: 0.9; 
                        margin: 0; 
                        color: #ffffff !important;
                    }
                    /* Force white color for all header content */
                    .header * { color: #ffffff !important; }
                    .header h1 { color: #ffffff !important; }
                    .header p { color: #ffffff !important; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="logo">🥋</div>
                        <h1 class="brand-title">Club de Taekwondo</h1>
                        <p class="brand-subtitle">Réinitialisation de mot de passe</p>
                    </div>
                    <div class="content">
                        <h2>Bonjour,</h2>
                        <p>Vous avez demandé à réinitialiser votre mot de passe pour votre compte du Club de Taekwondo.</p>
                        
                        <p>Cliquez sur le bouton ci-dessous pour créer un nouveau mot de passe :</p>
                        
                        <div style="text-align: center;">
                            <a href="%s" class="button">🔐 Réinitialiser mon mot de passe</a>
                        </div>
                        
                        <div class="warning">
                            ⚠️ <strong>Important :</strong>
                            <ul>
                                <li>Ce lien est valide pendant <strong>1 heure seulement</strong></li>
                                <li>Il ne peut être utilisé qu'une seule fois</li>
                                <li>Si vous n'avez pas demandé cette réinitialisation, ignorez cet email</li>
                            </ul>
                        </div>
                        
                        <p>Pour votre sécurité, ne partagez jamais ce lien avec qui que ce soit.</p>
                        
                        <p>Cordialement,<br>
                        <strong>L'équipe du Club de Taekwondo</strong></p>
                    </div>
                    <div class="footer">
                        <p>Cet email a été envoyé automatiquement. Merci de ne pas y répondre.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(lien);
    }

    /**
     * Envoie un email avec confirmation d'inscription
     */
    public void envoyerEmailConfirmationInscription(String email, String nomUtilisateur) {
        String sujet = "Bienvenue au Club de Taekwondo !";
        
        String contenuHtml = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <style>
                    body { font-family: 'Roboto', Arial, sans-serif; line-height: 1.6; color: #1e1e1e; max-width: 600px; margin: 0 auto; background: #fdfdfd; }
                    .container { padding: 20px; }
                    .header { 
                        background: linear-gradient(135deg, #1E6091, #283a5a); 
                        color: #ffffff !important; 
                        padding: 30px; 
                        text-align: center; 
                        border-radius: 12px 12px 0 0;
                        box-shadow: 0 4px 12px rgba(30, 96, 145, 0.15);
                    }
                    .content { 
                        background: #ffffff; 
                        padding: 30px; 
                        border-radius: 0 0 12px 12px;
                        box-shadow: 0 8px 20px rgba(0,0,0,.1);
                        border: 1px solid #e5e7eb;
                        border-top: none;
                    }
                    .footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }
                    .logo { font-size: 2.5rem; margin-bottom: 0.5rem; }
                    .brand-title { 
                        font-size: 1.8rem; 
                        font-weight: bold; 
                        margin: 0; 
                        color: #ffffff !important;
                    }
                    .welcome-message { 
                        background: linear-gradient(135deg, #2ecc71, #27ae60); 
                        color: #ffffff !important; 
                        padding: 15px; 
                        border-radius: 8px; 
                        margin: 20px 0;
                        text-align: center;
                        font-weight: bold;
                    }
                    /* Force white color for all header content */
                    .header * { color: #ffffff !important; }
                    .header h1 { color: #ffffff !important; }
                    .header p { color: #ffffff !important; }
                    .welcome-message * { color: #ffffff !important; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="logo">🥋</div>
                        <h1 class="brand-title">Bienvenue !</h1>
                    </div>
                    <div class="content">
                        <div class="welcome-message">
                            🎉 Inscription réussie !
                        </div>
                        <h2>Bonjour %s,</h2>
                        <p>Votre inscription au <strong>Club de Taekwondo</strong> a bien été prise en compte !</p>
                        <p>Vous pouvez maintenant vous connecter à votre espace membre et découvrir tous nos services.</p>
                        <p><strong>À bientôt sur les tatamis !</strong> 🥊</p>
                        <p>Cordialement,<br><strong>L'équipe du Club de Taekwondo</strong></p>
                    </div>
                    <div class="footer">
                        <p>Cet email a été envoyé automatiquement.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(nomUtilisateur);
        
        envoyerEmailHtml(email, sujet, contenuHtml);
    }

    /**
     * Méthode générique pour envoyer un email HTML
     */
    private void envoyerEmailHtml(String destinataire, String sujet, String contenuHtml) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(destinataire);
            helper.setSubject(sujet);
            helper.setText(contenuHtml, true); // true = HTML
            
            mailSender.send(message);
            System.out.println("✅ Email HTML envoyé avec succès à : " + destinataire);
            
        } catch (MessagingException e) {
            System.err.println("❌ Erreur envoi email HTML à " + destinataire + " : " + e.getMessage());
            throw new RuntimeException("Erreur lors de l'envoi de l'email", e);
        }
    }
}