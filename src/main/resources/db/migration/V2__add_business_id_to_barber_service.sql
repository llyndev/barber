-- SQL
ALTER TABLE barber_service
  ADD COLUMN business_id BIGINT;

CREATE INDEX idx_barber_service_business_id ON barber_service(business_id);

-- constraint apontando para a PK da tabela business (assumindo que seja id)
ALTER TABLE barber_service
  ADD CONSTRAINT fk_barber_service_business
  FOREIGN KEY (business_id) REFERENCES business(id);
