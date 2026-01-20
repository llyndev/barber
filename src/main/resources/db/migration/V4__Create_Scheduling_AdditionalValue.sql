CREATE TABLE scheduling_additional_value (
    id BIGSERIAL PRIMARY KEY,
    scheduling_id BIGINT NOT NULL,
    barber_id BIGINT NOT NULL,
    value DECIMAL(10, 2) NOT NULL,
    CONSTRAINT fk_sav_scheduling FOREIGN KEY (scheduling_id) REFERENCES scheduling (id),
    CONSTRAINT fk_sav_barber FOREIGN KEY (barber_id) REFERENCES users (id)
);

CREATE INDEX idx_sav_scheduling ON scheduling_additional_value(scheduling_id);
CREATE INDEX idx_sav_barber ON scheduling_additional_value(barber_id);
