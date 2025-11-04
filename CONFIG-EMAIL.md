# Configuration Email - Club Taekwondo

## 🔧 Configuration Locale (Développement)

### Étape 1 : Obtenir un mot de passe d'application Gmail

1. Allez sur [myaccount.google.com](https://myaccount.google.com)
2. **Sécurité** → Activez **Validation en 2 étapes** si ce n'est pas déjà fait
3. **Sécurité** → **Mots de passe des applications**
4. Sélectionnez **Application** : "Mail"
5. Sélectionnez **Appareil** : "Ordinateur Windows"
6. Cliquez sur **Générer**
7. Copiez le mot de passe de 16 caractères (format : `xxxx xxxx xxxx xxxx`)

### Étape 2 : Configurer application-local-secrets.properties

Le fichier `application-local-secrets.properties` a déjà été préparé. Remplacez simplement :

```properties
EMAIL_USERNAME=alizee.gueye@gmail.com
EMAIL_PASSWORD=VOTRE_MOT_DE_PASSE_APP_ICI  # ← Collez le mot de passe généré (sans espaces)
EMAIL_FROM=alizee.gueye@gmail.com
```

### Étape 3 : Redémarrer le backend

Une fois configuré, redémarrez votre serveur Spring Boot. Les logs afficheront :

```
✅ Email envoyé à : utilisateur@example.com
```

---

## 🚀 Configuration Production (Render)

### Variables d'environnement à définir sur Render

Allez dans votre service backend sur Render → **Environment** et ajoutez :

| Variable | Valeur | Description |
|----------|--------|-------------|
| `EMAIL_USERNAME` | `alizee.gueye@gmail.com` | Votre email Gmail |
| `EMAIL_PASSWORD` | `xxxx xxxx xxxx xxxx` | Mot de passe d'application (16 caractères) |
| `EMAIL_FROM` | `alizee.gueye@gmail.com` | Email "expéditeur" |
| `FRONTEND_URL` | `https://frontend-club-taekwondo.netlify.app` | URL du frontend (déjà configurée) |

### Configuration SMTP (déjà dans application.properties)

```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=${EMAIL_USERNAME}
spring.mail.password=${EMAIL_PASSWORD}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true

app.mail.from=${EMAIL_FROM:noreply@club-taekwondo.com}
app.mail.frontend-url=https://frontend-club-taekwondo.netlify.app
```

---

## 📧 Emails envoyés par l'application

| Événement | Template | Destinataire |
|-----------|----------|--------------|
| **Inscription** | Bienvenue au Club | Nouvel utilisateur |
| **Réinitialisation mot de passe** | Lien de réinitialisation | Utilisateur demandeur |
| **Paiement reçu** | Reçu de paiement | Client (si opt-in activé) |
| **Contact** | Message de contact | Club + accusé client |

---

## 🧪 Tester l'envoi d'emails

### 1. Créer un nouveau compte

```bash
POST http://localhost:8080/api/utilisateurs/register
Content-Type: application/json

{
  "email": "test@example.com",
  "password": "Test1234!",
  "nom": "Doe",
  "prenom": "John",
  "role": "MEMBRE"
}
```

**Réponse attendue :**
```json
{
  "message": "Utilisateur créé avec succès.",
  "emailSent": true  // ← doit être true si config OK
}
```

### 2. Vérifier les logs backend

```
✅ Email envoyé à : test@example.com
```

### 3. Vérifier la boîte mail

Un email "Bienvenue au Club de Taekwondo !" doit arriver dans la boîte du destinataire.

---

## ⚠️ Troubleshooting

### Email ne part pas (emailSent: false)

**Cause 1 : Mot de passe incorrect**
```
❌ Erreur envoi email : 535-5.7.8 Username and Password not accepted
```
→ Régénérez un mot de passe d'application Gmail

**Cause 2 : Validation en 2 étapes désactivée**
```
❌ Erreur envoi email : Sign-in blocked
```
→ Activez la validation en 2 étapes sur votre compte Google

**Cause 3 : Variables d'environnement non chargées**
```
❌ Erreur envoi email : Authentication failed
```
→ Vérifiez que `EMAIL_USERNAME` et `EMAIL_PASSWORD` sont bien définis
→ Redémarrez le serveur après modification

**Cause 4 : Pare-feu bloque le port 587**
```
❌ Erreur envoi email : Connection timed out
```
→ Vérifiez que votre réseau autorise les connexions SMTP sortantes

---

## 🔒 Sécurité

- ✅ **Mot de passe d'application** : utilisé (plus sécurisé que le vrai mot de passe)
- ✅ **Fichier secrets** : ignoré par Git (`.gitignore`)
- ✅ **Variables d'environnement** : utilisées en prod (Render)
- ✅ **STARTTLS** : activé (chiffrement des emails)

---

## 📝 Alternative : Autres fournisseurs SMTP

### SendGrid (recommandé pour production)

```properties
spring.mail.host=smtp.sendgrid.net
spring.mail.port=587
spring.mail.username=apikey
spring.mail.password=<YOUR_SENDGRID_API_KEY>
```

### Mailgun

```properties
spring.mail.host=smtp.mailgun.org
spring.mail.port=587
spring.mail.username=<YOUR_MAILGUN_SMTP_LOGIN>
spring.mail.password=<YOUR_MAILGUN_SMTP_PASSWORD>
```

### AWS SES

```properties
spring.mail.host=email-smtp.eu-west-1.amazonaws.com
spring.mail.port=587
spring.mail.username=<YOUR_AWS_SMTP_USERNAME>
spring.mail.password=<YOUR_AWS_SMTP_PASSWORD>
```

---

## 🎯 Résumé rapide

**Local** :
1. Générez un mot de passe d'application Gmail
2. Ajoutez-le dans `application-local-secrets.properties`
3. Redémarrez le backend

**Production (Render)** :
1. Ajoutez les variables `EMAIL_USERNAME`, `EMAIL_PASSWORD`, `EMAIL_FROM`
2. Redéployez le service
3. Testez avec une inscription

✅ **C'est tout !**
