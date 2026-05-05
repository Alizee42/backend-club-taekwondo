-- Ajout de la colonne lien_action à la table notification
ALTER TABLE notification ADD COLUMN IF NOT EXISTS lien_action VARCHAR(255);
