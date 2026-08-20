ALTER TABLE commande
    DROP COLUMN IF EXISTS campagne_id;

DROP TABLE IF EXISTS campagne_commande;
