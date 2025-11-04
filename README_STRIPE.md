# Stripe – Configuration locale (Windows)

Ce projet utilise Stripe pour générer des PaymentIntent. En environnement local, vous devez fournir des clés de TEST valides.

## 1) Clés nécessaires (mode test)
- Clé secrète (serveur) – commence par `sk_test_...`
- Clé publique (front) – commence par `pk_test_...`
- (Optionnel) Secret de webhook – `whsec_...`

Récupérez-les dans Stripe: Developers → API keys.

## 2) Définir les variables d'environnement (PowerShell)
Ouvrez un terminal PowerShell et définissez vos variables pour la session:

```powershell
$env:STRIPE_API_KEY = "sk_test_VOTRE_CLE_ICI"
$env:STRIPE_PUBLIC_KEY = "pk_test_VOTRE_CLE_ICI"
# Optionnel si vous testez les webhooks
# $env:STRIPE_WEBHOOK_SECRET = "whsec_VOTRE_SECRET_ICI"
```

Ensuite, démarrez le backend dans le même terminal:

```powershell
cd backend-club-taekwondo
./mvnw.cmd spring-boot:run
```

Astuce: pour des variables persistantes (hors session), utilisez les variables système Windows (Panneau de configuration → Système → Paramètres avancés → Variables d'environnement).

## 3) Vérifier la configuration
- Clé publique: `GET http://localhost:8080/api/stripe/public-key`
  - Doit renvoyer `{ "publicKey": "pk_test_..." }`.
  - Si 204 No Content: la clé publique n'est pas configurée (ou factice).
- Statut global: `GET http://localhost:8080/api/stripe/config-status`
  - Réponse `{ publishableKeyConfigured: bool, secretKeyConfigured: bool }`.
- Création PI: déclenchée par le front sur `/api/stripe/create-payment-intent`.

## 4) Points d'attention
- Ne mélangez pas les clés live/test.
- Les valeurs par défaut `pk_test_dummy` / `sk_test_dummy` sont factices et provoquent `Invalid API Key provided`.
- Relancez l'IDE/terminal si vous avez modifié des variables d'environnement système.

## 5) Dépannage rapide
- 400 avec `Invalid API Key provided`: la clé secrète n'est pas injectée (ou factice). Vérifiez `STRIPE_API_KEY` et redémarrez.
- `/public-key` renvoie pk_test_dummy: la clé publique n'est pas injectée. Définissez `STRIPE_PUBLIC_KEY`.
- Réseau/Proxy: vérifiez l'accès à `https://api.stripe.com`.

## 6) Alternative: config via application-local.properties (Eclipse)

Si vous lancez le backend depuis Eclipse, vous pouvez stocker vos clés de TEST dans `src/main/resources/application-local.properties` et activer le profil `local` pour éviter de dépendre des variables d'environnement.

1) Ouvrez `backend-club-taekwondo/src/main/resources/application-local.properties` et remplacez les valeurs Stripe:

```properties
stripe.api.key=sk_test_VOTRE_CLE_ICI
stripe.public.key=pk_test_VOTRE_CLE_ICI
# Optionnel pour les webhooks
stripe.webhook.secret=whsec_VOTRE_SECRET_ICI
```

2) Activez le profil `local` dans Eclipse:
- Run > Run Configurations… > Spring Boot App > ClubTaekwondoApplication
- Onglet "Arguments" → VM arguments: `-Dspring.profiles.active=local`
  - (ou onglet "Environment" → New… → Name: `SPRING_PROFILES_ACTIVE`, Value: `local`)

3) Démarrez l’application depuis Eclipse et vérifiez:
- `GET http://localhost:8080/api/stripe/config-status` → doit afficher `publishableKeyConfigured: true` et `secretKeyConfigured: true`.
- `GET http://localhost:8080/api/stripe/public-key` → doit renvoyer `pk_test_...`.

4) Sécurité / bonnes pratiques
- Évitez d’ajouter des clés réelles dans `application.properties` (profil par défaut) pour ne pas les committer.
- Préférez `application-local.properties` + profil `local` activé via les arguments d’exécution d’Eclipse.
- Si vous avez déjà committé des clés, remplacez-les par des valeurs factices, forcez un rotate dans le Dashboard Stripe, et repoussez.

## 7) Ne pas exposer les clés sur GitHub

Pour éviter toute fuite en poussant sur GitHub:

- Un fichier local de secrets est prévu et ignoré par Git: `src/main/resources/application-local-secrets.properties`.
- Il est automatiquement importé quand le profil `local` est actif: `spring.config.import=optional:classpath:application-local-secrets.properties` (ajouté dans `application-local.properties`).
- Utilisez l’exemple fourni: `application-local-secrets.properties.example` → renommez-le en `application-local-secrets.properties` et mettez vos vraies valeurs de TEST.
- Ne versionnez jamais le fichier sans l’extension `.example`.
