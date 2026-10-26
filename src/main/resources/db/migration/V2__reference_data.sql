-- Reference data only (no patient data, no credentials). Edit via Admin > Settings / Services.
INSERT INTO departments (name, description, created_at, updated_at) VALUES
 ('Orthopaedics', 'Bone, joint, muscle and spine care', NOW(6), NOW(6)),
 ('Physiotherapy', 'Rehabilitation and physical therapy', NOW(6), NOW(6)),
 ('Diagnostics', 'X-ray and basic diagnostic services', NOW(6), NOW(6));

INSERT INTO medical_services (name, description, department_id, price, active, created_at, updated_at) VALUES
 ('Orthopaedic Consultation', 'Consultation and examination for bone, joint and muscle problems', (SELECT id FROM departments WHERE name='Orthopaedics'), 0, 1, NOW(6), NOW(6)),
 ('Fracture Care', 'Assessment and management of fractures', (SELECT id FROM departments WHERE name='Orthopaedics'), 0, 1, NOW(6), NOW(6)),
 ('Joint Pain & Arthritis Care', 'Evaluation and treatment planning for joint pain', (SELECT id FROM departments WHERE name='Orthopaedics'), 0, 1, NOW(6), NOW(6)),
 ('Sports Injury Care', 'Assessment of ligament, tendon and muscle injuries', (SELECT id FROM departments WHERE name='Orthopaedics'), 0, 1, NOW(6), NOW(6)),
 ('Spine & Back Pain Care', 'Evaluation of neck and back pain', (SELECT id FROM departments WHERE name='Orthopaedics'), 0, 1, NOW(6), NOW(6)),
 ('Joint Replacement Consultation', 'Consultation for patients who may need joint replacement', (SELECT id FROM departments WHERE name='Orthopaedics'), 0, 1, NOW(6), NOW(6)),
 ('Plaster & Splinting', 'Cast, plaster and splint application', (SELECT id FROM departments WHERE name='Orthopaedics'), 0, 1, NOW(6), NOW(6)),
 ('Physiotherapy & Rehabilitation', 'Guided exercise and rehabilitation programmes', (SELECT id FROM departments WHERE name='Physiotherapy'), 0, 1, NOW(6), NOW(6)),
 ('X-Ray', 'Digital X-ray imaging', (SELECT id FROM departments WHERE name='Diagnostics'), 0, 1, NOW(6), NOW(6));

INSERT INTO hospital_settings (setting_key, setting_value, created_at, updated_at) VALUES
 ('hospital_name', 'Dr. Munot''s Orthopaedic Care Hospital', NOW(6), NOW(6)),
 ('tagline', 'Bone, joint and spine care with a personal touch', NOW(6), NOW(6)),
 ('address', '[Hospital address - update in Admin > Settings]', NOW(6), NOW(6)),
 ('phone', '[Hospital phone - update in Admin > Settings]', NOW(6), NOW(6)),
 ('emergency_phone', '[Emergency phone - update in Admin > Settings]', NOW(6), NOW(6)),
 ('email', '[Hospital email - update in Admin > Settings]', NOW(6), NOW(6)),
 ('opening_hours', '[Opening hours - update in Admin > Settings]', NOW(6), NOW(6)),
 ('map_embed_url', '', NOW(6), NOW(6)),
 ('about_text', 'An orthopaedic care hospital focused on accurate diagnosis, clear communication and structured recovery. [Edit this text in Admin > Settings]', NOW(6), NOW(6)),
 ('cancellation_cutoff_hours', '2', NOW(6), NOW(6)),
 ('tax_percent_default', '0', NOW(6), NOW(6)),
 ('invoice_footer', 'Thank you for choosing us. This is a computer generated invoice.', NOW(6), NOW(6));
