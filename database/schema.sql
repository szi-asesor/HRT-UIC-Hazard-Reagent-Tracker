-- schema.sql: creates the database and tables ONLY.
-- Safe to re-run: it never drops anything, so existing data is kept.
-- Load sample data separately with seed.sql.

CREATE DATABASE IF NOT EXISTS hazard_tracker;
USE hazard_tracker;

CREATE TABLE IF NOT EXISTS users (
    id        INT AUTO_INCREMENT PRIMARY KEY,
    name      VARCHAR(100) NOT NULL,
    password  VARCHAR(64)  NOT NULL,              -- SHA-256 hash
    email     VARCHAR(100) NOT NULL UNIQUE,       -- the email IS the login
    role      ENUM('ADMIN','TEACHER') NOT NULL
);

CREATE TABLE IF NOT EXISTS chemicals (
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

CREATE TABLE IF NOT EXISTS chemical_requests (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    teacher_id       INT NOT NULL,
    class_name       VARCHAR(50)  NOT NULL,
    experiment       VARCHAR(150) NOT NULL,
    status           ENUM('PENDING','APPROVED','REJECTED','CANCELLED') NOT NULL DEFAULT 'PENDING',
    rejection_reason VARCHAR(255),
    request_date     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (teacher_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS request_items (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    request_id  INT NOT NULL,
    chemical_id INT NOT NULL,
    quantity    DOUBLE NOT NULL,
    FOREIGN KEY (request_id)  REFERENCES chemical_requests(id),
    FOREIGN KEY (chemical_id) REFERENCES chemicals(id)
);

CREATE TABLE IF NOT EXISTS transactions (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    chemical_id      INT NOT NULL,
    user_id          INT NOT NULL,
    type             ENUM('ADD','RELEASE','ADJUSTMENT') NOT NULL,
    quantity         DOUBLE NOT NULL,
    transaction_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (chemical_id) REFERENCES chemicals(id),
    FOREIGN KEY (user_id)     REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS compatibility_rules (
    id      INT AUTO_INCREMENT PRIMARY KEY,
    group_a VARCHAR(20) NOT NULL,
    group_b VARCHAR(20) NOT NULL
);
