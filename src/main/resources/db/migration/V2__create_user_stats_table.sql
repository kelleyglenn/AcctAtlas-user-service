CREATE TABLE users.user_stats (
    user_id UUID PRIMARY KEY REFERENCES users.users(id) ON DELETE CASCADE,
    submission_count INTEGER NOT NULL DEFAULT 0,
    approved_count INTEGER NOT NULL DEFAULT 0,
    rejected_count INTEGER NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
