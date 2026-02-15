-- Dev seed data: Test users for local development
-- Password for all users: password123
-- BCrypt hash generated with cost factor 10

INSERT INTO users.users (id, email, email_verified, password_hash, display_name, trust_tier)
VALUES
    ('00000000-0000-0000-0000-000000000001', 'admin@example.com', true,
     '$2a$10$TQDQFlm7TjGqo5kUE/6/BusR7/mFLfMru3veFtfVQ5CmChTuP1F16', 'Admin User', 'ADMIN'),
    ('00000000-0000-0000-0000-000000000002', 'moderator@example.com', true,
     '$2a$10$TQDQFlm7TjGqo5kUE/6/BusR7/mFLfMru3veFtfVQ5CmChTuP1F16', 'Moderator User', 'MODERATOR'),
    ('00000000-0000-0000-0000-000000000003', 'trusted@example.com', true,
     '$2a$10$TQDQFlm7TjGqo5kUE/6/BusR7/mFLfMru3veFtfVQ5CmChTuP1F16', 'Trusted User', 'TRUSTED'),
    ('00000000-0000-0000-0000-000000000004', 'newuser@example.com', true,
     '$2a$10$TQDQFlm7TjGqo5kUE/6/BusR7/mFLfMru3veFtfVQ5CmChTuP1F16', 'New User', 'NEW')
ON CONFLICT (email) DO UPDATE SET
    password_hash = EXCLUDED.password_hash,
    display_name = EXCLUDED.display_name,
    trust_tier = EXCLUDED.trust_tier;

-- Create corresponding user_stats entries
INSERT INTO users.user_stats (user_id, submission_count, approved_count, rejected_count)
VALUES
    ('00000000-0000-0000-0000-000000000001', 0, 0, 0),
    ('00000000-0000-0000-0000-000000000002', 0, 0, 0),
    ('00000000-0000-0000-0000-000000000003', 10, 8, 2),
    ('00000000-0000-0000-0000-000000000004', 0, 0, 0)
ON CONFLICT (user_id) DO NOTHING;
