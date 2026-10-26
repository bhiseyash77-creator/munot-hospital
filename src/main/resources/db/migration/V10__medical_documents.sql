-- Doctor-issued medical documents (certificates, referral, admission advice). Printed on the clinic's own pre-printed letterhead.
CREATE TABLE medical_documents (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  doc_code VARCHAR(20) NOT NULL,
  patient_id BIGINT NOT NULL,
  doctor_id BIGINT NOT NULL,
  doc_type VARCHAR(30) NOT NULL,
  status VARCHAR(10) NOT NULL,
  document_date DATE NOT NULL,
  content TEXT NOT NULL,
  doctor_name_snapshot VARCHAR(120),
  doctor_qualification_snapshot VARCHAR(255),
  doctor_registration_snapshot VARCHAR(80),
  patient_name_snapshot VARCHAR(120),
  patient_code_snapshot VARCHAR(20),
  issued_at DATETIME(6),
  revision_of_id BIGINT,
  print_count INT NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT uq_meddoc_code UNIQUE (doc_code),
  CONSTRAINT fk_meddoc_patient FOREIGN KEY (patient_id) REFERENCES patients(id),
  CONSTRAINT fk_meddoc_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(id),
  CONSTRAINT fk_meddoc_revision FOREIGN KEY (revision_of_id) REFERENCES medical_documents(id),
  INDEX idx_meddoc_patient (patient_id, doc_type),
  INDEX idx_meddoc_doctor (doctor_id, status, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Blank area of the physical letterhead (millimetres). Adjustable by the doctor/admin with the alignment test sheet.
INSERT INTO hospital_settings (setting_key, setting_value, created_at, updated_at) VALUES
 ('print_margin_top_mm', '55', NOW(6), NOW(6)),
 ('print_margin_bottom_mm', '30', NOW(6), NOW(6)),
 ('print_margin_left_mm', '20', NOW(6), NOW(6)),
 ('print_margin_right_mm', '20', NOW(6), NOW(6));
