-- Patient-language (Marathi / Hindi) copy of the doctor's advice and medicine instructions.
ALTER TABLE consultations
  ADD COLUMN language VARCHAR(2) NOT NULL DEFAULT 'EN',
  ADD COLUMN advice_local TEXT NULL,
  ADD COLUMN tests_advised_local TEXT NULL;

ALTER TABLE prescription_items
  ADD COLUMN frequency_local VARCHAR(300) NULL,
  ADD COLUMN duration_local VARCHAR(300) NULL,
  ADD COLUMN instructions_local VARCHAR(300) NULL;
