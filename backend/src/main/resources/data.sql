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