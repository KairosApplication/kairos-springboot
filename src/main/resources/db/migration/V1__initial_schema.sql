-- Baseline for a new PostgreSQL database. V2 adds the users CPF/email constraints.
CREATE TABLE companies (
    id serial PRIMARY KEY,
    name varchar(100) NOT NULL,
    cnpj varchar(18) NOT NULL UNIQUE,
    email varchar(255) NOT NULL UNIQUE,
    plan varchar(20) NOT NULL
);

CREATE TABLE users (
    id serial PRIMARY KEY,
    name varchar(100) NOT NULL,
    last_name varchar(100) NOT NULL,
    birth_date date NOT NULL,
    cpf varchar(14) NOT NULL,
    email varchar(255) NOT NULL,
    password varchar(255) NOT NULL
);

CREATE TABLE sectors (
    id serial PRIMARY KEY,
    company_id integer NOT NULL REFERENCES companies(id),
    name varchar(100) NOT NULL,
    type varchar NOT NULL,
    UNIQUE (company_id, name)
);

CREATE TABLE aisles (
    id serial PRIMARY KEY,
    sector_id integer NOT NULL REFERENCES sectors(id),
    position integer NOT NULL CHECK (position > 0),
    UNIQUE (sector_id, position)
);

CREATE TABLE shelves (
    id serial PRIMARY KEY,
    aisle_id integer NOT NULL REFERENCES aisles(id),
    maximum_capacity integer NOT NULL CHECK (maximum_capacity > 0)
);

CREATE TABLE employees (
    id serial PRIMARY KEY,
    users_id integer NOT NULL UNIQUE REFERENCES users(id),
    company_id integer NOT NULL REFERENCES companies(id),
    position varchar(100) NOT NULL
);

CREATE TABLE inventories (
    id serial PRIMARY KEY,
    company_id integer NOT NULL REFERENCES companies(id),
    sector_id integer REFERENCES sectors(id)
);

CREATE TABLE inventory_employees (
    id serial PRIMARY KEY,
    inventory_id integer NOT NULL REFERENCES inventories(id),
    employee_id integer NOT NULL REFERENCES employees(id),
    UNIQUE (inventory_id, employee_id)
);

CREATE TABLE shelf_employees (
    id serial PRIMARY KEY,
    shelf_id integer REFERENCES shelves(id),
    employee_id integer REFERENCES employees(id),
    UNIQUE (shelf_id, employee_id)
);

CREATE TABLE employee_aisles (
    id serial PRIMARY KEY,
    aisle_id integer REFERENCES aisles(id),
    employee_id integer REFERENCES employees(id),
    UNIQUE (employee_id, aisle_id)
);

CREATE TABLE categories (
    id serial PRIMARY KEY,
    company_id integer NOT NULL REFERENCES companies(id),
    category varchar(50) NOT NULL,
    UNIQUE (company_id, category)
);

CREATE TABLE products (
    id serial PRIMARY KEY,
    company_id integer NOT NULL REFERENCES companies(id),
    brand varchar,
    price money CHECK (price >= 0::money),
    name varchar(100) NOT NULL,
    category_id integer REFERENCES categories(id)
);

CREATE TABLE product_shelves (
    id serial PRIMARY KEY,
    product_id integer NOT NULL REFERENCES products(id),
    shelf_id integer NOT NULL REFERENCES shelves(id),
    product_quantity integer NOT NULL CHECK (product_quantity > 0),
    UNIQUE (product_id, shelf_id)
);

CREATE TABLE product_inventories (
    id serial PRIMARY KEY,
    inventory_id integer NOT NULL REFERENCES inventories(id),
    product_id integer NOT NULL REFERENCES products(id),
    product_quantity integer NOT NULL CHECK (product_quantity >= 0),
    UNIQUE (inventory_id, product_id)
);

CREATE TABLE restockings (
    id serial PRIMARY KEY,
    inventory_id integer NOT NULL REFERENCES inventories(id),
    employee_id integer NOT NULL REFERENCES employees(id),
    shelf_id integer NOT NULL REFERENCES shelves(id),
    restocking_date timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    restocked_quantity integer NOT NULL CHECK (restocked_quantity > 0),
    qnt_avarias integer NOT NULL DEFAULT 0 CHECK (qnt_avarias >= 0),
    product_id integer NOT NULL REFERENCES products(id)
);

CREATE TABLE alerts (
    id serial PRIMARY KEY,
    stockout_date date,
    employee_id integer REFERENCES employees(id),
    shelf_id integer NOT NULL REFERENCES shelves(id),
    product_id integer REFERENCES products(id),
    description varchar(250) NOT NULL,
    status varchar NOT NULL CHECK (status IN ('OPEN', 'RESOLVED', 'CANCELLED')),
    resolution_date timestamp
);

CREATE TABLE customers (
    id serial PRIMARY KEY,
    users_id integer NOT NULL UNIQUE REFERENCES users(id),
    company_id integer NOT NULL REFERENCES companies(id)
);

CREATE TABLE purchases (
    id serial PRIMARY KEY,
    customer_id integer NOT NULL REFERENCES customers(id),
    date timestamp,
    status varchar(20) NOT NULL CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'CANCELLED'))
);

CREATE TABLE purchase_products (
    id serial PRIMARY KEY,
    product_id integer NOT NULL REFERENCES products(id),
    purchase_id integer NOT NULL REFERENCES purchases(id),
    quantity integer NOT NULL CHECK (quantity > 0),
    amount money NOT NULL CHECK (amount >= 0::money),
    UNIQUE (purchase_id, product_id)
);

CREATE TABLE audits (
    id serial PRIMARY KEY,
    table_name varchar(100),
    operation varchar(10) NOT NULL CHECK (operation IN ('INSERT', 'UPDATE', 'DELETE')),
    timestamp timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    old_data jsonb,
    new_data jsonb,
    record_id integer,
    users varchar(100)
);
