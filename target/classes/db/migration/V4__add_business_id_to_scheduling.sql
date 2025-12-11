UPDATE scheduling SET business_id = 1 WHERE business_id IS NULL;

ALTER TABLE scheduling
  ALTER COLUMN business_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_scheduling_business ON scheduling(business_id);

ALTER TABLE scheduling
  ADD CONSTRAINT fk_scheduling_business
  FOREIGN KEY (business_id) REFERENCES business(id);