-- Follow-up advice on the prescription, in days after the prescription date (NULL = no follow-up advised).
-- Stored on the consultation so a completed prescription can be edited. Existing follow-up appointments (follow_ups table) are not touched.
ALTER TABLE consultations ADD COLUMN follow_up_days INT NULL;
