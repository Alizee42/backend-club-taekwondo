CREATE TABLE IF NOT EXISTS campagne_commande (
    id               BIGSERIAL PRIMARY KEY,
    titre            VARCHAR(255) NOT NULL,
    description      TEXT,
    date_ouverture   DATE NOT NULL,
    date_fermeture   DATE NOT NULL,
    actif            BOOLEAN NOT NULL DEFAULT TRUE,
    club_id          BIGINT REFERENCES clubs(id)
);

ALTER TABLE commande
    ADD COLUMN IF NOT EXISTS campagne_id BIGINT REFERENCES campagne_commande(id);
