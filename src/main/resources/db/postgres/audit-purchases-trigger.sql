DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger
        WHERE tgname = 'trg_purchases_audit' AND tgrelid = 'purchases'::regclass
    ) THEN
        CREATE TRIGGER trg_purchases_audit
        AFTER INSERT OR UPDATE OR DELETE ON purchases
        FOR EACH ROW EXECUTE FUNCTION register_audit();
    END IF;
END;
$$;
