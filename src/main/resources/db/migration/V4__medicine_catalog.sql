-- Medicines the doctor has prescribed before: used for auto-suggest + auto-fill
CREATE TABLE medicines (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(200) NOT NULL,
  default_dosage VARCHAR(100),
  default_frequency VARCHAR(100),
  default_duration VARCHAR(100),
  default_instructions VARCHAR(300),
  use_count INT NOT NULL DEFAULT 0,
  last_used_at DATETIME(6),
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT uq_medicine_name UNIQUE (name),
  INDEX idx_medicine_use (use_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
