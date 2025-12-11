
CREATE TABLE IF NOT EXISTS business (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    slug VARCHAR(255) NOT NULL,
    add_cep VARCHAR(20),
    add_logradouro VARCHAR(255),
    add_numero VARCHAR(50),
    add_complemento VARCHAR(255),
    add_bairro VARCHAR(255),
    timezone VARCHAR(100),
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_business_slug ON business (slug);


CREATE TABLE IF NOT EXISTS memberships (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    business_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(50) NOT NULL,
    CONSTRAINT fk_membership_business FOREIGN KEY (business_id) REFERENCES business(id) ON DELETE CASCADE,
    CONSTRAINT fk_membership_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE
);

-- Observações:
-- 1) Se sua tabela de usuários não for "app_user", substitua "app_user" por "nome_da_sua_tabela" acima.
-- 2) Se usar H2 em dev, AUTO_INCREMENT funciona; em outros DBs (Postgres) você pode precisar ajustar para SERIAL / IDENTITY.
-- 3) Em banco já existente, considere usar flyway.baselineOnMigrate=true para não tentar reaplicar migrações que já existem.