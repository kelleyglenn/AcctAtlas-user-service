-- Social links table (1:1 with users, temporal)

CREATE TABLE IF NOT EXISTS users.user_social_links (
    user_id UUID PRIMARY KEY REFERENCES users.users(id) ON DELETE CASCADE,
    youtube VARCHAR(100),
    facebook VARCHAR(100),
    instagram VARCHAR(50),
    tiktok VARCHAR(50),
    x_twitter VARCHAR(50),
    bluesky VARCHAR(100),
    sys_period tstzrange NOT NULL DEFAULT tstzrange(current_timestamp, null)
);

CREATE TABLE IF NOT EXISTS users.user_social_links_history (LIKE users.user_social_links);

CREATE TRIGGER user_social_links_versioning
    BEFORE INSERT OR UPDATE OR DELETE ON users.user_social_links
    FOR EACH ROW EXECUTE FUNCTION users.versioning_trigger_fn('users.user_social_links_history');

-- Privacy settings table (1:1 with users, temporal)

CREATE TABLE IF NOT EXISTS users.user_privacy_settings (
    user_id UUID PRIMARY KEY REFERENCES users.users(id) ON DELETE CASCADE,
    social_links_visibility VARCHAR(20) NOT NULL DEFAULT 'REGISTERED',
    submissions_visibility VARCHAR(20) NOT NULL DEFAULT 'PUBLIC',
    sys_period tstzrange NOT NULL DEFAULT tstzrange(current_timestamp, null),
    CONSTRAINT valid_social_links_visibility CHECK (social_links_visibility IN ('PUBLIC', 'REGISTERED')),
    CONSTRAINT valid_submissions_visibility CHECK (submissions_visibility IN ('PUBLIC', 'REGISTERED'))
);

CREATE TABLE IF NOT EXISTS users.user_privacy_settings_history (LIKE users.user_privacy_settings);

CREATE TRIGGER user_privacy_settings_versioning
    BEFORE INSERT OR UPDATE OR DELETE ON users.user_privacy_settings
    FOR EACH ROW EXECUTE FUNCTION users.versioning_trigger_fn('users.user_privacy_settings_history');

COMMENT ON TABLE users.user_social_links IS 'User social media profile links (1:1 with users)';
COMMENT ON TABLE users.user_privacy_settings IS 'Per-section privacy visibility settings (1:1 with users)';
