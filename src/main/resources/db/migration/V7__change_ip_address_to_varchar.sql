ALTER TABLE users.sessions ALTER COLUMN ip_address TYPE VARCHAR(45) USING ip_address::VARCHAR;
