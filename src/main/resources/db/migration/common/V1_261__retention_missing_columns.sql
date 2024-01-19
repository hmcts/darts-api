ALTER TABLE case_retention ALTER COLUMN total_sentence DROP NOT NULL;
ALTER TABLE case_retention ADD COLUMN is_manual_override          BOOLEAN                       NOT NULL default false;

