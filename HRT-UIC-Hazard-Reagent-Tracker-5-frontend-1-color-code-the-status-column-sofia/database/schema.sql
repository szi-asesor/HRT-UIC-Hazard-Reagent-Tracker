DROP DATABASE IF EXISTS hazard_tracker;
CREATE DATABASE hazard_tracker;
USE hazard_tracker;

CREATE TABLE users (
    id        INT AUTO_INCREMENT PRIMARY KEY,
    name      VARCHAR(100) NOT NULL,
    password  VARCHAR(64)  NOT NULL,              -- SHA-256 hash
    email     VARCHAR(100) NOT NULL UNIQUE,      -- the email IS the login
    role      ENUM('ADMIN','TEACHER') NOT NULL
);

CREATE TABLE chemicals (
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    name                VARCHAR(100) NOT NULL,
    quantity            DOUBLE NOT NULL DEFAULT 0,
    unit                VARCHAR(10)  NOT NULL,
    hazard_type         VARCHAR(100) NOT NULL DEFAULT 'None',  -- e.g. 'Corrosive,Irritant'
    storage_group       VARCHAR(20)  NOT NULL,
    storage_temperature VARCHAR(30),
    location            VARCHAR(50)  NOT NULL,
    minimum_stock       DOUBLE NOT NULL DEFAULT 0,
    expiration_date     DATE,
    active              BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE chemical_requests (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    teacher_id       INT NOT NULL,
    class_name       VARCHAR(50)  NOT NULL,
    experiment       VARCHAR(150) NOT NULL,
    status           ENUM('PENDING','APPROVED','REJECTED','CANCELLED') NOT NULL DEFAULT 'PENDING',
    rejection_reason VARCHAR(255),
    request_date     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (teacher_id) REFERENCES users(id)
);

CREATE TABLE request_items (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    request_id  INT NOT NULL,
    chemical_id INT NOT NULL,
    quantity    DOUBLE NOT NULL,
    FOREIGN KEY (request_id)  REFERENCES chemical_requests(id),
    FOREIGN KEY (chemical_id) REFERENCES chemicals(id)
);

CREATE TABLE transactions (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    chemical_id      INT NOT NULL,
    user_id          INT NOT NULL,
    type             ENUM('ADD','RELEASE','ADJUSTMENT') NOT NULL,
    quantity         DOUBLE NOT NULL,
    transaction_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (chemical_id) REFERENCES chemicals(id),
    FOREIGN KEY (user_id)     REFERENCES users(id)
);

CREATE TABLE compatibility_rules (
    id      INT AUTO_INCREMENT PRIMARY KEY,
    group_a VARCHAR(20) NOT NULL,
    group_b VARCHAR(20) NOT NULL
);

-- ---------- Sample data ----------
-- Logins (email + password):  admin@school.edu / admin123     your.email@gmail.com / teacher123
-- CHANGE the teacher email to an address you can read, so you can see the Gmail notification.
INSERT INTO users (name, password, email, role) VALUES
 ('Lab Admin',    SHA2('admin123',256),   'admin@school.edu',     'ADMIN'),
 ('Maria Santos', SHA2('teacher123',256), 'your.email@gmail.com', 'TEACHER');

INSERT INTO chemicals (name, quantity, unit, hazard_type, storage_group, storage_temperature, location, minimum_stock, expiration_date) VALUES
 ('Hydrochloric Acid', 1000, 'mL', 'Corrosive,Irritant', 'Acid',      '15-25°C', 'Cabinet A', 200, '2028-12-31'),
 ('Sodium Hydroxide',  1000, 'mL', 'Corrosive',          'Base',      '15-25°C', 'Cabinet B', 200, '2028-06-30'),
 ('Phenolphthalein',    100, 'mL', 'Irritant',           'Other',     '15-25°C', 'Cabinet C',  20, DATE_ADD(CURDATE(), INTERVAL 20 DAY)),
 ('Ethanol',            150, 'mL', 'Flammable',          'Flammable', '15-25°C', 'Cabinet D', 200, '2027-12-31'),
 ('Old Reagent X',       50, 'g',  'Toxic',              'Other',     '15-25°C', 'Cabinet C',  10, '2025-01-01');

INSERT INTO compatibility_rules (group_a, group_b) VALUES
 ('Acid','Base'),
 ('Oxidizer','Flammable');
