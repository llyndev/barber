ALTER TABLE opening_hours
    ADD COLUMN business_id BIGINT;

UPDATE opening_hours SET business_id = 1 WHERE business_id IS NULL;

ALTER TABLE opening_hours
  ALTER COLUMN business_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_opening_hours_business ON opening_hours(business_id);

ALTER TABLE opening_hours
  ADD CONSTRAINT fk_opening_hours_business
  FOREIGN KEY (business_id) REFERENCES business(id);