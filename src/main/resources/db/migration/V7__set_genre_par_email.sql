-- Mise à jour ciblée par email (identifiants sûrs)
UPDATE utilisateur SET genre = 'FEMININ'  WHERE email IN (
    'oce.gueye@gmail.com',
    'gina.nenet@hotmail.fr',
    'alizee.gueye@gmail.com',
    'dc@gmail.com'
);

UPDATE utilisateur SET genre = 'MASCULIN' WHERE email IN (
    'agueye1403@gmail.com'
);

-- S'assurer que tout le reste est NON_PRECISE (sécurité)
UPDATE utilisateur SET genre = 'NON_PRECISE' WHERE genre IS NULL;
UPDATE membre     SET genre = 'NON_PRECISE' WHERE genre IS NULL;
