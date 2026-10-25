-- Use this ONLY if you already ran the old schema.sql and want to keep your data.
-- (Running the new schema.sql instead wipes the database and recreates it.)
USE hazard_tracker;
ALTER TABLE users DROP COLUMN username;
ALTER TABLE users ADD UNIQUE KEY uq_users_email (email);
ALTER TABLE chemical_requests
  MODIFY status ENUM('PENDING','APPROVED','REJECTED','CANCELLED') NOT NULL DEFAULT 'PENDING';
