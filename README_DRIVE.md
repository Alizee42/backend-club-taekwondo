Configuration Google Drive (backend)

But
---
Cette doc explique comment configurer le backend pour uploader les fichiers sur Google Drive via un compte de service.

Étapes rapides
---------------
1) Créer un service account dans Google Cloud
   - GCP Console -> IAM & Admin -> Service Accounts -> Create Service Account
   - Donner au compte le rôle minimal requis (pour tests: "Editor" temporaire ; en prod préférez un rôle plus restreint)
   - Activer l'API Google Drive pour le projet (APIs & Services -> Library -> Google Drive API -> Enable)
   - Créer une clé JSON (Create Key -> JSON) et télécharger le fichier (ex: service-account-drive-key.json)

2) Déposer la clé sur le serveur local (ou CI) — NE PAS la committer
   - Option A (recommandée): stocker la clé hors du repo, p.ex. /home/alizee/keys/service-account-drive-key.json
   - Option B (temporaire pour tests): copier dans backend-club-taekwondo/src/main/resources/ (mais ne pas committer)

3) Lancer le backend en pointant vers la clé
   - Depuis le dossier backend-club-taekwondo :

     ./mvnw -Dgoogle.credentials.path=file:/chemin/absolu/service-account-drive-key.json spring-boot:run

   - Si vous avez ajouté un dossier parent Drive (optionnel) :

     ./mvnw -Dgoogle.credentials.path=file:/chemin/vers/key.json -Dgoogle.drive.parentFolderId=ID_DOSSIER spring-boot:run

4) Tester l'upload depuis l'UI
   - Ouvrez l'UI (Parent / Membre) -> Documents -> téléverser
   - Contrôlez la console du backend, vous devriez voir :

     Fichier téléchargé sur Google Drive: https://drive.google.com/...

Propriétés disponibles
----------------------
- google.credentials.path
  - Valeur par défaut : classpath:credentials.json
  - Exemple : file:/home/alizee/keys/service-account-drive-key.json

- google.drive.parentFolderId
  - ID du dossier Drive où vous voulez déposer les fichiers (optionnel). Si absent, les fichiers iront dans la racine du Drive du compte service.

Sécurité
--------
- NE JAMAIS committer les fichiers de clés (service account JSON) ou les OAuth client secrets.
- Si un secret a été committé par erreur, révoquez-le immédiatement dans la Google Console.
- Préférez l'utilisation d'un secret manager (GCP Secret Manager, Vault) en production.

Remarque technique
------------------
- Le service `GoogleDriveUploadService` utilise un compte de service et les bibliothèques Google pour uploader et rendre le fichier accessible (permission "anyone" par défaut). Si vous voulez restreindre l'accès, on peut ajouter une propriété `google.drive.makePublic=false` et modifier le service en conséquence.

Support / Tests
---------------
Si vous voulez, je peux :
- lancer un test d'upload local et vous donner le lien Drive (il faudra me fournir un token si l'API est protégée),
- ou ajouter un exemple `curl` dans ce README.
