CREATE SCHEMA IF NOT EXISTS users;

CREATE TABLE users.users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    password_hash VARCHAR(255),
    display_name VARCHAR(100) NOT NULL,
    avatar_url VARCHAR(500),
    trust_tier VARCHAR(20) NOT NULL DEFAULT 'NEW',
    sys_period tstzrange NOT NULL DEFAULT tstzrange(NOW(), NULL),

    CONSTRAINT valid_trust_tier CHECK (
        trust_tier IN ('NEW', 'TRUSTED', 'MODERATOR', 'ADMIN')
    )
);

CREATE INDEX idx_users_email ON users.users(email);
CREATE INDEX idx_users_trust_tier ON users.users(trust_tier);
