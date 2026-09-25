ALTER TABLE users ADD CONSTRAINT uk_users_cpf UNIQUE (cpf);
ALTER TABLE users ADD CONSTRAINT uk_users_email UNIQUE (email);
