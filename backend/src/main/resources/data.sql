-- Zones de garde (11 zones de Dakar)
INSERT INTO zones (id, nom) VALUES
(1, 'Plateau'),
(2, 'Fann - Point E'),
(3, 'Ouakam - Ngor'),
(4, 'Yoff'),
(5, 'Grand Dakar'),
(6, 'Liberté - Sicap'),
(7, 'Parcelles Assainies'),
(8, 'Guédiawaye'),
(9, 'Pikine'),
(10, 'Rufisque'),
(11, 'Keur Massar')
ON CONFLICT (id) DO NOTHING;

-- Quartiers
INSERT INTO quartiers (id, nom, zone_id) VALUES
(1, 'Plateau', 1), (2, 'Médina', 1), (3, 'Fass', 1),
(4, 'Fann', 2), (5, 'Point E', 2), (6, 'Fann Résidence', 2), (7, 'Mermoz', 2),
(8, 'Ouakam', 3), (9, 'Ngor', 3), (10, 'Almadies', 3), (11, 'Mamelles', 3),
(12, 'Yoff', 4), (13, 'Nord Foire', 4), (14, 'Ouest Foire', 4), (15, 'Sud Foire', 4),
(16, 'Grand Dakar', 5), (17, 'Biscuiterie', 5), (18, 'HLM', 5), (19, 'Dieuppeul-Derklé', 5),
(20, 'Sicap Liberté', 6), (21, 'Sacré-Cœur', 6), (22, 'Karack', 6),
(23, 'Parcelles Assainies', 7), (24, 'Cambérène', 7), (25, 'Grand Yoff', 7),
(26, 'Guédiawaye', 8), (27, 'Golf Sud', 8), (28, 'Ndiarème', 8), (29, 'Wakhinane', 8),
(30, 'Pikine', 9), (31, 'Thiaroye', 9), (32, 'Diamaguène', 9), (33, 'Mbao', 9),
(34, 'Rufisque', 10), (35, 'Bargny', 10), (36, 'Diamniadio', 10),
(37, 'Keur Massar', 11), (38, 'Malika', 11), (39, 'Jaxaay', 11)
ON CONFLICT (id) DO NOTHING;

-- Centre approximatif de chaque quartier, pour ouvrir les cartes au bon endroit.
-- Chaque centre est le point le plus intérieur du contour de la commune correspondante
-- (contours OpenStreetMap, non officiels). Plusieurs quartiers partagent une même commune
-- (Fann / Point E / Fann Résidence, Mermoz / Sacré-Cœur, Ngor / Almadies, les trois Foire) :
-- ils ont donc le même centre. Un UPDATE peut être rejoué sans risque à chaque démarrage.
UPDATE quartiers AS q
SET latitude = v.latitude, longitude = v.longitude
FROM (VALUES
  (1, 14.6732, -17.4393),
  (2, 14.6832, -17.4476),
  (3, 14.6874, -17.456),
  (4, 14.6971, -17.4601),
  (5, 14.6971, -17.4601),
  (6, 14.6971, -17.4601),
  (7, 14.7078, -17.4733),
  (8, 14.724, -17.4923),
  (9, 14.7435, -17.5128),
  (10, 14.7435, -17.5128),
  (11, 14.724, -17.4914),
  (12, 14.7448, -17.4855),
  (13, 14.7455, -17.4855),
  (14, 14.7455, -17.4855),
  (15, 14.7455, -17.4855),
  (16, 14.7069, -17.4557),
  (17, 14.7127, -17.4501),
  (18, 14.7211, -17.4439),
  (19, 14.723, -17.4523),
  (20, 14.7239, -17.4649),
  (21, 14.7078, -17.4733),
  (22, 14.7239, -17.4651),
  (23, 14.7589, -17.442),
  (24, 14.7692, -17.4268),
  (25, 14.7381, -17.4629),
  (26, 14.7851, -17.3733),
  (27, 14.766, -17.4164),
  (28, 14.7843, -17.3869),
  (29, 14.7851, -17.3735),
  (30, 14.7549, -17.4092),
  (31, 14.7573, -17.3737),
  (32, 14.7567, -17.3531),
  (33, 14.7487, -17.3206),
  (34, 14.7324, -17.2965),
  (35, 14.7062, -17.2347),
  (36, 14.7356, -17.2036),
  (37, 14.7961, -17.3064),
  (38, 14.7978, -17.3356),
  (39, 14.7681, -17.2874)
) AS v(id, latitude, longitude)
WHERE q.id = v.id;

