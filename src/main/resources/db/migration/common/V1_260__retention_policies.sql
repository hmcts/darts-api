DELETE
FROM case_retention;
DELETE
FROM retention_policy_type;

ALTER TABLE case_management_retention
  ADD COLUMN is_manual_override BOOLEAN NOT NULL;
ALTER TABLE case_management_retention
  ADD COLUMN event_ts TIMESTAMP WITH TIME ZONE;
ALTER TABLE case_management_retention
  ALTER COLUMN eve_id DROP NOT NULL;

ALTER TABLE retention_policy_type
  ALTER COLUMN duration TYPE CHARACTER VARYING;
ALTER TABLE retention_policy_type
  ALTER COLUMN policy_end_ts DROP NOT NULL;

INSERT INTO darts.retention_policy_type
  (rpt_id, fixed_policy_key, policy_name, duration, policy_start_ts, retention_policy_object_id, created_ts, created_by, last_modified_ts, last_modified_by)
VALUES (1, -1, 'DARTS Permanent Retention v3', '30 years = 30y0m0d', '2024-01-01 00:00:00', '08170758956b0614', current_timestamp, 0, current_timestamp, 0);
INSERT INTO darts.retention_policy_type
  (rpt_id, fixed_policy_key, policy_name, duration, policy_start_ts, retention_policy_object_id, created_ts, created_by, last_modified_ts, last_modified_by)
VALUES (2, -2, 'DARTS Standard Retention v3', '7 years = 7y0m0d', '2024-01-01 00:00:00', '08170758956b0615', current_timestamp, 0, current_timestamp, 0);
INSERT INTO darts.retention_policy_type
  (rpt_id, fixed_policy_key, policy_name, duration, policy_start_ts, retention_policy_object_id, created_ts, created_by, last_modified_ts, last_modified_by)
VALUES (3, 1, 'DARTS Not Guilty Policy', '1 year - format = 1y0m0d', '2024-01-01 00:00:00', '081707589800c90a', current_timestamp, 0, current_timestamp, 0);
INSERT INTO darts.retention_policy_type
  (rpt_id, fixed_policy_key, policy_name, duration, policy_start_ts, retention_policy_object_id, created_ts, created_by, last_modified_ts, last_modified_by)
VALUES (4, 2, 'DARTS Non Custodial Policy', '7 years = 7y0m0d', '2024-01-01 00:00:00', '081707589800cd00', current_timestamp, 0, current_timestamp, 0);
INSERT INTO darts.retention_policy_type
  (rpt_id, fixed_policy_key, policy_name, duration, policy_start_ts, retention_policy_object_id, created_ts, created_by, last_modified_ts, last_modified_by)
VALUES (5, 3, 'DARTS Custodial Policy', '7 years or length of sentence (passed into the case total sentence field), whichever is greater', '2024-01-01 00:00:00', '081707589800c90b', current_timestamp, 0, current_timestamp, 0);
INSERT INTO darts.retention_policy_type
  (rpt_id, fixed_policy_key, policy_name, duration, policy_start_ts, retention_policy_object_id, created_ts, created_by, last_modified_ts, last_modified_by)
VALUES (6, 4, 'DARTS Life Policy', '99 years = 99y0m0d', '2024-01-01 00:00:00', '081707589800cd01', current_timestamp, 0, current_timestamp, 0);
