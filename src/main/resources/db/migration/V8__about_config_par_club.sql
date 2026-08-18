-- La table about_config était un singleton global (une seule ligne, id=1) partagé par tous les clubs.
-- On la rattache désormais à un club, comme le reste des entités de contenu.

ALTER TABLE about_config ADD COLUMN IF NOT EXISTS club_id BIGINT;

-- Rattache la ligne existante (si présente) au premier club connu, pour ne pas perdre le contenu déjà saisi.
UPDATE about_config
SET club_id = (SELECT id FROM clubs ORDER BY id ASC LIMIT 1)
WHERE club_id IS NULL;

ALTER TABLE about_config ADD CONSTRAINT fk_about_config_club
    FOREIGN KEY (club_id) REFERENCES clubs(id);

ALTER TABLE about_config ADD CONSTRAINT uq_about_config_club
    UNIQUE (club_id);
