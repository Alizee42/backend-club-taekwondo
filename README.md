# Backend — Club Taekwondo

API REST Spring Boot pour une plateforme de gestion de club de taekwondo multi-établissements. Elle gère l'authentification JWT, les membres, les paiements (Stripe), la boutique en ligne, les événements, les actualités, les documents et un CMS intégré.

## Stack technique

| Technologie | Version |
|---|---|
| Java | 17 (image Docker : JRE 21) |
| Spring Boot | 3.2.12 |
| Build | Maven |
| Authentification | JWT (jjwt 0.11.5) + Spring Security stateless |
| Paiements | Stripe Java SDK 29.2.0-beta.1 |
| Rate limiting | Bucket4j 8.10.1 |
| Email | Spring Mail (Gmail SMTP) |
| Stockage fichiers | Google Drive API v3 + répertoire local `uploads/` |
| Migrations BDD | Flyway (V1 → V7) |
| Tests | JUnit 5, H2 (tests uniquement) |
| CI/CD | GitHub Actions → VPS IONOS via SSH + Docker Compose |

## Base de données

**PostgreSQL** — configurée via les variables d'environnement `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD` (défaut : `localhost:5432/taekwondodb`).

Flyway applique automatiquement les migrations au démarrage.

## Prérequis

- Java 17+
- Maven
- PostgreSQL en cours d'exécution

## Variables d'environnement requises

| Variable | Description |
|---|---|
| `PGHOST` / `PGPORT` / `PGDATABASE` / `PGUSER` / `PGPASSWORD` | Connexion PostgreSQL |
| `EMAIL_USERNAME` / `EMAIL_PASSWORD` | Credentials Gmail SMTP |
| `STRIPE_API_KEY` / `STRIPE_PUBLIC_KEY` / `STRIPE_WEBHOOK_SECRET` | Clés Stripe |
| `JWT_SECRET` | Secret de signature JWT |
| `BOOTSTRAP_SUPER_ADMIN_EMAIL` / `BOOTSTRAP_SUPER_ADMIN_PASSWORD` | Création du premier super-admin au démarrage |

## Installation et lancement

```bash
# Lancement en local (port 8081, profil local)
.\scripts\start-local-super-admin.ps1

# Ou manuellement
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

```bash
# Build JAR
./mvnw clean package -DskipTests
java -jar target/club-taekwondo-0.0.1-SNAPSHOT.jar
```

```bash
# Docker
docker build -t club-taekwondo .
docker run -p 8080:8080 --env-file .env club-taekwondo
```

```bash
# Tests (H2 in-memory, Flyway désactivé)
./mvnw test
```

## Rôles utilisateur

| Rôle | Accès |
|---|---|
| `SUPER_ADMIN` | Gestion de tous les clubs et de toutes les données |
| `ADMIN` | Gestion d'un club spécifique |
| `MEMBRE` | Accès à son propre espace membre |
| `PARENT` | Accès aux données de ses enfants membres |

## Principaux endpoints (préfixe `/api/`)

| Domaine | Routes clés |
|---|---|
| Auth | `POST /utilisateurs/login`, `POST /utilisateurs/register`, `POST /utilisateurs/logout`, `GET /utilisateurs/me` |
| Membres | CRUD + `/mes-enfants`, `/by-parent/{id}`, `/by-user/{id}`, `/me` |
| Événements | CRUD + `/actifs`, `/mon-club`, `/inscriptions-enfants`, `PUT /{id}/statut` |
| Paiements | Création manuelle/complète, annulation, téléchargement de facture (`/*/facture`) |
| Stripe | `POST /stripe/payment-intent`, `POST /stripe/sync-payment`, `POST /stripe/webhook`, `GET /stripe/receipt/{id}` |
| Actualités | CRUD avec upload d'image (multipart) |
| Galerie | CRUD par club |
| Boutique | `/produits`, `/commandes`, `/campagnes-commande` |
| Dashboard | `GET /dashboard/admin` — KPIs agrégés par club |
| CMS | `/hero-config`, `/about-config` — configuration de la page d'accueil |
| Fichiers | `POST /uploads`, `GET /uploads/**` — stockage local + Google Drive |
| Contact | `POST /public/contact` — formulaire public |

## CI/CD

Chaque push sur `main` déclenche un pipeline GitHub Actions (`.github/workflows/deploy-ionos.yml`) qui :
1. Build l'image Docker
2. La pousse sur le VPS IONOS via SSH
3. Redémarre le service `taekwondo-api` via Docker Compose
