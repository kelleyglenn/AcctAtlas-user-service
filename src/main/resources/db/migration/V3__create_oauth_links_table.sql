CREATE TABLE users.oauth_links (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users.users(id) ON DELETE CASCADE,
    provider VARCHAR(20) NOT NULL,
    provider_id VARCHAR(255) NOT NULL,
    sys_period tstzrange NOT NULL DEFAULT tstzrange(NOW(), NULL),

    CONSTRAINT valid_provider CHECK (provider IN ('GOOGLE', 'APPLE')),
    UNIQUE (provider, provider_id)
);

CREATE INDEX idx_oauth_links_user ON users.oauth_links(user_id);
