-- Doctor photo + admin-editable website content

ALTER TABLE doctors
  ADD COLUMN photo_stored_name VARCHAR(100) NULL,
  ADD COLUMN photo_content_type VARCHAR(50) NULL;

CREATE TABLE content_items (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  type VARCHAR(20) NOT NULL,
  slug VARCHAR(120),
  title VARCHAR(200) NOT NULL,
  subtitle VARCHAR(200),
  body TEXT,
  icon VARCHAR(40),
  sort_order INT NOT NULL DEFAULT 0,
  active TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT uq_content_slug UNIQUE (type, slug),
  INDEX idx_content_type (type, active, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- "Why choose us": describes features of this software, safe to publish.
INSERT INTO content_items (type, title, body, icon, sort_order, active, created_at, updated_at) VALUES
 ('WHY_US','Easy online appointments','Choose a doctor and a free slot from your phone, and get confirmation and reminders.','calendar',1,1,NOW(6),NOW(6)),
 ('WHY_US','Organised queue','Token-based check-in means less confusion and more predictable waiting.','clipboard',2,1,NOW(6),NOW(6)),
 ('WHY_US','Digital prescriptions & reports','Access and download your prescriptions and reports whenever you need them.','file',3,1,NOW(6),NOW(6)),
 ('WHY_US','Follow-up reminders','Doctor-advised follow-ups are scheduled automatically with timely reminders.','activity',4,1,NOW(6),NOW(6)),
 ('WHY_US','Transparent billing','Itemised invoices with clear paid and pending amounts.','wallet',5,1,NOW(6),NOW(6)),
 ('WHY_US','Privacy-first records','Your records are visible only to you and the doctors treating you.','shield',6,1,NOW(6),NOW(6));

-- Facilities are seeded INACTIVE: the hospital must enable only what it really has.
INSERT INTO content_items (type, title, body, icon, sort_order, active, created_at, updated_at) VALUES
 ('FACILITY','Consultation Rooms','Private rooms for examination and discussion of your treatment plan.','stethoscope',1,0,NOW(6),NOW(6)),
 ('FACILITY','X-Ray & Diagnostics','Basic imaging and diagnostic support for bone and joint problems.','scan',2,0,NOW(6),NOW(6)),
 ('FACILITY','Physiotherapy & Rehabilitation','Guided exercise and recovery programmes after injury or surgery.','dumbbell',3,0,NOW(6),NOW(6)),
 ('FACILITY','Plaster & Splint Room','Cast, plaster and splint application and dressing.','bone',4,0,NOW(6),NOW(6)),
 ('FACILITY','Patient Waiting Area','A comfortable waiting space with a token/queue system.','users',5,0,NOW(6),NOW(6)),
 ('FACILITY','Online Patient Portal','Book appointments, view prescriptions, reports and bills online.','smartphone',6,0,NOW(6),NOW(6));

-- Generic health tips (general advice, editable). No testimonials are seeded on purpose.
INSERT INTO content_items (type, slug, title, subtitle, body, sort_order, active, created_at, updated_at) VALUES
 ('HEALTH_TIP','protect-your-joints','Everyday habits that are kind to your joints','Simple changes in how you sit, lift and move can reduce strain on knees, hips and back.',
  'Regular, moderate movement keeps joints lubricated and muscles strong. Walking, swimming and cycling are low-impact options for many people.\n\nMaintain a healthy body weight, because extra weight increases the load on knees and hips. Wear supportive footwear and avoid sitting in one position for hours.\n\nWhen lifting, bend your knees, keep the object close to your body and avoid twisting. If you have pain that does not settle in a few days, see a doctor.',1,1,NOW(6),NOW(6)),
 ('HEALTH_TIP','back-pain-posture','Back pain and posture at your desk','How to set up your workspace and take breaks to ease neck and back strain.',
  'Keep your screen at eye level, your feet flat on the floor and your lower back supported. Avoid slouching forward for long periods.\n\nStand up and stretch every 30 to 45 minutes. Gentle stretching and walking breaks help reduce stiffness.\n\nSeek medical advice promptly if back pain comes with numbness, weakness in the legs, fever, or loss of bladder or bowel control.',2,1,NOW(6),NOW(6)),
 ('HEALTH_TIP','bone-health','Building and protecting bone health','Nutrition, activity and lifestyle choices that support strong bones at every age.',
  'Bones need calcium, vitamin D and protein, along with regular weight-bearing activity such as walking or light resistance exercise.\n\nAvoid smoking and limit alcohol, both of which can weaken bones over time. Safe sun exposure helps your body make vitamin D.\n\nAsk your doctor whether a bone health check is right for you, especially if you are over 50 or have had a fracture.',3,1,NOW(6),NOW(6)),
 ('HEALTH_TIP','after-a-fracture','Recovering well after a fracture','General guidance on cast care, rest and rehabilitation while healing.',
  'Follow your doctor''s instructions on rest, cast care and weight-bearing. Keep a cast dry and do not insert objects inside it.\n\nAttend all follow-up visits. Rehabilitation exercises, when advised, help restore movement and strength after healing.\n\nContact your doctor if you notice increasing pain, swelling, numbness, colour change in the fingers or toes, or a bad smell from the cast.',4,1,NOW(6),NOW(6));