-- Comptes par défaut
-- Mot de passe en clair pour TOUS les comptes ci-dessous : password123

INSERT INTO users (id, email, password, role, tenant_id, zone_id, active) VALUES
(1, 'admin@pharmagarde.sn', '$2b$10$wrUFH0YVrcniwonlzLFgQO3bF98R48TacfydNHXTBE.f7RlsQr/26', 'ADMIN', NULL, NULL, true)
ON CONFLICT (id) DO NOTHING;

INSERT INTO users (id, email, password, role, tenant_id, zone_id, active) VALUES
(2, 'superviseur@pharmagarde.sn', '$2b$10$wrUFH0YVrcniwonlzLFgQO3bF98R48TacfydNHXTBE.f7RlsQr/26', 'SUPERVISEUR', NULL, NULL, true)
ON CONFLICT (id) DO NOTHING;

INSERT INTO users (id, email, password, role, tenant_id, zone_id, active) VALUES
(3, 'superviseur.plateau@pharmagarde.sn', '$2b$10$wrUFH0YVrcniwonlzLFgQO3bF98R48TacfydNHXTBE.f7RlsQr/26', 'SUPERVISEUR', NULL, 1, true)
ON CONFLICT (id) DO NOTHING;

INSERT INTO users (id, email, password, role, tenant_id, zone_id, active) VALUES
(4, 'pharmacien@pharmagarde.sn', '$2b$10$wrUFH0YVrcniwonlzLFgQO3bF98R48TacfydNHXTBE.f7RlsQr/26', 'PHARMACIEN', 4, NULL, true)
ON CONFLICT (id) DO NOTHING;

INSERT INTO users (id, email, password, role, tenant_id, zone_id, active) VALUES
(6, 'pharmacien2@pharmagarde.sn', '$2b$10$wrUFH0YVrcniwonlzLFgQO3bF98R48TacfydNHXTBE.f7RlsQr/26', 'PHARMACIEN', 6, NULL, true)
ON CONFLICT (id) DO NOTHING;

-- Pharmacie de test
INSERT INTO pharmacies (id, nom, adresse, quartier_id, zone_id, latitude, longitude, telephone, horaire_ouverture, horaire_fermeture, active, tenant_user_id, numero_agrement_ordre, valide_par_user_id, valide_le) VALUES
(1, 'Pharmacie du Plateau', 'Avenue Léopold Sédar Senghor, Plateau', 1, 1, 14.6708, -17.4313, '+221338210001', '08:00', '23:00', true, 4, 'ONP-SN-2024-001', 3, '2026-08-01 09:00:00')
ON CONFLICT (id) DO NOTHING;

-- Remise à niveau des séquences
SELECT setval('zones_id_seq', (SELECT MAX(id) FROM zones));
SELECT setval('quartiers_id_seq', (SELECT MAX(id) FROM quartiers));
SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));
SELECT setval('pharmacies_id_seq', (SELECT MAX(id) FROM pharmacies));

