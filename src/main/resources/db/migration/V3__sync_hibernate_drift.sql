-- Rattrape la derive de schema causee par hibernate.ddl-auto=update encore actif
-- en prod apres la remise a plat du 2026-08-20 (V1) : la colonne
-- notification.lien_action et deux contraintes UNIQUE dupliquees ont ete
-- (re)creees automatiquement par Hibernate depuis V1. Ecrit de facon idempotente
-- pour fonctionner aussi bien sur une base neuve que sur la base de prod existante.
--
-- campagne_commande : table orpheline creee par la derive Hibernate, sans plus
-- aucune entite/repository/controller dans le code actuel, et vide en prod
-- (verifie le 2026-08-22) -> suppression.

DROP TABLE IF EXISTS campagne_commande;

ALTER TABLE notification ADD COLUMN IF NOT EXISTS lien_action VARCHAR(255);

-- Doublons crees par Hibernate a cote des contraintes nommees deja definies en V1
ALTER TABLE required_document DROP CONSTRAINT IF EXISTS ukp29d5penda011pj7ky6n3sbwp;
ALTER TABLE inscription_evenement DROP CONSTRAINT IF EXISTS uksam6du569g187ohjlfxg9qjj9;
