-- Dr. Munot's Orthopaedic Care Hospital - core schema (MySQL 8)

CREATE TABLE users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  email VARCHAR(150) NOT NULL,
  phone VARCHAR(20),
  password_hash VARCHAR(100) NOT NULL,
  full_name VARCHAR(120) NOT NULL,
  role VARCHAR(20) NOT NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT uq_users_email UNIQUE (email),
  INDEX idx_users_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE departments (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  description VARCHAR(500),
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT uq_departments_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE doctors (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  department_id BIGINT,
  specialization VARCHAR(150),
  qualification VARCHAR(255),
  registration_number VARCHAR(80),
  consultation_fee DECIMAL(10,2) NOT NULL DEFAULT 0,
  bio TEXT,
  slot_minutes INT NOT NULL DEFAULT 15,
  max_patients_per_slot INT NOT NULL DEFAULT 1,
  active TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT uq_doctors_user UNIQUE (user_id),
  CONSTRAINT fk_doctors_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_doctors_dept FOREIGN KEY (department_id) REFERENCES departments(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE receptionists (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  designation VARCHAR(80),
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT uq_receptionists_user UNIQUE (user_id),
  CONSTRAINT fk_receptionists_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE patients (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  patient_code VARCHAR(20) NOT NULL,
  user_id BIGINT,
  full_name VARCHAR(120) NOT NULL,
  phone VARCHAR(20) NOT NULL,
  email VARCHAR(150),
  date_of_birth DATE,
  gender VARCHAR(20),
  address VARCHAR(300),
  blood_group VARCHAR(10),
  emergency_contact_name VARCHAR(120),
  emergency_contact_phone VARCHAR(20),
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT uq_patients_code UNIQUE (patient_code),
  CONSTRAINT uq_patients_user UNIQUE (user_id),
  CONSTRAINT uq_patients_identity UNIQUE (phone, full_name, date_of_birth),
  CONSTRAINT fk_patients_user FOREIGN KEY (user_id) REFERENCES users(id),
  INDEX idx_patients_phone (phone),
  INDEX idx_patients_name (full_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE medical_services (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(150) NOT NULL,
  description VARCHAR(600),
  department_id BIGINT,
  price DECIMAL(10,2) NOT NULL DEFAULT 0,
  active TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT uq_services_name UNIQUE (name),
  CONSTRAINT fk_services_dept FOREIGN KEY (department_id) REFERENCES departments(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE doctor_availability (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  doctor_id BIGINT NOT NULL,
  day_of_week VARCHAR(12) NOT NULL,
  start_time TIME NOT NULL,
  end_time TIME NOT NULL,
  break_start TIME,
  break_end TIME,
  active TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_avail_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(id),
  INDEX idx_avail_doctor_day (doctor_id, day_of_week)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE doctor_leaves (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  doctor_id BIGINT NOT NULL,
  leave_date DATE NOT NULL,
  reason VARCHAR(200),
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT uq_leave UNIQUE (doctor_id, leave_date),
  CONSTRAINT fk_leave_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE appointments (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  appointment_code VARCHAR(40) NOT NULL,
  patient_id BIGINT NOT NULL,
  doctor_id BIGINT NOT NULL,
  appointment_date DATE NOT NULL,
  start_time TIME NOT NULL,
  end_time TIME NOT NULL,
  status VARCHAR(20) NOT NULL,
  type VARCHAR(20) NOT NULL,
  reason VARCHAR(500),
  cancel_reason VARCHAR(300),
  created_by BIGINT,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT uq_appt_code UNIQUE (appointment_code),
  CONSTRAINT fk_appt_patient FOREIGN KEY (patient_id) REFERENCES patients(id),
  CONSTRAINT fk_appt_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(id),
  CONSTRAINT fk_appt_creator FOREIGN KEY (created_by) REFERENCES users(id),
  INDEX idx_appt_doctor_slot (doctor_id, appointment_date, start_time),
  INDEX idx_appt_patient (patient_id, appointment_date),
  INDEX idx_appt_date_status (appointment_date, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE queue_entries (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  appointment_id BIGINT NOT NULL,
  doctor_id BIGINT NOT NULL,
  queue_date DATE NOT NULL,
  token_number INT NOT NULL,
  status VARCHAR(20) NOT NULL,
  checked_in_at DATETIME(6) NOT NULL,
  called_at DATETIME(6),
  completed_at DATETIME(6),
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT uq_queue_appt UNIQUE (appointment_id),
  CONSTRAINT uq_queue_token UNIQUE (doctor_id, queue_date, token_number),
  CONSTRAINT fk_queue_appt FOREIGN KEY (appointment_id) REFERENCES appointments(id),
  CONSTRAINT fk_queue_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(id),
  INDEX idx_queue_doctor_date (doctor_id, queue_date, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE consultations (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  appointment_id BIGINT NOT NULL,
  patient_id BIGINT NOT NULL,
  doctor_id BIGINT NOT NULL,
  symptoms TEXT,
  examination_notes TEXT,
  treatment_plan TEXT,
  tests_advised TEXT,
  advice TEXT,
  status VARCHAR(20) NOT NULL,
  started_at DATETIME(6) NOT NULL,
  completed_at DATETIME(6),
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT uq_consult_appt UNIQUE (appointment_id),
  CONSTRAINT fk_consult_appt FOREIGN KEY (appointment_id) REFERENCES appointments(id),
  CONSTRAINT fk_consult_patient FOREIGN KEY (patient_id) REFERENCES patients(id),
  CONSTRAINT fk_consult_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(id),
  INDEX idx_consult_patient (patient_id, started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE diagnoses (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  consultation_id BIGINT NOT NULL,
  name VARCHAR(200) NOT NULL,
  icd_code VARCHAR(20),
  notes VARCHAR(500),
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_diag_consult FOREIGN KEY (consultation_id) REFERENCES consultations(id) ON DELETE CASCADE,
  INDEX idx_diag_consult (consultation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE prescriptions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  prescription_code VARCHAR(30) NOT NULL,
  consultation_id BIGINT NOT NULL,
  patient_id BIGINT NOT NULL,
  doctor_id BIGINT NOT NULL,
  notes TEXT,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT uq_rx_code UNIQUE (prescription_code),
  CONSTRAINT uq_rx_consult UNIQUE (consultation_id),
  CONSTRAINT fk_rx_consult FOREIGN KEY (consultation_id) REFERENCES consultations(id),
  CONSTRAINT fk_rx_patient FOREIGN KEY (patient_id) REFERENCES patients(id),
  CONSTRAINT fk_rx_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(id),
  INDEX idx_rx_patient (patient_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE prescription_items (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  prescription_id BIGINT NOT NULL,
  medicine_name VARCHAR(200) NOT NULL,
  dosage VARCHAR(100),
  frequency VARCHAR(100),
  duration VARCHAR(100),
  instructions VARCHAR(300),
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_rxi_rx FOREIGN KEY (prescription_id) REFERENCES prescriptions(id) ON DELETE CASCADE,
  INDEX idx_rxi_rx (prescription_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE follow_ups (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  consultation_id BIGINT NOT NULL,
  patient_id BIGINT NOT NULL,
  doctor_id BIGINT NOT NULL,
  follow_up_date DATE NOT NULL,
  follow_up_time TIME,
  appointment_id BIGINT,
  status VARCHAR(20) NOT NULL,
  notes VARCHAR(300),
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT uq_followup_consult UNIQUE (consultation_id),
  CONSTRAINT fk_fu_consult FOREIGN KEY (consultation_id) REFERENCES consultations(id),
  CONSTRAINT fk_fu_patient FOREIGN KEY (patient_id) REFERENCES patients(id),
  CONSTRAINT fk_fu_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(id),
  CONSTRAINT fk_fu_appt FOREIGN KEY (appointment_id) REFERENCES appointments(id),
  INDEX idx_fu_patient (patient_id, follow_up_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE patient_documents (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  patient_id BIGINT NOT NULL,
  file_name VARCHAR(255) NOT NULL,
  stored_name VARCHAR(100) NOT NULL,
  content_type VARCHAR(100) NOT NULL,
  size_bytes BIGINT NOT NULL,
  uploaded_by BIGINT,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT uq_doc_stored UNIQUE (stored_name),
  CONSTRAINT fk_doc_patient FOREIGN KEY (patient_id) REFERENCES patients(id),
  CONSTRAINT fk_doc_uploader FOREIGN KEY (uploaded_by) REFERENCES users(id),
  INDEX idx_doc_patient (patient_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE medical_reports (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  patient_id BIGINT NOT NULL,
  appointment_id BIGINT,
  document_id BIGINT,
  report_type VARCHAR(20) NOT NULL,
  title VARCHAR(200) NOT NULL,
  notes VARCHAR(500),
  report_date DATE NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_rep_patient FOREIGN KEY (patient_id) REFERENCES patients(id),
  CONSTRAINT fk_rep_appt FOREIGN KEY (appointment_id) REFERENCES appointments(id),
  CONSTRAINT fk_rep_doc FOREIGN KEY (document_id) REFERENCES patient_documents(id),
  INDEX idx_rep_patient (patient_id, report_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE bills (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  bill_number VARCHAR(30) NOT NULL,
  patient_id BIGINT NOT NULL,
  appointment_id BIGINT,
  subtotal DECIMAL(12,2) NOT NULL,
  discount DECIMAL(12,2) NOT NULL DEFAULT 0,
  tax_percent DECIMAL(5,2) NOT NULL DEFAULT 0,
  tax DECIMAL(12,2) NOT NULL DEFAULT 0,
  total DECIMAL(12,2) NOT NULL,
  paid_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
  pending_amount DECIMAL(12,2) NOT NULL,
  status VARCHAR(10) NOT NULL,
  notes VARCHAR(300),
  created_by BIGINT,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT uq_bill_number UNIQUE (bill_number),
  CONSTRAINT uq_bill_appt UNIQUE (appointment_id),
  CONSTRAINT fk_bill_patient FOREIGN KEY (patient_id) REFERENCES patients(id),
  CONSTRAINT fk_bill_appt FOREIGN KEY (appointment_id) REFERENCES appointments(id),
  CONSTRAINT fk_bill_creator FOREIGN KEY (created_by) REFERENCES users(id),
  INDEX idx_bill_patient (patient_id),
  INDEX idx_bill_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE bill_items (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  bill_id BIGINT NOT NULL,
  category VARCHAR(20) NOT NULL,
  description VARCHAR(200) NOT NULL,
  quantity INT NOT NULL,
  unit_price DECIMAL(12,2) NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_bi_bill FOREIGN KEY (bill_id) REFERENCES bills(id) ON DELETE CASCADE,
  INDEX idx_bi_bill (bill_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE payments (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  bill_id BIGINT NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  method VARCHAR(20) NOT NULL,
  reference VARCHAR(100),
  paid_at DATETIME(6) NOT NULL,
  received_by BIGINT,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_pay_bill FOREIGN KEY (bill_id) REFERENCES bills(id),
  CONSTRAINT fk_pay_user FOREIGN KEY (received_by) REFERENCES users(id),
  INDEX idx_pay_bill (bill_id),
  INDEX idx_pay_date (paid_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE notifications (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  recipient_user_id BIGINT,
  patient_id BIGINT,
  appointment_id BIGINT,
  channel VARCHAR(12) NOT NULL,
  type VARCHAR(30) NOT NULL,
  title VARCHAR(200) NOT NULL,
  message VARCHAR(1000) NOT NULL,
  status VARCHAR(12) NOT NULL,
  scheduled_at DATETIME(6) NOT NULL,
  sent_at DATETIME(6),
  read_flag TINYINT(1) NOT NULL DEFAULT 0,
  attempts INT NOT NULL DEFAULT 0,
  last_error VARCHAR(300),
  dedupe_key VARCHAR(120) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT uq_notif_dedupe UNIQUE (dedupe_key),
  CONSTRAINT fk_notif_user FOREIGN KEY (recipient_user_id) REFERENCES users(id),
  CONSTRAINT fk_notif_patient FOREIGN KEY (patient_id) REFERENCES patients(id),
  CONSTRAINT fk_notif_appt FOREIGN KEY (appointment_id) REFERENCES appointments(id),
  INDEX idx_notif_due (status, scheduled_at),
  INDEX idx_notif_user (recipient_user_id, channel, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE audit_logs (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT,
  username VARCHAR(150),
  action VARCHAR(60) NOT NULL,
  entity_type VARCHAR(60),
  entity_id VARCHAR(60),
  details VARCHAR(500),
  ip_address VARCHAR(60),
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  INDEX idx_audit_created (created_at),
  INDEX idx_audit_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE hospital_settings (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  setting_key VARCHAR(80) NOT NULL,
  setting_value TEXT,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT uq_setting_key UNIQUE (setting_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