-- Jeu de démonstration : pharmacies fictives réparties dans les zones, pour que le tableau de
-- bord du superviseur, la page de modération et la carte soient parlants.
-- Tout est inventé : noms, numéros de téléphone (+221 33 000 00 xx), numéros d'agrément
-- (ONP-SN-DEMO-xxx) et positions (à l'intérieur de la commune, pas à une vraie adresse).
-- Chaque compte a pour mot de passe password123.
-- Ce bloc doit rester APRES les lignes SELECT setval ci-dessus : elles remettent les compteurs
-- d'identifiants à jour, et les lignes ci-dessous laissent la base choisir leurs identifiants.
-- Il peut être rejoué à chaque démarrage sans doublon : une ligne déjà présente est ignorée, et
-- une ligne supprimée (par exemple par un superviseur pendant une démonstration) est recréée.
-- À retirer, ou à déplacer dans un fichier réservé au profil dev, avant une mise en production.

-- Comptes pharmaciens de démonstration
INSERT INTO users (email, password, role, active)
SELECT v.email, '$2b$10$wrUFH0YVrcniwonlzLFgQO3bF98R48TacfydNHXTBE.f7RlsQr/26', 'PHARMACIEN', true
FROM (VALUES
  ('demo.teranga@pharmagarde.sn'),
  ('demo.mouettes@pharmagarde.sn'),
  ('demo.baobab@pharmagarde.sn'),
  ('demo.littoral@pharmagarde.sn'),
  ('demo.filaos@pharmagarde.sn'),
  ('demo.golf@pharmagarde.sn'),
  ('demo.kergui@pharmagarde.sn'),
  ('demo.manguiers@pharmagarde.sn'),
  ('demo.aurore@pharmagarde.sn'),
  ('demo.alizes@pharmagarde.sn'),
  ('demo.bidon@pharmagarde.sn')
) AS v(email)
WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.email = v.email);

-- Un pharmacien est son propre tenant : son tenant_id est son propre identifiant, connu seulement
-- une fois le compte créé
UPDATE users SET tenant_id = id
WHERE role = 'PHARMACIEN' AND tenant_id IS NULL AND email LIKE 'demo.%@pharmagarde.sn';

-- Leurs fiches : 9 validées, 2 en attente (dont une manifestement bidon, sans position, pour
-- démontrer la suppression). La zone se déduit du quartier, le valideur est le superviseur
-- général, et "jours" est l'ancienneté de la validation.
INSERT INTO pharmacies (nom, adresse, quartier_id, zone_id, latitude, longitude, telephone,
                        horaire_ouverture, horaire_fermeture, active, tenant_user_id,
                        numero_agrement_ordre, valide_par_user_id, valide_le)
SELECT v.nom, v.adresse, q.id, q.zone_id, v.latitude, v.longitude, v.telephone,
       v.ouverture, v.fermeture, v.validee, u.id, v.agrement,
       CASE WHEN v.validee THEN (SELECT s.id FROM users s WHERE s.email = 'superviseur@pharmagarde.sn') END,
       CASE WHEN v.validee THEN CURRENT_TIMESTAMP - make_interval(days => v.jours) END
FROM (VALUES
  ('demo.teranga@pharmagarde.sn',   'Pharmacie Teranga',         'Rue 10, Point E',                        5,  14.6985, -17.4588, '+221 33 000 00 01', '08:00', '23:00', TRUE,  'ONP-SN-DEMO-001',  2),
  ('demo.mouettes@pharmagarde.sn',  'Pharmacie Les Mouettes',    'Corniche Ouest, Fann Résidence',         6,  14.6960, -17.4615, '+221 33 000 00 02', '07:30', '22:00', TRUE,  'ONP-SN-DEMO-002', 20),
  ('demo.baobab@pharmagarde.sn',    'Pharmacie Baobab',          'Cité HLM 5, près du marché',             18, 14.7216, -17.4433, '+221 33 000 00 03', '08:00', '22:00', TRUE,  'ONP-SN-DEMO-003',  5),
  ('demo.littoral@pharmagarde.sn',  'Pharmacie du Littoral',     'Route de Yoff, près de la plage',        12, 14.7440, -17.4862, '+221 33 000 00 04', '08:00', '23:00', TRUE,  'ONP-SN-DEMO-004', 12),
  ('demo.filaos@pharmagarde.sn',    'Pharmacie Les Filaos',      'Unité 15, Parcelles Assainies',          23, 14.7594, -17.4413, '+221 33 000 00 05', '09:00', '22:00', TRUE,  'ONP-SN-DEMO-005',  1),
  ('demo.golf@pharmagarde.sn',      'Pharmacie du Golf',         'Route de Golf Sud, Guédiawaye',          27, 14.7665, -17.4158, '+221 33 000 00 06', '08:00', '21:00', TRUE,  'ONP-SN-DEMO-006', 30),
  ('demo.kergui@pharmagarde.sn',    'Pharmacie Keur Gui',        'Près du marché, Pikine',                 30, 14.7553, -17.4087, '+221 33 000 00 07', '08:30', '22:30', TRUE,  'ONP-SN-DEMO-007', 40),
  ('demo.manguiers@pharmagarde.sn', 'Pharmacie Les Manguiers',   'Route de Rufisque, centre-ville',        34, 14.7330, -17.2958, '+221 33 000 00 08', '08:00', '22:00', TRUE,  'ONP-SN-DEMO-008',  9),
  ('demo.aurore@pharmagarde.sn',    'Pharmacie Nouvelle Aurore', 'Route de Keur Massar, près du rond-point', 37, 14.7966, -17.3070, '+221 33 000 00 09', '08:00', '23:00', TRUE,  'ONP-SN-DEMO-009',  3),
  ('demo.alizes@pharmagarde.sn',    'Pharmacie Les Alizés',      'Malika, près de la mosquée',             38, 14.7982, -17.3350, '+221 33 000 00 10', '08:00', '22:00', FALSE, 'ONP-SN-DEMO-010',  0),
  ('demo.bidon@pharmagarde.sn',     'Pharmacie Bidon',           'Adresse inconnue',                       16, NULL,    NULL,     '+221 33 000 00 11', '08:00', '18:00', FALSE, '123',              0)
) AS v(email, nom, adresse, quartier_id, latitude, longitude, telephone, ouverture, fermeture, validee, agrement, jours)
JOIN users u ON u.email = v.email
JOIN quartiers q ON q.id = v.quartier_id
WHERE NOT EXISTS (SELECT 1 FROM pharmacies p WHERE p.tenant_user_id = u.id);

