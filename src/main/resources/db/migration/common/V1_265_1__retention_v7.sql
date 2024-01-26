ALTER TABLE case_management_retention ALTER COLUMN total_sentence DROP NOT NULL;
ALTER TABLE case_retention DROP COLUMN is_manual_override;

ALTER TABLE retention_policy_type ALTER COLUMN fixed_policy_key SET DATA TYPE CHARACTER VARYING;
ALTER TABLE retention_policy_type ADD COLUMN display_name                CHARACTER VARYING;
ALTER TABLE retention_policy_type ADD COLUMN description                 CHARACTER VARYING;
update retention_policy_type set display_name = 'Legacy Permanent', description = 'DARTS Permanent retention policy' WHERE fixed_policy_key = '-1';
update retention_policy_type set display_name = 'Legacy Standard', description = 'DARTS Standard retention policy' WHERE fixed_policy_key = '-2';
update retention_policy_type set display_name = 'Not Guilty', description = 'DARTS Not Guilty policy for Variable Retention release October 2023' WHERE fixed_policy_key = '1';
update retention_policy_type set display_name = 'Non Custodial', description = 'DARTS Non Custodial policy for Variable Retention release October 2023' WHERE fixed_policy_key = '2';
update retention_policy_type set display_name = 'Custodial', description = 'DARTS Custodial policy for Variable Retention release October 2023' WHERE fixed_policy_key = '3';
update retention_policy_type set display_name = 'Life', description = 'DARTS Life policy for Variable Retention release October 2023' WHERE fixed_policy_key = '4';
update retention_policy_type set display_name = 'Default', description = 'DARTS Default retention policy' WHERE fixed_policy_key = '5';

INSERT INTO darts.retention_policy_type
(rpt_id, fixed_policy_key, policy_name, display_name, description, duration, policy_start_ts, retention_policy_object_id, created_ts, created_by, last_modified_ts, last_modified_by)
VALUES(7, '0', 'DARTS Default Policy', 'Default', 'DARTS Default retention policy', 'yY0M0D', '2024-01-01 00:00:00', '', current_timestamp, 0, current_timestamp, 0);

INSERT INTO darts.retention_policy_type
(rpt_id, fixed_policy_key, policy_name, display_name, description, duration, policy_start_ts, retention_policy_object_id, created_ts, created_by, last_modified_ts, last_modified_by)
VALUES(8, 'PERM', 'DARTS Permanent Policy', 'Permanent', 'DARTS Manually Applied Permanent retention policy', '99Y0M0D', '2024-01-01 00:00:00', '', current_timestamp, 0, current_timestamp, 0);

INSERT INTO darts.retention_policy_type
(rpt_id, fixed_policy_key, policy_name, display_name, description, duration, policy_start_ts, retention_policy_object_id, created_ts, created_by, last_modified_ts, last_modified_by)
VALUES(9, 'MANUAL', 'DARTS Manual Policy', 'Manual', 'DARTS Manually Applied retention policy', '0Y0M0D', '2024-01-01 00:00:00', '', current_timestamp, 0, current_timestamp, 0);

ALTER TABLE retention_policy_type ALTER COLUMN fixed_policy_key SET NOT NULL;
ALTER TABLE retention_policy_type ALTER COLUMN description SET NOT NULL;

