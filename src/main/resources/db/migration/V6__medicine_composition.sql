-- Medicine catalog keeps only NAME + COMPOSITION. Dose/frequency/duration are typed fresh for every prescription.
ALTER TABLE prescription_items ADD COLUMN composition VARCHAR(300) NULL AFTER medicine_name;

ALTER TABLE medicines
  ADD COLUMN composition VARCHAR(300) NULL AFTER name,
  DROP COLUMN default_dosage,
  DROP COLUMN default_frequency,
  DROP COLUMN default_duration,
  DROP COLUMN default_instructions;
