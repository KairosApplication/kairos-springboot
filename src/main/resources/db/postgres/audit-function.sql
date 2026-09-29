CREATE OR REPLACE FUNCTION register_audit()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        INSERT INTO audits (table_name, operation, old_data, new_data, record_id, users)
        VALUES (TG_TABLE_NAME, TG_OP, NULL, TO_JSONB(NEW), NEW.id, CURRENT_USER);
        RETURN NEW;
    ELSIF TG_OP = 'UPDATE' THEN
        INSERT INTO audits (table_name, operation, old_data, new_data, record_id, users)
        VALUES (TG_TABLE_NAME, TG_OP, TO_JSONB(OLD), TO_JSONB(NEW), NEW.id, CURRENT_USER);
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        INSERT INTO audits (table_name, operation, old_data, new_data, record_id, users)
        VALUES (TG_TABLE_NAME, TG_OP, TO_JSONB(OLD), NULL, OLD.id, CURRENT_USER);
        RETURN OLD;
    END IF;
    RETURN NULL;
END;
$$;
