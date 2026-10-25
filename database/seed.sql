-- seed.sql: sample data only. Run schema.sql first.
-- Run this ONCE on a fresh database. Running it again will fail on the
-- duplicate user emails and would duplicate the other rows.
--
-- Logins (email + password):
--   admin@school.edu / admin123
--   mcona_250000000763@uic.edu.ph / teacher123
-- CHANGE the teacher email to an address you can read, so you can see the Gmail notification.

USE hazard_tracker;

INSERT INTO users (name, password, email, role) VALUES
 ('Lab Admin',    SHA2('admin123',256),   'admin@school.edu',     'ADMIN'),
 ('Maria Santos', SHA2('teacher123',256), 'mcona_250000000763@uic.edu.ph', 'TEACHER');

INSERT INTO chemicals (name, quantity, unit, hazard_type, storage_group, storage_temperature, location, minimum_stock, expiration_date) VALUES
 ('Hydrochloric Acid', 1000, 'mL', 'Corrosive,Irritant', 'Acid',      '15-25°C', 'Cabinet A', 200, '2028-12-31'),
 ('Sodium Hydroxide',  1000, 'mL', 'Corrosive',          'Base',      '15-25°C', 'Cabinet B', 200, '2028-06-30'),
 ('Phenolphthalein',    100, 'mL', 'Irritant',           'Other',     '15-25°C', 'Cabinet C',  20, DATE_ADD(CURDATE(), INTERVAL 20 DAY)),
 ('Ethanol',            150, 'mL', 'Flammable',          'Flammable', '15-25°C', 'Cabinet D', 200, '2027-12-31'),
 ('Old Reagent X',       50, 'g',  'Toxic',              'Other',     '15-25°C', 'Cabinet C',  10, '2025-01-01');

INSERT INTO compatibility_rules (group_a, group_b) VALUES
 ('Acid','Base'),
 ('Oxidizer','Flammable');
