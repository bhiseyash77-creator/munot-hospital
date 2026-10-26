-- Clinic photo gallery managed by the admin
CREATE TABLE clinic_photos (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  caption VARCHAR(200),
  stored_name VARCHAR(100) NOT NULL,
  content_type VARCHAR(50) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  active TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT uq_clinic_photo_file UNIQUE (stored_name),
  INDEX idx_clinic_photo_order (active, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
