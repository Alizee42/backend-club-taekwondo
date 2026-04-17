CREATE TABLE IF NOT EXISTS actualites (
    id BIGSERIAL PRIMARY KEY,
    titre VARCHAR(255) NOT NULL,
    contenu TEXT NOT NULL,
    date_publication TIMESTAMP NOT NULL,
    type_actu VARCHAR(100),
    club_id BIGINT NOT NULL,
    is_featured BOOLEAN NOT NULL DEFAULT FALSE,
    image_url VARCHAR(512),
    complement TEXT
);

CREATE INDEX IF NOT EXISTS idx_actualites_club_id
    ON actualites (club_id);

CREATE INDEX IF NOT EXISTS idx_actualites_featured
    ON actualites (is_featured);

CREATE TABLE IF NOT EXISTS galeries (
    id BIGSERIAL PRIMARY KEY,
    titre VARCHAR(255) NOT NULL,
    image_url VARCHAR(512),
    description TEXT,
    date_publication TIMESTAMP NOT NULL,
    club_id BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_galeries_club_id
    ON galeries (club_id);
