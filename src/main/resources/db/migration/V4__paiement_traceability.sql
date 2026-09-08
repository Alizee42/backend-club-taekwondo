-- Tracabilite des paiements : qui a cree/valide un paiement manuel, et anti-spam
-- pour les relances email. Le champ existant admin_responsable (annulation) reste
-- inchange : c'est une String libre, deja utilisee par AnnulationRequestDTO, donc
-- on ne le retouche pas pour eviter un changement de contrat cote frontend.

ALTER TABLE paiement ADD COLUMN IF NOT EXISTS cree_par_id BIGINT NULL;
ALTER TABLE paiement ADD COLUMN IF NOT EXISTS valide_par_id BIGINT NULL;
ALTER TABLE paiement ADD COLUMN IF NOT EXISTS date_validation TIMESTAMP NULL;
ALTER TABLE paiement ADD COLUMN IF NOT EXISTS derniere_relance TIMESTAMP NULL;

ALTER TABLE paiement ADD CONSTRAINT fk_paiement_cree_par FOREIGN KEY (cree_par_id) REFERENCES utilisateur(id);
ALTER TABLE paiement ADD CONSTRAINT fk_paiement_valide_par FOREIGN KEY (valide_par_id) REFERENCES utilisateur(id);
