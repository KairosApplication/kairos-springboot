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
COMMIT;
