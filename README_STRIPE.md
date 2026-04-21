# Stripe - configuration locale

Ce projet lit les cles Stripe de dev depuis `src/main/resources/application-local.properties`.
La convention locale retenue pour les ecrans de paiement est donc simple :

- le backend local doit etre lance avec `SPRING_PROFILES_ACTIVE=local`
- le profil par defaut ne suffit pas pour le montage Stripe du front
- le point de controle rapide est `GET /api/stripe/config-status`

## Demarrage local recommande

Depuis PowerShell :

```powershell
cd backend-club-taekwondo
.\bin\start-backend-local.ps1
```

Le script :

- active `SPRING_PROFILES_ACTIVE=local`
- garde un niveau de logs lisible pour le dev
- rappelle les endpoints de diagnostic Stripe

Si vous preferez lancer Maven vous-meme :

```powershell
cd backend-club-taekwondo
$env:SPRING_PROFILES_ACTIVE = "local"
.\mvnw.cmd spring-boot:run
```

## Ou sont lues les cles Stripe de dev

Le profil `local` charge :

- `src/main/resources/application-local.properties`
- optionnellement `src/main/resources/application-local-secrets.properties`

En pratique, les cles suivantes doivent y etre disponibles :

- `stripe.api.key=sk_test_...`
- `stripe.public.key=pk_test_...`
- `stripe.webhook.secret=whsec_...` (optionnel pour les webhooks)

Important :

- si vous lancez le backend sans profil `local`, le profil par defaut retombe sur des valeurs `*_dummy`
- dans ce cas `GET /api/stripe/public-key` renvoie `204 No Content`
- le frontend ne peut alors pas monter Stripe Elements

## Verification rapide

Une fois le backend demarre :

```text
GET http://localhost:8080/api/stripe/config-status
GET http://localhost:8080/api/stripe/public-key
```

Resultat attendu pour `config-status` en local :

```json
{
  "publishableKeyConfigured": true,
  "secretKeyConfigured": true
}
```

Resultat attendu pour `public-key` :

```json
{
  "publicKey": "pk_test_..."
}
```

Si `public-key` renvoie `204`, le backend n'est pas lance avec `SPRING_PROFILES_ACTIVE=local` ou les cles Stripe ne sont pas disponibles.

## Eclipse / STS

Si vous demarrez depuis Eclipse ou STS :

1. Ouvrez la configuration d'execution de `ClubTaekwondoApplication`
2. Ajoutez `SPRING_PROFILES_ACTIVE=local` dans les variables d'environnement
3. Demarrez l'application
4. Verifiez ensuite `/api/stripe/config-status`

Vous pouvez aussi passer par les VM arguments :

```text
-Dspring.profiles.active=local
```

## Bonnes pratiques

- Ne mettez pas de vraies cles dans `application.properties`
- Preferez `application-local.properties` ou `application-local-secrets.properties`
- Ne versionnez jamais `application-local-secrets.properties`
- Si une cle a deja ete exposee, faites une rotation dans Stripe
