ALTER TABLE case_link_import_result ADD COLUMN case_2_courthouse TEXT;

-- The current import format supplies one courthouse for both cases.
UPDATE case_link_import_result SET case_2_courthouse = case_1_courthouse;