-- Gardes de démonstration, calculées à partir de la date du jour : elles couvrent donc toujours
-- aujourd'hui, quel que soit le jour où le backend démarre. "debut" et "fin" sont des décalages en
-- jours par rapport à aujourd'hui (-1 = hier, 0 = aujourd'hui, 5 = dans 5 jours).
-- Elles sont recalculées à chaque démarrage, pour les seuls comptes demo.* : les périodes que
-- ces comptes auraient déclarées pendant une démonstration sont donc effacées au redémarrage.
-- Volontairement, plusieurs zones restent sans garde aujourd'hui (Guédiawaye, Pikine) pour que le
-- tableau de bord du superviseur montre aussi l'état d'alerte.
-- Ce bloc doit rester APRES celui qui crée les pharmacies de démonstration.

DELETE FROM gardes
WHERE pharmacie_id IN (
  SELECT p.id
  FROM pharmacies p
  JOIN users u ON u.id = p.tenant_user_id
  WHERE u.email LIKE 'demo.%@pharmagarde.sn'
);

INSERT INTO gardes (pharmacie_id, date_debut, date_fin, type)
SELECT p.id, CURRENT_DATE + v.debut, CURRENT_DATE + v.fin, v.genre
FROM (VALUES
  -- Fann - Point E : Teranga est de garde aujourd'hui puis à nouveau dans 12 jours (2 périodes),
  -- Les Mouettes ne le sont que plus tard, donc une seule des deux pharmacies de la zone couvre aujourd'hui
  ('demo.teranga@pharmagarde.sn',   -1,  5, 'COMPLETE'),
  ('demo.teranga@pharmagarde.sn',   12, 14, 'NUIT'),
  ('demo.mouettes@pharmagarde.sn',   8, 10, 'JOUR'),
  -- Zones couvertes aujourd'hui
  ('demo.baobab@pharmagarde.sn',     0,  6, 'NUIT'),
  ('demo.littoral@pharmagarde.sn',  -2,  4, 'JOUR'),
  ('demo.filaos@pharmagarde.sn',    -1,  1, 'COMPLETE'),
  ('demo.manguiers@pharmagarde.sn',  0,  0, 'NUIT'),
  ('demo.aurore@pharmagarde.sn',    -3,  3, 'COMPLETE'),
  -- Validées mais sans garde aujourd'hui : une période passée (Guédiawaye), une à venir (Pikine)
  ('demo.golf@pharmagarde.sn',      -20, -14, 'COMPLETE'),
  ('demo.kergui@pharmagarde.sn',      2,   8, 'COMPLETE')
) AS v(email, debut, fin, genre)
JOIN users u ON u.email = v.email
JOIN pharmacies p ON p.tenant_user_id = u.id;