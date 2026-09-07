-- Anti-spam pour le rappel automatique quotidien (EcheanceReminderJob) : sans cette
-- date, une echeance en retard non payee generait une notification + email
-- identiques chaque jour tant qu'elle restait impayee (aucune deduplication).

ALTER TABLE echeance ADD COLUMN IF NOT EXISTS derniere_relance TIMESTAMP NULL;
