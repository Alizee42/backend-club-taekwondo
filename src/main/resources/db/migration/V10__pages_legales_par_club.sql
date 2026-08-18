-- Les pages "Mentions légales" et "Politique de confidentialité" étaient un texte fixe
-- identique pour tout le site. Chaque club ayant sa propre structure juridique, on crée
-- une configuration éditable par club (le texte RGPD commun reste statique côté frontend).

CREATE TABLE IF NOT EXISTS mentions_legales_config (
    id                  BIGSERIAL PRIMARY KEY,
    club_id             BIGINT NOT NULL UNIQUE REFERENCES clubs(id),
    nom_association     VARCHAR(255),
    statut_juridique    VARCHAR(255),
    adresse             VARCHAR(255),
    numero_rna          VARCHAR(100),
    numero_siren        VARCHAR(100),
    representant_legal  VARCHAR(255),
    email               VARCHAR(255),
    telephone           VARCHAR(50),
    hebergeur_nom       VARCHAR(255),
    hebergeur_adresse   VARCHAR(255),
    hebergeur_site_web  VARCHAR(255),
    mediateur_nom       VARCHAR(255),
    mediateur_contact   VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS politique_confidentialite_config (
    id                  BIGSERIAL PRIMARY KEY,
    club_id             BIGINT NOT NULL UNIQUE REFERENCES clubs(id),
    nom_association     VARCHAR(255),
    adresse             VARCHAR(255),
    email_contact       VARCHAR(255),
    email_rgpd          VARCHAR(255),
    representant_legal  VARCHAR(255)
);
