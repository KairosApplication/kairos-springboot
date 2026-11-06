CREATE TABLE customer_damage_reports (
    id uuid PRIMARY KEY,
    command_id uuid NOT NULL,
    customer_id integer NOT NULL REFERENCES customers(id),
    company_id integer NOT NULL REFERENCES companies(id),
    product_id integer NOT NULL REFERENCES products(id),
    shelf_id integer REFERENCES shelves(id),
    problem_type varchar(30) NOT NULL CHECK (problem_type IN ('DAMAGED','TAMPERED','EXPIRED','OTHER')),
    description varchar(500) NOT NULL DEFAULT '',
    photo bytea CHECK (photo IS NULL OR octet_length(photo) <= 3145728),
    fingerprint varchar(64) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    alert_id integer NOT NULL UNIQUE REFERENCES alerts(id),
    UNIQUE (customer_id,command_id)
);
CREATE INDEX customer_damage_reports_owner ON customer_damage_reports(customer_id,created_at DESC);

CREATE FUNCTION create_customer_damage_alert() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE target_shelf integer; target_employee integer;
BEGIN
    IF NOT EXISTS (SELECT 1 FROM customers WHERE id=NEW.customer_id AND company_id=NEW.company_id)
       OR NOT EXISTS (SELECT 1 FROM products WHERE id=NEW.product_id AND company_id=NEW.company_id)
    THEN RAISE EXCEPTION 'Customer report scope mismatch' USING ERRCODE='23514'; END IF;

    SELECT sh.id,e.id INTO target_shelf,target_employee
    FROM product_shelves ps JOIN shelves sh ON sh.id=ps.shelf_id
    JOIN aisles a ON a.id=sh.aisle_id JOIN sectors s ON s.id=a.sector_id
    JOIN employees e ON e.company_id=s.company_id AND e.position='STOCKER'
    WHERE ps.product_id=NEW.product_id AND s.company_id=NEW.company_id
      AND (NEW.shelf_id IS NULL OR sh.id=NEW.shelf_id)
      AND (EXISTS (SELECT 1 FROM shelf_employees se WHERE se.shelf_id=sh.id AND se.employee_id=e.id)
        OR EXISTS (SELECT 1 FROM employee_aisles ea WHERE ea.aisle_id=a.id AND ea.employee_id=e.id))
    ORDER BY CASE WHEN EXISTS (SELECT 1 FROM shelf_employees se
        WHERE se.shelf_id=sh.id AND se.employee_id=e.id) THEN 0 ELSE 1 END,sh.id,e.id LIMIT 1;

    IF target_employee IS NULL THEN
        RAISE EXCEPTION 'No assigned stocker for this product/location' USING ERRCODE='23514';
    END IF;
    INSERT INTO alerts(stockout_date,employee_id,shelf_id,product_id,description,status)
    VALUES (CURRENT_DATE,target_employee,target_shelf,NEW.product_id,
        LEFT('Relato de cliente - ' || NEW.problem_type || ': ' ||
            CASE WHEN NEW.shelf_id IS NULL THEN '[Local nao informado] ' ELSE '' END ||
            NEW.description,250),'OPEN') RETURNING id INTO NEW.alert_id;
    RETURN NEW;
END $$;
CREATE TRIGGER customer_damage_alert BEFORE INSERT ON customer_damage_reports
FOR EACH ROW EXECUTE FUNCTION create_customer_damage_alert();
