-- Doctor-managed blocked times (whole day when start/end are NULL) + booking limit settings
CREATE TABLE slot_blocks (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  doctor_id BIGINT NOT NULL,
  block_date DATE NOT NULL,
  start_time TIME NULL,
  end_time TIME NULL,
  reason VARCHAR(200),
  created_by BIGINT,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_block_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(id),
  CONSTRAINT fk_block_creator FOREIGN KEY (created_by) REFERENCES users(id),
  CONSTRAINT chk_block_times CHECK ((start_time IS NULL AND end_time IS NULL) OR (start_time IS NOT NULL AND end_time IS NOT NULL AND end_time > start_time)),
  INDEX idx_block_doctor_date (doctor_id, block_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO hospital_settings (setting_key, setting_value, created_at, updated_at) VALUES
 ('max_advance_days', '60', NOW(6), NOW(6)),
 ('max_active_appointments', '5', NOW(6), NOW(6));
