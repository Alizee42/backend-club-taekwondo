-- Chaque club peut connecter son propre compte Stripe "Standard" pour recevoir
-- directement ses paiements par carte (au lieu du compte Stripe partage de la
-- plateforme). Colonnes nullables/avec valeur par defaut : aucune migration
-- manuelle necessaire, contrairement au piege deja rencontre avec club_id
-- NOT NULL sur about_config.
ALTER TABLE clubs ADD COLUMN stripe_account_id VARCHAR(255);
ALTER TABLE clubs ADD COLUMN stripe_charges_enabled BOOLEAN NOT NULL DEFAULT false;
