-- Mise à jour des genres pour les utilisateurs identifiés
UPDATE utilisateur SET genre = 'FEMININ'   WHERE LOWER(prenom) IN ('oce', 'alizee', 'alizée', 'gina', 'coco', 'malorie');
UPDATE utilisateur SET genre = 'MASCULIN'  WHERE LOWER(prenom) IN ('ali', 'leo', 'léo', 'loic', 'loïc', 'system', 'admin', 'adminvilleurbanne', 'adminbourg', 'super');

-- Mise à jour des genres pour les membres
UPDATE membre SET genre = 'FEMININ'   WHERE LOWER(prenom) IN ('oce', 'alizee', 'alizée', 'gina', 'coco', 'malorie');
UPDATE membre SET genre = 'MASCULIN'  WHERE LOWER(prenom) IN ('ali', 'leo', 'léo', 'loic', 'loïc');
