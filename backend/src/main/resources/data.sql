-- ========== ZONES DE GARDE (11 zones de Dakar) ==========
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

-- ========== QUARTIERS ==========
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

-- ========== COMPTES PAR DÉFAUT ==========
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

-- ========== PHARMACIE TEST ==========
INSERT INTO pharmacies (id, nom, adresse, quartier_id, zone_id, latitude, longitude, telephone, horaire_ouverture, horaire_fermeture, active, tenant_user_id, numero_agrement_ordre, valide_par_user_id, valide_le) VALUES
(1, 'Pharmacie du Plateau', 'Avenue Léopold Sédar Senghor, Plateau', 1, 1, 14.6708, -17.4313, '+221338210001', '08:00', '23:00', true, 4, 'ONP-SN-2024-001', 3, '2026-08-01 09:00:00')
ON CONFLICT (id) DO NOTHING;
-- Remise à niveau des séquences
SELECT setval('zones_id_seq', (SELECT MAX(id) FROM zones));
SELECT setval('quartiers_id_seq', (SELECT MAX(id) FROM quartiers));
SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));
SELECT setval('pharmacies_id_seq', (SELECT MAX(id) FROM pharmacies));