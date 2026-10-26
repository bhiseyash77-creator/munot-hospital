-- Database-level safety net checks. Run after migrations:  mysql munot_hospital < src/test/sql/constraints_check.sql
-- Each statement marked EXPECT FAIL must be rejected by MySQL.  Everything runs inside a transaction that is rolled back.
START TRANSACTION;
INSERT INTO users (email,password_hash,full_name,role,enabled,created_at,updated_at) VALUES ('t-doc@example.com','x','Dr T','DOCTOR',1,NOW(6),NOW(6)),('t-pat@example.com','x','P T','PATIENT',1,NOW(6),NOW(6));
SET @du=(SELECT id FROM users WHERE email='t-doc@example.com'), @pu=(SELECT id FROM users WHERE email='t-pat@example.com');
INSERT INTO doctors (user_id,created_at,updated_at) VALUES (@du,NOW(6),NOW(6));
SET @d=(SELECT id FROM doctors WHERE user_id=@du);
INSERT INTO patients (patient_code,user_id,full_name,phone,date_of_birth,created_at,updated_at) VALUES ('PAT-T1',@pu,'P T','9000000999','1990-01-01',NOW(6),NOW(6));
SET @p=(SELECT id FROM patients WHERE patient_code='PAT-T1');
INSERT INTO appointments (appointment_code,patient_id,doctor_id,appointment_date,start_time,end_time,status,type,created_at,updated_at) VALUES ('APT-T1',@p,@d,'2030-01-01','10:00','10:15','BOOKED','ONLINE',NOW(6),NOW(6));
SET @a=(SELECT id FROM appointments WHERE appointment_code='APT-T1');
INSERT INTO queue_entries (appointment_id,doctor_id,queue_date,token_number,status,checked_in_at,created_at,updated_at) VALUES (@a,@d,'2030-01-01',1,'WAITING',NOW(6),NOW(6),NOW(6));
-- EXPECT FAIL: duplicate appointment code
INSERT INTO appointments (appointment_code,patient_id,doctor_id,appointment_date,start_time,end_time,status,type,created_at,updated_at) VALUES ('APT-T1',@p,@d,'2030-01-02','10:00','10:15','BOOKED','ONLINE',NOW(6),NOW(6));
-- EXPECT FAIL: same patient identity (phone+name+dob) twice
INSERT INTO patients (patient_code,full_name,phone,date_of_birth,created_at,updated_at) VALUES ('PAT-T2','P T','9000000999','1990-01-01',NOW(6),NOW(6));
-- EXPECT FAIL: same queue token twice for a doctor/day
INSERT INTO appointments (appointment_code,patient_id,doctor_id,appointment_date,start_time,end_time,status,type,created_at,updated_at) VALUES ('APT-T3',@p,@d,'2030-01-01','10:15','10:30','BOOKED','ONLINE',NOW(6),NOW(6));
INSERT INTO queue_entries (appointment_id,doctor_id,queue_date,token_number,status,checked_in_at,created_at,updated_at) VALUES ((SELECT id FROM appointments WHERE appointment_code='APT-T3'),@d,'2030-01-01',1,'WAITING',NOW(6),NOW(6),NOW(6));
-- EXPECT FAIL: a block with only one time
INSERT INTO slot_blocks (doctor_id,block_date,start_time,end_time,created_at,updated_at) VALUES (@d,'2030-01-01','10:00',NULL,NOW(6),NOW(6));
-- EXPECT FAIL: block ending before it starts
INSERT INTO slot_blocks (doctor_id,block_date,start_time,end_time,created_at,updated_at) VALUES (@d,'2030-01-01','12:00','11:00',NOW(6),NOW(6));
-- EXPECT FAIL: duplicate reminder (dedupe key)
INSERT INTO notifications (patient_id,channel,type,title,message,status,scheduled_at,dedupe_key,created_at,updated_at) VALUES (@p,'SMS','GENERAL','t','m','PENDING',NOW(6),'K1',NOW(6),NOW(6));
INSERT INTO notifications (patient_id,channel,type,title,message,status,scheduled_at,dedupe_key,created_at,updated_at) VALUES (@p,'SMS','GENERAL','t','m','PENDING',NOW(6),'K1',NOW(6),NOW(6));
-- EXPECT FAIL: two bills for one appointment
INSERT INTO bills (bill_number,patient_id,appointment_id,subtotal,total,pending_amount,status,created_at,updated_at) VALUES ('INV-T1',@p,@a,100,100,100,'PENDING',NOW(6),NOW(6));
INSERT INTO bills (bill_number,patient_id,appointment_id,subtotal,total,pending_amount,status,created_at,updated_at) VALUES ('INV-T2',@p,@a,100,100,100,'PENDING',NOW(6),NOW(6));
-- EXPECT FAIL: duplicate user email (case-insensitive)
INSERT INTO users (email,password_hash,full_name,role,enabled,created_at,updated_at) VALUES ('T-DOC@example.com','x','Dup','DOCTOR',1,NOW(6),NOW(6));
ROLLBACK;
