-- History tables
CREATE TABLE users.users_history (LIKE users.users);
CREATE TABLE users.oauth_links_history (LIKE users.oauth_links);

-- Generic versioning function
CREATE OR REPLACE FUNCTION users.versioning_trigger_fn()
RETURNS TRIGGER AS $$
DECLARE
    history_table TEXT;
BEGIN
    history_table := TG_ARGV[0];

    IF TG_OP = 'UPDATE' OR TG_OP = 'DELETE' THEN
        OLD.sys_period := tstzrange(lower(OLD.sys_period), NOW());
        EXECUTE format('INSERT INTO %s SELECT $1.*', history_table) USING OLD;
    END IF;

    IF TG_OP = 'UPDATE' THEN
        NEW.sys_period := tstzrange(NOW(), NULL);
        RETURN NEW;
    END IF;

    IF TG_OP = 'INSERT' THEN
        NEW.sys_period := tstzrange(NOW(), NULL);
        RETURN NEW;
    END IF;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER users_versioning_trigger
    BEFORE INSERT OR UPDATE OR DELETE ON users.users
    FOR EACH ROW EXECUTE FUNCTION users.versioning_trigger_fn('users.users_history');

CREATE TRIGGER oauth_links_versioning_trigger
    BEFORE INSERT OR UPDATE OR DELETE ON users.oauth_links
    FOR EACH ROW EXECUTE FUNCTION users.versioning_trigger_fn('users.oauth_links_history');
