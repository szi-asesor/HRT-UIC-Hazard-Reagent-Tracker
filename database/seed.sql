-- Account records and 25 inventory entries for a fresh hazard_tracker database.
-- Run schema.sql first. Existing admin and teacher accounts are retained.
-- Logins: admin@school.edu / admin123
--         mcona_250000000763@uic.edu.ph / teacher123

USE hazard_tracker;

INSERT IGNORE INTO users (name, password, email, role) VALUES
 ('Lab Admin',    SHA2('admin123',256),   'admin@school.edu',     'ADMIN'),
 ('Maria Santos', SHA2('teacher123',256), 'mcona_250000000763@uic.edu.ph', 'TEACHER');

INSERT INTO chemicals
    (name, quantity, unit, hazard_type, storage_group, storage_temperature,
     location, minimum_stock, expiration_date)
VALUES
 ('Hydrochloric Acid',       1000, 'mL', 'Corrosive, Irritant',          'Acid',      '15-25°C', 'Acid Corrosive', 200, '2028-12-31'),
 ('Sodium Hydroxide',         1000, 'g',  'Corrosive',                    'Base',      '15-25°C', 'Base Corrosive', 200, '2028-06-30'),
 ('Phenolphthalein',           100, 'mL', 'Irritant',                     'Other',     '15-25°C', 'Others',          20, DATE_ADD(CURDATE(), INTERVAL 20 DAY)),
 ('Ethanol',                   150, 'mL', 'Flammable',                    'Flammable', '15-25°C', 'Flammable',      200, '2027-12-31'),
 ('Old Reagent X',              50, 'g',  'Toxic',                        'Other',     '15-25°C', 'Others',          10, '2025-01-01'),
 ('Methanol',                  350, 'mL', 'Flammable, Toxic',             'Flammable', '15-25°C', 'Flammable',      100, '2028-03-31'),
 ('Methylene Blue',            250, 'g',  'Irritant',                     'Other',     '15-25°C', 'Others',          50, '2029-08-31'),
 ('Glacial Acetic Acid',       500, 'mL', 'Corrosive, Flammable',         'Acid',      '15-25°C', 'Acid Corrosive', 150, '2028-11-30'),
 ('Ammonium Hydroxide',        600, 'mL', 'Corrosive, Irritant',          'Base',      '15-25°C', 'Base Corrosive', 150, '2028-05-31'),
 ('Hexane',                    800, 'mL', 'Flammable, Toxic',             'Flammable', '15-25°C', 'Flammable',      200, '2028-09-30'),
 ('Iodine',                    100, 'g',  'Toxic, Irritant',              'Other',     '15-25°C', 'Others',          25, '2029-08-31'),
 ('Nitric Acid',               800, 'mL', 'Corrosive, Oxidizer',          'Acid',      '15-25°C', 'Acid Corrosive', 200, '2028-10-31'),
 ('Acetone',                   500, 'mL', 'Flammable, Irritant',          'Flammable', '15-25°C', 'Flammable',      100, '2028-04-30'),
 ('Isopropyl Alcohol',         500, 'mL', 'Flammable, Irritant',          'Flammable', '15-25°C', 'Flammable',      100, '2028-07-31'),
 ('Sulfuric Acid',             250, 'mL', 'Corrosive',                    'Acid',      '15-25°C', 'Acid Corrosive', 100, '2028-12-31'),
 ('Phosphoric Acid',           300, 'mL', 'Corrosive',                    'Acid',      '15-25°C', 'Acid Corrosive', 100, '2029-02-28'),
 ('Potassium Hydroxide',       300, 'g',  'Corrosive',                    'Base',      '15-25°C', 'Base Corrosive', 100, '2028-08-31'),
 ('Sodium Bicarbonate',        500, 'g',  'None',                         'Other',     '15-25°C', 'Others',         100, '2029-12-31'),
 ('Sodium Chloride',          1000, 'g',  'None',                         'None',      '15-25°C', 'None',           200, '2030-01-31'),
 ('Copper Sulfate',            250, 'g',  'Toxic, Irritant',              'Other',     '15-25°C', 'Others',          50, '2029-04-30'),
 ('Potassium Permanganate',    250, 'g',  'Oxidizer, Irritant',           'Oxidizer',  '15-25°C', 'Oxidizer',        50, '2029-06-30'),
 ('Hydrogen Peroxide 3%',      500, 'mL', 'Oxidizer, Irritant',           'Oxidizer',  '15-25°C', 'Oxidizer',       100, '2028-02-29'),
 ('Sodium Nitrate',            500, 'g',  'Oxidizer, Irritant',           'Oxidizer',  '15-25°C', 'Oxidizer',       100, '2029-05-31'),
 ('Glycerol',                  250, 'mL', 'Organic Material',             'Other',     '15-25°C', 'Others',          50, '2029-10-31'),
 ('Urea',                      500, 'g',  'Irritant',                     'Other',     '15-25°C', 'Others',         100, '2029-11-30');
