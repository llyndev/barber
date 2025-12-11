-- Adiciona coluna is_business_creator como nullable primeiro
ALTER TABLE users ADD COLUMN IF NOT EXISTS is_business_creator BOOLEAN;

-- Atualiza todos os registros existentes para false
UPDATE users SET is_business_creator = false WHERE is_business_creator IS NULL;

-- Agora altera para NOT NULL
ALTER TABLE users ALTER COLUMN is_business_creator SET NOT NULL;
ALTER TABLE users ALTER COLUMN is_business_creator SET DEFAULT false;

-- Adiciona coluna plant_type (já deve existir, mas garantindo)
ALTER TABLE users ADD COLUMN IF NOT EXISTS plant_type VARCHAR(255);

-- Adiciona coluna plan_expiration_date na tabela business (já deve existir, mas garantindo)
ALTER TABLE business ADD COLUMN IF NOT EXISTS plan_expiration_date TIMESTAMP(6);

