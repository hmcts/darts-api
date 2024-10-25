-- Inserts per DMP-3807

INSERT INTO automated_task (aut_id, task_name, task_description, cron_expression, cron_editable, batch_size,
                            created_ts, created_by, last_modified_ts, last_modified_by)
VALUES (nextval('aut_seq'), 'ProcessE2EArmRpoPendingAutomatedTasks', 'RPO job to trigger the asynchronous search in ARM', '0 18 23 * * *', true, null, current_timestamp, 0, current_timestamp, 0),
       (nextval('aut_seq'), 'ArmRpoPollAutomatedTask', 'RPO job which polls ARM for CSV generation and downloads the CSV once search is completed', '0 */15 * * * *', true, 10000, current_timestamp, 0, current_timestamp, 0),
       (nextval('aut_seq'), 'ArmRpoReplayAutomatedTask', 'Replay job to push the all the objects which failed during ARM RPO process', '0 0 0 31 2 *', true, 100000, current_timestamp, 0, current_timestamp, 0);

INSERT INTO arm_automated_task (aat_id, aut_id, rpo_csv_start_hour, rpo_csv_end_hour, arm_replay_start_ts, arm_replay_end_ts, arm_attribute_type)
VALUES (1, (SELECT aut_id FROM automated_task WHERE task_name = 'ProcessE2EArmRpoPendingAutomatedTasks'), 25, 49, null, null, 'RPO'),
       (2, (SELECT aut_id FROM automated_task WHERE task_name = 'ArmRpoReplayAutomatedTask'), null, null, null, null, 'REPLAY');

INSERT INTO arm_rpo_state (are_id, are_description)
VALUES (1, 'GET_RECORD_MANAGEMENT_MATTER'),
       (2, 'GET_INDEXES_BY_MATTERID'),
       (3, 'GET_STORAGE_ACCOUNTS'),
       (4, 'GET_PROFILE_ENTITLEMENTS'),
       (5, 'GET_MASTERINDEXFIELD_BY_RECORDCLASS_SCHEMA_PRIMARY'),
       (6, 'ADD_ASYNC_SEARCH'),
       (7, 'SAVE_BACKGROUND_SEARCH'),
       (8, 'GET_EXTENDED_SEARCHES_BY_MATTER'),
       (9, 'GET_MASTERINDEXFIELD_BY_RECORDCLASS_SCHEMA_SECONDARY'),
       (10, 'CREATE_EXPORT_BASED_ON_SEARCH_RESULTS_TABLE'),
       (11, 'GET_EXTENDED_PRODUCTIONS_BY_MATTER'),
       (12, 'GET_PRODUCTION_OUTPUT_FILES'),
       (13, 'DOWNLOAD_PRODUCTION'),
       (14, 'REMOVE_PRODUCTION');

INSERT INTO arm_rpo_status (aru_id, aru_description)
VALUES (1, 'IN_PROGRESS'),
       (2, 'COMPLETED'),
       (3, 'FAILED');

INSERT INTO object_record_status (ors_id, ors_description)
VALUES (22, 'Arm Replay')
