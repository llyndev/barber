ALTER TABLE scheduling DROP CONSTRAINT IF EXISTS scheduling_states_check;

ALTER TABLE scheduling ADD CONSTRAINT scheduling_states_check 
CHECK (states IN ('SCHEDULED', 'CONFIRMED', 'IN_PROGRESS', 'COMPLETED', 'CANCELED', 'RESCHEDULED'));
