-- Public fixtures, exclusively for compose.dev.yaml's isolated local database.
BEGIN;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO companies (name, cnpj, email, plan)
VALUES ('Kairos Desenvolvimento', '11222333000181', 'company@kairos.local', 'BASIC')
ON CONFLICT (email) DO NOTHING;

INSERT INTO users (name, last_name, birth_date, cpf, email, password)
VALUES
    ('Gerente', 'Desenvolvimento', '1990-01-01', '52998224725', 'manager@kairos.local',
        '{bcrypt}' || crypt('Kairos-Dev-Only!123', gen_salt('bf', 10))),
    ('Cliente', 'Desenvolvimento', '2000-01-01', '11144477735', 'customer@kairos.local',
        '{bcrypt}' || crypt('Kairos-Dev-Only!123', gen_salt('bf', 10)))
ON CONFLICT (email) DO NOTHING;

INSERT INTO employees (users_id, company_id, position)
SELECT u.id, c.id, 'MANAGER'
FROM users u CROSS JOIN companies c
WHERE u.email = 'manager@kairos.local' AND c.email = 'company@kairos.local'
ON CONFLICT (users_id) DO NOTHING;

INSERT INTO customers (users_id, company_id)
SELECT u.id, c.id
FROM users u CROSS JOIN companies c
WHERE u.email = 'customer@kairos.local' AND c.email = 'company@kairos.local'
ON CONFLICT (users_id) DO NOTHING;

INSERT INTO users (name, last_name, birth_date, cpf, email, password)
VALUES ('Repositor', 'Desenvolvimento', '2000-01-01', '12345678909', 'stocker@kairos.local',
    '{bcrypt}' || crypt('Kairos-Dev-Only!123', gen_salt('bf', 10)))
ON CONFLICT (email) DO NOTHING;
INSERT INTO employees (users_id, company_id, position)
SELECT u.id, c.id, 'STOCKER' FROM users u CROSS JOIN companies c
WHERE u.email = 'stocker@kairos.local' AND c.email = 'company@kairos.local'
ON CONFLICT (users_id) DO NOTHING;
INSERT INTO sectors (company_id, name, type)
SELECT id, 'Setor desenvolvimento', 'Estoque' FROM companies WHERE email = 'company@kairos.local'
ON CONFLICT (company_id, name) DO NOTHING;
INSERT INTO aisles (sector_id, position)
SELECT id, 1 FROM sectors WHERE name = 'Setor desenvolvimento'
ON CONFLICT (sector_id, position) DO NOTHING;
INSERT INTO shelves (aisle_id, maximum_capacity)
SELECT a.id, 100 FROM aisles a JOIN sectors s ON s.id = a.sector_id
WHERE s.name = 'Setor desenvolvimento' AND NOT EXISTS (SELECT 1 FROM shelves sh WHERE sh.aisle_id = a.id);
INSERT INTO inventories (company_id, sector_id)
SELECT company_id, id FROM sectors s WHERE name = 'Setor desenvolvimento'
AND NOT EXISTS (SELECT 1 FROM inventories i WHERE i.sector_id = s.id);
INSERT INTO categories (company_id, category)
SELECT id, 'Desenvolvimento' FROM companies WHERE email = 'company@kairos.local'
ON CONFLICT (company_id, category) DO NOTHING;
INSERT INTO products (company_id, brand, price, name, category_id)
SELECT company_id, 'Kairos', 10::money, 'Produto desenvolvimento', id FROM categories c
WHERE category = 'Desenvolvimento' AND NOT EXISTS
(SELECT 1 FROM products p WHERE p.company_id = c.company_id AND p.name = 'Produto desenvolvimento');
INSERT INTO product_inventories (inventory_id, product_id, product_quantity)
SELECT i.id, p.id, 1000 FROM inventories i JOIN products p ON p.company_id = i.company_id
JOIN companies c ON c.id = i.company_id
WHERE c.email = 'company@kairos.local' AND p.name = 'Produto desenvolvimento'
ON CONFLICT (inventory_id, product_id) DO NOTHING;
COMMIT;
