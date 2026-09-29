DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger
        WHERE tgname = 'trg_products_audit' AND tgrelid = 'products'::regclass
    ) THEN
        CREATE TRIGGER trg_products_audit
        AFTER INSERT OR UPDATE OR DELETE ON products
        FOR EACH ROW EXECUTE FUNCTION register_audit();
    END IF;
END;
$$;
