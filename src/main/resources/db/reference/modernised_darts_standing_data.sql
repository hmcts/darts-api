--v2 add insert for system user in the user_account table, required for inserts that follow
--   set role and search_path to remove need to prefix

-- standing data required

SET ROLE DARTS_OWNER;
SET SEARCH_PATH TO darts;


INSERT INTO user_account (usr_id, dm_user_s_object_id, user_name, user_email_address, description, user_state, last_login_ts, is_system_user, account_guid, created_ts, created_by, last_modified_ts, last_modified_by)
VALUES (0, NULL, 'System User', 'dartssystemuser@hmcts.net', 'System User', 0, NULL, true, 'Not available', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0);
INSERT INTO user_account (usr_id, dm_user_s_object_id, user_name, user_email_address, description, user_state, last_login_ts, is_system_user, account_guid, created_ts, created_by, last_modified_ts, last_modified_by)
VALUES (-1, NULL, 'Event Processor', 'Event.Processor@example.com', 'Event Processor', 0, NULL, true, 'Not available', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0);
INSERT INTO user_account (usr_id, dm_user_s_object_id, user_name, user_email_address, description, user_state, last_login_ts, is_system_user, account_guid, created_ts, created_by, last_modified_ts, last_modified_by)
VALUES (-2, NULL, 'DailyList Processor', 'DailyList.Processor@example.com', 'DailyList Processor', 0, NULL, true, 'Not available', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0);
INSERT INTO user_account (usr_id, dm_user_s_object_id, user_name, user_email_address, description, user_state, last_login_ts, is_system_user, account_guid, created_ts, created_by, last_modified_ts, last_modified_by)
VALUES (-3, NULL, 'AddAudio Processor', 'AddAudio.Processor@example.com', 'AddAudio Processor', 0, NULL, true, 'Not available', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0);
INSERT INTO user_account (usr_id, dm_user_s_object_id, user_name, user_email_address, description, user_state, last_login_ts, is_system_user, account_guid, created_ts, created_by, last_modified_ts, last_modified_by)
VALUES (-4, NULL, 'AddCase Processor', 'AddCase.Processor@example.com', 'AddCase Processor', 0, NULL, true, 'Not available', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0);
INSERT INTO user_account (usr_id, dm_user_s_object_id, user_name, user_email_address, description, user_state, last_login_ts, is_system_user, account_guid, created_ts, created_by, last_modified_ts, last_modified_by)
VALUES (-40, NULL, 'Xhibit', 'xhibit@hmcts.net', 'Xhibit', 0, NULL, true, 'Not available', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0);
INSERT INTO user_account (usr_id, dm_user_s_object_id, user_name, user_email_address, description, user_state, last_login_ts, is_system_user, account_guid, created_ts, created_by, last_modified_ts, last_modified_by)
VALUES (-41, NULL, 'Cpp', 'cpp@hmcts.net', 'Cpp', 0, NULL, true, 'Not available', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0);
INSERT INTO user_account (usr_id, dm_user_s_object_id, user_name, user_email_address, description, user_state, last_login_ts, is_system_user, account_guid, created_ts, created_by, last_modified_ts, last_modified_by)
VALUES (-42, NULL, 'Dar Pc', 'dar.pc@hmcts.net', 'Dar Pc', 0, NULL, true, 'Not available', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0);
INSERT INTO user_account (usr_id, dm_user_s_object_id, user_name, user_email_address, description, user_state, last_login_ts, is_system_user, account_guid, created_ts, created_by, last_modified_ts, last_modified_by)
VALUES (-43, NULL, 'Mid Tier', 'dar.midtier@hmcts.net', 'Mid Tier', 0, NULL, true, 'Not available', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0);

INSERT INTO audit_activity (aua_id, activity_name, activity_description, created_ts, created_by, last_modified_ts, last_modified_by)
VALUES (1, 'Move Courtroom', 'Move Courtroom', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0);
INSERT INTO audit_activity (aua_id, activity_name, activity_description, created_ts, created_by, last_modified_ts, last_modified_by)
VALUES (2, 'Export Audio', 'Export Audio', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0);
INSERT INTO audit_activity (aua_id, activity_name, activity_description, created_ts, created_by, last_modified_ts, last_modified_by)
VALUES (3, 'Request Audio', 'Request Audio', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0);
INSERT INTO audit_activity (aua_id, activity_name, activity_description, created_ts, created_by, last_modified_ts, last_modified_by)
VALUES (4, 'Audio Playback', 'Audio Playback', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0);
INSERT INTO audit_activity (aua_id, activity_name, activity_description, created_ts, created_by, last_modified_ts, last_modified_by)
VALUES (5, 'Apply Retention', 'Apply Retention', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0);
INSERT INTO audit_activity (aua_id, activity_name, activity_description, created_ts, created_by, last_modified_ts, last_modified_by)
VALUES (6, 'Request Transcription', 'Request Transcription', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0);
INSERT INTO audit_activity (aua_id, activity_name, activity_description, created_ts, created_by, last_modified_ts, last_modified_by)
VALUES (7, 'Import Transcription', 'Import Transcription', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0);
INSERT INTO audit_activity (aua_id, activity_name, activity_description, created_ts, created_by, last_modified_ts, last_modified_by)
VALUES (8, 'Download Transcription', 'Download Transcription', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0);
INSERT INTO audit_activity (aua_id, activity_name, activity_description, created_ts, created_by, last_modified_ts, last_modified_by)
VALUES (9, 'Authorise Transcription', 'Authorise Transcription', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0);
INSERT INTO audit_activity (aua_id, activity_name, activity_description, created_ts, created_by, last_modified_ts, last_modified_by)
VALUES (10, 'Reject Transcription', 'Reject Transcription', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0);
INSERT INTO audit_activity (aua_id, activity_name, activity_description, created_ts, created_by, last_modified_ts, last_modified_by)
VALUES (11, 'Accept Transcription', 'Accept Transcription', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0);
INSERT INTO audit_activity (aua_id, activity_name, activity_description, created_ts, created_by, last_modified_ts, last_modified_by)
VALUES (12, 'Complete Transcription', 'Complete Transcription', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0);

INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (1, '1000', '1002', 'Proceedings in chambers', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (2, '1000', '1001', 'Offences put to defendant', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (3, '1000', '1062', 'Defendant sworn-in', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (4, '1000', '1063', 'Defendant examination in-chief', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (5, '1000', '1064', 'Defendant continued', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (6, '1000', '1065', 'Defendant cross-examined by Prosecution', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (7, '1000', '1066', 'Defendant cross-examined Defence', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (8, '1000', '1067', 'Re-examination', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (9, '1000', '1068', 'Defendant released', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (10, '1000', '1069', 'Defendant recalled', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (11, '1000', '1070', 'Defendant questioned by Judge', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (12, '1000', '1003', 'Prosecution opened', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (13, '1000', '1004', 'Voir dire', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (14, '1000', '1005', 'Prosecution closed case', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (15, '1000', '1006', 'Prosecution gave closing speech', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (16, '1000', '1007', 'Defence opened case', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (17, '1000', '1009', 'Defence closed case', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (18, '1000', '1010', 'Defence gave closing speech', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (19, '1000', '1011', 'Discussion on directions to be given to the jury', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (20, '1000', '1012', 'Discussion on juror irregularity', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (21, '1000', '1014', 'Discussion on contempt of court issues', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (22, '1000', '1022', 'Application: Goodyear indication', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (23, '1000', '1024', 'Application: No case to answer', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (24, '1000', '1027', 'Judge directed defendant', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (25, '1000', '1028', 'Judge directed jury', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (26, '1000', '1029', 'Judge gave a majority direction', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (27, '1000', '1026', 'Judge summing-up', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (28, '1000', '1052', 'Jury sworn-in', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (29, '1000', '1051', 'Jury in', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (30, '1000', '1053', 'Jury out', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (31, '1000', '1054', 'Jury retired', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (32, '1000', '1055', 'Jury returned', 'DarStartHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (33, '1000', '1056', 'Jury gave verdict', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (34, '1000', '1057', 'Juror discharged', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (35, '1000', '1058', 'Jury discharged', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (36, '1000', '1059', 'Witness recalled', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (37, '1100', NULL, 'Hearing started', 'DarStartHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (38, '1200', NULL, 'Hearing ended', 'DarStopHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (39, '1400', NULL, 'Hearing paused', 'DarStopHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (40, '1500', NULL, 'Hearing resumed', 'DarStartHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (41, '2100', NULL, 'Defendant identified', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (42, '2198', '3900', 'Defendant arraigned', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (43, '2198', '3901', 'Defendant rearraigned', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (44, '2198', '3903', 'Prosecution responded', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (45, '2198', '3905', 'Defence responded', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (46, '2198', '3904', 'Mitigation', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (47, '2198', '3906', 'Discussion on ground rules', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (48, '2198', '3907', 'Discussion on basis of plea', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (49, '2198', '3918', 'Point of law raised', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (50, '2198', '3931', 'Prosecution application: Break fixture', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (51, '2198', '3932', 'Defence application: Break fixture', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (52, '2198', '3921', 'Prosecution application: Adjourn', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (53, '2198', '3986', 'Defence application: Adjourn', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (54, '2198', '3933', 'Judge directed on reporting restrictions', 'SetReportingRestrictionEventHandler', true, true, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (55, '2198', '3935', 'Judge directed Prosecution to obtain a report', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (56, '2198', '3936', 'Judge directed Defence to obtain a medical report', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (57, '2198', '3937', 'Judge directed Defence to obtain a psychiatric report', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (58, '2198', '3938', 'Judge directed Defence counsel to obtain a report', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (59, '2198', '3940', 'Judges ruling', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (60, '2198', '3934', 'Judge passed sentence', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (61, '2199', NULL, 'Prosecution application', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (62, '2201', NULL, 'Defence application', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (63, '2902', '3964', 'Application: Fitness to plead', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (64, '2906', '3968', 'Witness gave pre-recorded evidence', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (65, '2907', '3969', 'Witness read', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (66, '2908', '3970', 'Witness sworn-in', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (67, '2909', '3971', 'Witness examination in-chief', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (68, '2910', '3972', 'Witness continued', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (69, '2912', '3974', 'Re-examination', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (70, '2913', '3975', 'Witness released', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (71, '2914', '3976', 'Witness questioned by Judge', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (72, '2917', '3979', 'Interpreter sworn-in', 'InterpreterUsedHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (73, '2918', '3980', 'Intermediatory sworn-in', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (74, '2920', '3981', 'Probation gave oral PSR', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (75, '2933', '3982', 'Victim Personal Statement(s) read', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (76, '2934', '3983', 'Unspecified event', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (77, '3000', NULL, 'Archive Case', 'StopAndCloseHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (78, '3010', NULL, 'Sentence Transcription Required', 'TranscriptionRequestHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (79, '4101', NULL, 'Witness cross-examined by Defence', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (80, '4102', NULL, 'Witness cross-examined by Prosecution', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (81, '10100', NULL, 'Case called on', 'DarStartHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (82, '10200', NULL, 'Defendant attendance', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (83, '10300', NULL, 'Prosecution addresses judge', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (84, '10400', NULL, 'Defence addresses judge', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (85, '10500', NULL, 'Resume', 'DarStartHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (86, '20100', NULL, 'Bench Warrant Issued', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (87, '20101', NULL, 'Bench Warrant Executed', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (88, '20198', '13900', 'Acceptable guilty plea(s) entered late to some or all charges / counts on the charge sheet, offered for the first time by the defence.', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (89, '20198', '13901', 'Acceptable guilty plea(s) entered late to some or all charges / counts on the charge sheet, previously rejected by the prosecution.', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (90, '20198', '13902', 'Acceptable guilty plea(s) to alternative new charge (not previously on the charge sheet), first offered by defence.', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (91, '20198', '13903', 'Acceptable guilty plea(s) to alternative new charge (not previously on the charge sheet), previously rejected by the prosecution.', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (92, '20198', '13904', 'Defendant bound over, acceptable to prosecution - offered for the first by the defence.', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (93, '20198', '13905', 'Effective Trial.', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (94, '20198', '13906', 'Defendant bound over, now acceptable to prosecution - previously rejected by the prosecution', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (159, '20909', NULL, 'Defence closing speech', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (95, '20198', '13907', 'Unable to proceed with the trail because defendant incapable through alcohol/drugs', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (96, '20198', '13908', 'Defendant deceased', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (97, '20198', '13909', 'Prosecution end case: insufficient evidence', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (98, '20198', '13910', 'Prosecution end case: witness absent / withdrawn', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (99, '20198', '13911', 'Prosecution end case: public interest grounds', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (100, '20198', '13912', 'Prosecution end case: adjournment refused', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (101, '20198', '13913', 'Prosecution not ready: served late notice of additional evidence on defence', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (102, '20198', '13914', 'Prosecution not ready: specify in comments', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (103, '20198', '13915', 'Prosecution failed to disclose unused material', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (104, '20198', '13916', 'Prosecution witness adsent: police', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (105, '20198', '13917', 'Prosecution witness adsent: professional / expert', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (106, '20198', '13918', 'Prosecution witness adsent: other', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (107, '20198', '13919', 'Prosecution advocate engaged in another trial', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (108, '20198', '13920', 'Prosecution advocate failed to attend', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (109, '20198', '13921', 'Prosecution increased time estimate - insufficient time for trail to start', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (110, '20198', '13922', 'Defence not ready: disclosure problems', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (111, '20198', '13923', 'Defence not ready: specify in comments (inc. no instructions)', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (112, '20198', '13924', 'Defence asked for additional prosecution witness toattend', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (113, '20198', '13925', 'Defence witness absent', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (114, '20198', '13926', 'Defendant absent - did not proceed in absence (judicial discretion)', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (115, '20198', '13927', 'Defendant ill or otherwise unfit to proceed', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (116, '20198', '13928', 'Defendant not produced by PECS', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (117, '20198', '13929', 'Defendant absent - unable to proceed as defendant not notified of place and time of hearing', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (118, '20198', '13930', 'Defence increased time estimate - insufficient time to for trial to start', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (119, '20198', '13931', 'Defence advocate engaged in other trial', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (120, '20198', '13932', 'Defence advocate failed to attend', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (121, '20198', '13933', 'Defence dismissed advocate', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (122, '20198', '13934', 'Another case over-ran', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (123, '20198', '13935', 'Judge / magistrate availability', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (124, '20198', '13936', 'Case not reached / insufficient cases drop out / floater not reached', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (125, '20198', '13937', 'Equipment / accomodation failure', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (126, '20198', '13938', 'No interpreter available', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (127, '20198', '13939', 'Insufficient jurors available', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (128, '20198', '13940', 'Outstanding committals in a magistrates court', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (129, '20198', '13941', 'Outstanding committals in a Crown Court centre', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (130, '20200', NULL, 'Bail and custody', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (131, '20501', NULL, 'Indictment to be filed', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (132, '20502', NULL, 'List from plea and direction hearing', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (133, '20503', NULL, 'Certify readiness for trial', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (134, '20504', NULL, 'Directions form completed', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (135, '20601', NULL, 'Appellant attendance', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (136, '20602', NULL, 'Respondant case opened', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (137, '20603', NULL, 'Appeal witness sworn in', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (138, '20604', NULL, 'Appeal witness released', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (139, '20605', NULL, 'Respondant case closed', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (140, '20606', NULL, 'Appellant case opened', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (141, '20607', NULL, 'Appellant submissions', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (142, '20608', NULL, 'Appellant case closed', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (143, '20609', NULL, 'Bench retires', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (144, '20612', NULL, 'Appeal interpreter sworn in', 'InterpreterUsedHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (145, '20613', NULL, 'Appeal witness continues', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (146, '20701', NULL, 'Application to stand out', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (147, '20702', NULL, 'Defence application', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (148, '20703', NULL, 'Judges ruling', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (149, '20704', NULL, 'Prosecution application', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (150, '20705', NULL, 'Other application', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (151, '20901', NULL, 'Time estimate', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (152, '20902', NULL, 'Jury sworn in', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (153, '20903', NULL, 'Prosecution opening', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (154, '20904', NULL, 'Witness sworn in', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (155, '20905', NULL, 'Witness released', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (156, '20906', NULL, 'Defence case opened', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (157, '20907', NULL, 'Prosecution closing speech', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (158, '20908', NULL, 'Prosecution case closed', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (160, '20910', NULL, 'Defence case closed', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (161, '20911', NULL, 'Summing up', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (162, '20912', NULL, 'Jury out', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (163, '20913', NULL, 'Jury returns', 'DarStartHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (164, '20914', NULL, 'Jury retire', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (165, '20915', NULL, 'Jury/Juror discharged', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (166, '20916', NULL, 'Judge addresses advocate', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (167, '20917', NULL, 'Interpretor sworn', 'InterpreterUsedHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (168, '20918', NULL, 'Cracked or ineffective trial', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (169, '20920', NULL, 'Witness continued', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (170, '20933', '10622', 'Judge sentences', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (171, '20934', '10623', 'Special measures application', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (172, '20935', '10630', 'Witness Read', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (173, '20935', '10631', 'Defendant Read', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (174, '20935', '10632', 'Interpreter Read', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (175, '20935', '10633', 'Apellant Read', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (176, '20936', '10630', 'Witness Read', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (177, '20936', '10631', 'Defendant Read', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (178, '20936', '10632', 'Interpreter Read', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (179, '20936', '10633', 'Apellant Read', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (180, '21200', '10311', 'Bail Conditions Ceased - sentence deferred', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (181, '21200', '10312', 'Bail Conditions Ceased - defendant deceased', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (182, '21200', '10313', 'Bail Conditions Ceased - non-custodial sentence imposed', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (183, '21200', '11000', 'Section 4(2) of the Contempt of Court Act 1981', 'SetReportingRestrictionEventHandler', true, true, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (184, '21200', '11001', 'Section 11 of the Contempt of Court Act 1981', 'SetReportingRestrictionEventHandler', true, true, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (185, '21200', '11002', 'Section 39 of the Children and Young Persons Act 1933', 'SetReportingRestrictionEventHandler', true, true, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (186, '21200', '11003', 'Section 4 of the Sexual Offenders (Amendment) Act 1976', 'SetReportingRestrictionEventHandler', true, true, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (187, '21200', '11004', 'Section 2 of the Sexual Offenders (Amendment) Act 1992', 'SetReportingRestrictionEventHandler', true, true, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (188, '21200', '11006', 'An order made under s45 of the Youth Justice and Criminal Evidence Act 1999', 'SetReportingRestrictionEventHandler', true, true, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (189, '21200', '11007', 'An order made under s45a of the Youth Justice and Criminal Evidence Act 1999', 'SetReportingRestrictionEventHandler', true, true, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (190, '21200', '11008', 'An order made under s46 of the Youth Justice and Criminal Evidence Act 1999', 'SetReportingRestrictionEventHandler', true, true, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (191, '21200', '11009', 'An order made under s49 of the Children and Young Persons Act 1933', 'SetReportingRestrictionEventHandler', true, true, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (192, '21201', NULL, 'Restrictions lifted', 'SetReportingRestrictionEventHandler', true, true, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (193, '21300', NULL, 'Freetext', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (194, '21400', '12414', 'Defendant disqualified from working with children for life (Defendant under 18)', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (195, '21400', '12415', 'Defendant disqualified from working with children for life (Defendant over 18)', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (196, '21500', '13700', 'Defendant ordered to be electronically monitored', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (197, '21500', '13701', 'Electronic monitoring requirement amended', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (198, '21500', '13702', 'Electronic monitoring/tag to be removed', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (199, '21500', '13703', 'Defendant subject to a electronically monitored curfew', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (200, '21500', '13704', 'Terms of electronically monitored curfew amended', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (201, '21500', '13705', 'Requirement for an electronically monitored curfew removed', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (202, '21600', '13600', 'Sex Offenders Register - victim under 18 years of age - for an indefinite period', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (203, '21600', '13601', 'Sex Offenders Register - victim under 18 years of age - for 10 years', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (204, '21600', '13602', 'Sex Offenders Register - victim under 18 years of age - for 3-7 years', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (205, '21600', '13603', 'Sex Offenders Register - victim under 18 years of age - period to be specified later', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (206, '21600', '13604', 'Sex Offenders Register - victim under 18 years of age - for another period', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (207, '21600', '13605', 'Sex Offenders Register - victim over 18 years of age - for an indefinite period', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (208, '21600', '13606', 'Sex Offenders Register - victim over 18 years of age - for 10 years', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (209, '21600', '13607', 'Sex Offenders Register - victim over 18 years of age - for 3-7 years', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (210, '21600', '13608', 'Sex Offenders Register - victim over 18 years of age - for another period', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (211, '21600', '13609', 'Sex Offenders Register - victim over 18 years of age - period to be specified later', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (212, '21800', '12310', 'Disqualification from driving removed (3076)', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (213, '30100', NULL, 'Short adjournment', 'DarStopHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (214, '30300', NULL, 'Case closed', 'DarStopHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (215, '30500', NULL, 'Hearing ended', 'StopAndCloseHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (216, '30600', NULL, 'Hearing ended', 'StopAndCloseHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (217, '30601', '11113', 'Delete end hearing', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (218, '40203', NULL, 'Join indictments', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (219, '40410', NULL, 'Maintain charges', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (220, '40601', NULL, '7/14 day orders', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (221, '40706', '10305', 'Remanded in Custody', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (222, '40706', '10308', 'Bail as before', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (223, '40706', '10309', 'Bail varied', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (224, '40711', NULL, 'Time estimate supplied', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (225, '40720', NULL, 'Verdict', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (226, '40721', NULL, 'Verdict', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (227, '40722', NULL, 'Verdict', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (228, '40725', NULL, 'Verdict', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (229, '40726', NULL, 'Verdict', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (230, '40727', NULL, 'Verdict', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (231, '40730', NULL, 'Verdict', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (232, '40730', '10808', 'Case Level Criminal Appeal Result', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (233, '40731', NULL, 'Verdict', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (234, '40731', '10808', 'Offence Level Criminal Appeal Result', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (235, '40732', NULL, 'Verdict', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (236, '40732', '10808', 'Offence Level Criminal Appeal Result with alt offence', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (237, '40733', NULL, 'Verdict', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (238, '40733', '10808', 'Case Level Misc Appeal Result', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (239, '40735', '10808', 'Delete Offence Level Appeal Result', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (240, '40735', NULL, 'Verdict', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (241, '40736', NULL, 'Verdict', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (242, '40737', NULL, 'Verdict', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (243, '40738', NULL, 'Verdict', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (244, '40750', '12309', 'Driving disqualification suspended pending appeal subsequent to imposition (3075)', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (245, '40750', NULL, 'Sentencing', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (246, '40750', '11504', 'Life Imprisonment', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (247, '40750', '11505', 'Life Imprisonment (with minimum period)', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (248, '40750', '11506', 'Custody for Life', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (249, '40750', '11507', 'Mandatory Life Sentence for Second Serious Offence', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (376, 'UPDCASE', NULL, 'Update Case', 'com.synapps.moj.dfs.handler.DARTSXHIBITUpdateCaseHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (250, '40750', '11508', 'Mandatory Life Sentence for Second Serious Offence (Young Offender)', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (251, '40750', '11509', 'Detained During Her Majestys Pleasure', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (252, '40750', '11521', 'INIMP: Indeterminate Sentence of Imprisonment for Public Protection', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (253, '40750', '11522', 'INDET: Indeterminate Sentence of Detention for Public Protection', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (254, '40750', '11523', 'Mandatory Life Sentence for Second Listed Offence', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (255, '40750', '11524', 'Mandatory Life Sentence for Second Listed Offence (Young Offender)', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (256, '40750', '12400', 'Disqualification Order (from working with children) - ADULTS', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (257, '40750', '12401', 'Disqualification Order (from working with children) - JUVENILES', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (258, '40750', '13505', 'S226b Extended Discretional for under 18', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (259, '40750', '13506', 'S226b Extended Automatic for under 18', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (260, '40750', '13503', 'S226a Extended Discretional for over 18', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (261, '40750', '13504', 'S226a Extended Automatic for over 18', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (262, '40750', '11529', 'Detention for Life under s226 (u18)', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (263, '40750', '11525', 'Imprisonment - Extended under s236A CJA2003', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (264, '40750', '11526', 'Imprisonment - Minimum Imposed after 3 strikes (Young Offender) - Extended under s236A CJA2003', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (265, '40750', '11527', 'Imprisonment - Minimum Imposed after 3 strikes - Extended under s236A CJA2003', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (266, '40750', '11528', 'Detention in Y.O.I. - Extended under s235A CJA2003', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (267, '40750', '11533', 'Imprisonment for life (adult) for manslaughter of an emergency worker', 'com.synapps.moj.dfs.handler.eventtransrequest.SentencingRemarksHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (268, '40750', '11534', 'Detention for life (youth) for manslaughter of an emergency worker', 'com.synapps.moj.dfs.handler.eventtransrequest.SentencingRemarksHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (269, '40750', '13507', '(Extended Discretional 18 to 20)   Section 266', 'com.synapps.moj.dfs.handler.eventtransrequest.SentencingRemarksHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (270, '40750', '13508', '(Extended Discretional over 21)', 'com.synapps.moj.dfs.handler.eventtransrequest.SentencingRemarksHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (271, '40751', '11528', 'Detention in Y.O.I. - Extended under s235A CJA2003', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (272, '40751', NULL, 'Sentencing', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (368, '407131', NULL, 'Case to be listed', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (273, '40751', '11504', 'Life Imprisonment', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (274, '40751', '11505', 'Life Imprisonment (with minimum period)', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (275, '40751', '11506', 'Custody for Life', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (276, '40751', '11507', 'Mandatory Life Sentence for Second Serious Offence', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (277, '40751', '11508', 'Mandatory Life Sentence for Second Serious Offence (Young Offender)', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (278, '40751', '11509', 'Detained During Her Majestys Pleasure', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (279, '40751', '11521', 'INIMP: Indeterminate Sentence of Imprisonment for Public Protection', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (280, '40751', '11522', 'INDET: Indeterminate Sentence of Detention for Public Protection', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (281, '40751', '11523', 'Mandatory Life Sentence for Second Listed Offence', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (282, '40751', '11524', 'Mandatory Life Sentence for Second Listed Offence (Young Offender)', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (283, '40751', '12400', 'Disqualification Order (from working with children) - ADULTS', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (284, '40751', '12401', 'Disqualification Order (from working with children) - JUVENILES', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (285, '40751', '13505', 'S226b Extended Discretional for under 18', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (286, '40751', '13506', 'S226b Extended Automatic for under 18', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (287, '40751', '13503', 'S226a Extended Discretional for over 18', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (377, 'LOG', NULL, 'LOG', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (288, '40751', '13504', 'S226a Extended Automatic for over 18', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (289, '40751', '11529', 'Detention for Life under s226 (u18)', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (290, '40751', '11525', 'Imprisonment - Extended under s236A CJA2003', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (291, '40751', '11526', 'Imprisonment - Minimum Imposed after 3 strikes (Young Offender) - Extended under s236A CJA2003', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (292, '40751', '11527', 'Imprisonment - Minimum Imposed after 3 strikes - Extended under s236A CJA2003', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (293, '40751', '11533', 'Imprisonment for life (adult) for manslaughter of an emergency worker', 'com.synapps.moj.dfs.handler.eventtransrequest.SentencingRemarksHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (294, '40751', '11534', 'Detention for life (youth) for manslaughter of an emergency worker', 'com.synapps.moj.dfs.handler.eventtransrequest.SentencingRemarksHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (295, '40751', '13507', '(Extended Discretional 18 to 20)   Section 266', 'com.synapps.moj.dfs.handler.eventtransrequest.SentencingRemarksHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (296, '40751', '13508', '(Extended Discretional over 21)', 'com.synapps.moj.dfs.handler.eventtransrequest.SentencingRemarksHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (297, '40752', '11528', 'Detention in Y.O.I. - Extended under s235A CJA2003', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (298, '40752', NULL, 'Sentencing', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (299, '40752', '11504', 'Life Imprisonment', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (300, '40752', '11505', 'Life Imprisonment (with minimum period)', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (301, '40752', '11506', 'Custody for Life', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (302, '40752', '11507', 'Mandatory Life Sentence for Second Serious Offence', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (303, '40752', '11508', 'Mandatory Life Sentence for Second Serious Offence (Young Offender)', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (304, '40752', '11509', 'Detained During Her Majestys Pleasure', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (305, '40752', '11521', 'INIMP: Indeterminate Sentence of Imprisonment for Public Protection', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (306, '40752', '11522', 'INDET: Indeterminate Sentence of Detention for Public Protection', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (307, '40752', '11523', 'Mandatory Life Sentence for Second Listed Offence', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (308, '40752', '11524', 'Mandatory Life Sentence for Second Listed Offence (Young Offender)', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (309, '40752', '12400', 'Disqualification Order (from working with children) - ADULTS', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (310, '40752', '12401', 'Disqualification Order (from working with children) - JUVENILES', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (311, '40752', '13505', 'S226b Extended Discretional for under 18', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (312, '40752', '13506', 'S226b Extended Automatic for under 18', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (313, '40752', '13503', 'S226a Extended Discretional for over 18', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (314, '40752', '13504', 'S226a Extended Automatic for over 18', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (315, '40752', '11529', 'Detention for Life under s226 (u18)', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (316, '40752', '11525', 'Imprisonment - Extended under s236A CJA2003', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (369, '407132', NULL, 'Case to be listed', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (317, '40752', '11526', 'Imprisonment - Minimum Imposed after 3 strikes (Young Offender) - Extended under s236A CJA2003', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (318, '40752', '11527', 'Imprisonment - Minimum Imposed after 3 strikes - Extended under s236A CJA2003', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (319, '40752', '11533', 'Imprisonment for life (adult) for manslaughter of an emergency worker', 'com.synapps.moj.dfs.handler.eventtransrequest.SentencingRemarksHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (320, '40752', '11534', 'Detention for life (youth) for manslaughter of an emergency worker', 'com.synapps.moj.dfs.handler.eventtransrequest.SentencingRemarksHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (321, '40752', '13507', '(Extended Discretional 18 to 20)   Section 266', 'com.synapps.moj.dfs.handler.eventtransrequest.SentencingRemarksHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (322, '40752', '13508', '(Extended Discretional over 21)', 'com.synapps.moj.dfs.handler.eventtransrequest.SentencingRemarksHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (323, '40753', '11528', 'Detention in Y.O.I. - Extended under s235A CJA2003', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (324, '40753', NULL, 'Sentencing', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (325, '40753', '11504', 'Life Imprisonment', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (326, '40753', '11505', 'Life Imprisonment (with minimum period)', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (327, '40753', '11506', 'Custody for Life', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (328, '40753', '11507', 'Mandatory Life Sentence for Second Serious Offence', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (329, '40753', '11508', 'Mandatory Life Sentence for Second Serious Offence (Young Offender)', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (330, '40753', '11509', 'Detained During Her Majestys Pleasure', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (331, '40753', '11521', 'INIMP: Indeterminate Sentence of Imprisonment for Public Protection', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (332, '40753', '11522', 'INDET: Indeterminate Sentence of Detention for Public Protection', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (333, '40753', '11523', 'Mandatory Life Sentence for Second Listed Offence', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (334, '40753', '11524', 'Mandatory Life Sentence for Second Listed Offence (Young Offender)', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (335, '40753', '12400', 'Disqualification Order (from working with children) - ADULTS', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (336, '40753', '12401', 'Disqualification Order (from working with children) - JUVENILES', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (337, '40753', '13505', 'S226b Extended Discretional for under 18', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (338, '40753', '13506', 'S226b Extended Automatic for under 18', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (339, '40753', '13503', 'S226a Extended Discretional for over 18', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (340, '40753', '13504', 'S226a Extended Automatic for over 18', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (341, '40753', '11529', 'Detention for Life under s226 (u18)', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (342, '40753', '11525', 'Imprisonment - Extended under s236A CJA2003', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (343, '40753', '11526', 'Imprisonment - Minimum Imposed after 3 strikes (Young Offender) - Extended under s236A CJA2003', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (344, '40753', '11527', 'Imprisonment - Minimum Imposed after 3 strikes - Extended under s236A CJA2003', 'com.synapps.moj.dfs.handler.eventtransrequest.SenencingRemarksAndRetentionPolicyHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (345, '40753', '11533', 'Imprisonment for life (adult) for manslaughter of an emergency worker', 'com.synapps.moj.dfs.handler.eventtransrequest.SentencingRemarksHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (346, '40753', '11534', 'Detention for life (youth) for manslaughter of an emergency worker', 'com.synapps.moj.dfs.handler.eventtransrequest.SentencingRemarksHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (347, '40753', '13507', '(Extended Discretional 18 to 20)   Section 266', 'com.synapps.moj.dfs.handler.eventtransrequest.SentencingRemarksHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (348, '40753', '13508', '(Extended Discretional over 21)', 'com.synapps.moj.dfs.handler.eventtransrequest.SentencingRemarksHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (349, '40754', NULL, 'Sentencing', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (350, '40754', '11533', 'Imprisonment for life (adult) for manslaughter of an emergency worker', 'com.synapps.moj.dfs.handler.eventtransrequest.SentencingRemarksHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (351, '40754', '11534', 'Detention for life (youth) for manslaughter of an emergency worker', 'com.synapps.moj.dfs.handler.eventtransrequest.SentencingRemarksHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (352, '40754', '13507', '(Extended Discretional 18 to 20)   Section 266', 'com.synapps.moj.dfs.handler.eventtransrequest.SentencingRemarksHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (353, '40754', '13508', '(Extended Discretional over 21)', 'com.synapps.moj.dfs.handler.eventtransrequest.SentencingRemarksHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (354, '40755', NULL, 'Sentencing', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (355, '40756', NULL, 'Guilty', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (357, '40791', NULL, 'Recommended for Deportation', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (358, '60101', NULL, 'Plea', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (359, '60102', NULL, 'Plea', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (360, '60103', NULL, 'Plea', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (361, '60104', NULL, 'Plea', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (362, '60106', '11317', 'Admitted ( Bail Act Offence)', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (363, '60106', '11318', 'Not Admitted ( Bail Act Offence)', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (364, '302001', NULL, 'Long adjournment', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (365, '302002', NULL, 'Adjourned for pre-sentence report', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (366, '302003', NULL, 'Case reserved', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (367, '302004', NULL, 'Case not reserved', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (370, 'CPPDL', 'CPPDL', 'CPP Daily List', 'com.synapps.moj.dfs.handler.DARTSDailyListFromCPPHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (371, 'DETTO', '11531', 'Special Sentence of Detention for Terrorist Offenders of Particular Concern', 'com.synapps.moj.dfs.handler.eventtransrequest.SentencingRemarksHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (372, 'DL', NULL, 'Daily List', 'com.synapps.moj.dfs.handler.DARTSDailyListHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (373, 'NEWCASE', NULL, 'New Case', 'com.synapps.moj.dfs.handler.DARTSXHIBITNewCaseHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (374, 'STS', '11530', 'Serious Terrorism Sentence', 'com.synapps.moj.dfs.handler.eventtransrequest.SentencingRemarksHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (375, 'STS1821', '11532', 'Serious Terrorism Sentence 18 to 21', 'com.synapps.moj.dfs.handler.eventtransrequest.SentencingRemarksHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (378, 'Event_Type_728', 'Event_Sub_Type_728', 'DMP-728 Test', 'StandardEventHandler', false, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (379, 'Event_Type_728', 'Event_Sub_Type_728', 'DMP-728 Test', 'StandardEventHandler', false, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (380, 'Event_Type_728', 'Event_Sub_Type_728', 'DMP-728 Test', 'StandardEventHandler', false, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (381, 'Event_Type_728', 'Event_Sub_Type_728', 'DMP-728 Test', 'StandardEventHandler', false, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (382, 'Event_Type_728', 'Event_Sub_Type_728', 'DMP-728 Test', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (383, 'NewEvent_Type_728', 'NewEvent_Sub_Type_728', 'DMP-856 Test', 'StandardEventHandler', true, false, 0, current_timestamp);
INSERT INTO event_handler (evh_id, event_type, event_sub_type, event_name, handler, active, is_reporting_restriction, created_by, created_ts)
VALUES (356, '40790', NULL, 'Results', 'DartsEventNullHandler', true, false, 0, current_timestamp);

INSERT INTO external_location_type (elt_id, elt_description)
VALUES (1, 'inbound');
INSERT INTO external_location_type (elt_id, elt_description)
VALUES (2, 'unstructured');
INSERT INTO external_location_type (elt_id, elt_description)
VALUES (3, 'arm');
INSERT INTO external_location_type (elt_id, elt_description)
VALUES (4, 'tempstore');
INSERT INTO external_location_type (elt_id, elt_description)
VALUES (5, 'vodafone');

INSERT INTO object_record_status (ors_id, ors_description)
VALUES (1, 'New');
INSERT INTO object_record_status (ors_id, ors_description)
VALUES (2, 'Stored');
INSERT INTO object_record_status (ors_id, ors_description)
VALUES (3, 'Failure');
INSERT INTO object_record_status (ors_id, ors_description)
VALUES (4, 'Failure - File not found');
INSERT INTO object_record_status (ors_id, ors_description)
VALUES (5, 'Failure - File size check failed');
INSERT INTO object_record_status (ors_id, ors_description)
VALUES (6, 'Failure - File type check failed');
INSERT INTO object_record_status (ors_id, ors_description)
VALUES (7, 'Failure - Checksum failed');
INSERT INTO object_record_status (ors_id, ors_description)
VALUES (8, 'Failure - ARM ingestion failed');
INSERT INTO object_record_status (ors_id, ors_description)
VALUES (9, 'Awaiting Verification');
INSERT INTO object_record_status (ors_id, ors_description)
VALUES (10, 'marked for Deletion');
INSERT INTO object_record_status (ors_id, ors_description)
VALUES (11, 'Deleted');
INSERT INTO object_record_status (ors_id, ors_description)
VALUES (12, 'Arm Ingestion');
INSERT INTO object_record_status (ors_id, ors_description)
VALUES (13, 'Arm Drop Zone');
INSERT INTO object_record_status (ors_id, ors_description)
VALUES (14, 'Arm Raw Data Failed');
INSERT INTO object_record_status (ors_id, ors_description)
VALUES (15, 'Arm Manifest Failed');

INSERT INTO security_permission (per_id, permission_name)
VALUES (1, 'ACCEPT_TRANSCRIPTION_JOB_REQUEST');
INSERT INTO security_permission (per_id, permission_name)
VALUES (2, 'APPROVE_REJECT_TRANSCRIPTION_REQUEST');
INSERT INTO security_permission (per_id, permission_name)
VALUES (3, 'LISTEN_TO_AUDIO_FOR_DOWNLOAD');
INSERT INTO security_permission (per_id, permission_name)
VALUES (4, 'LISTEN_TO_AUDIO_FOR_PLAYBACK');
INSERT INTO security_permission (per_id, permission_name)
VALUES (5, 'READ_JUDGES_NOTES');
INSERT INTO security_permission (per_id, permission_name)
VALUES (6, 'READ_TRANSCRIBED_DOCUMENT');
INSERT INTO security_permission (per_id, permission_name)
VALUES (7, 'REQUEST_AUDIO');
INSERT INTO security_permission (per_id, permission_name)
VALUES (8, 'REQUEST_TRANSCRIPTION');
INSERT INTO security_permission (per_id, permission_name)
VALUES (9, 'RETENTION_ADMINISTRATION');
INSERT INTO security_permission (per_id, permission_name)
VALUES (10, 'SEARCH_CASES');
INSERT INTO security_permission (per_id, permission_name)
VALUES (11, 'UPLOAD_JUDGES_NOTES');
INSERT INTO security_permission (per_id, permission_name)
VALUES (12, 'UPLOAD_TRANSCRIPTION');
INSERT INTO security_permission (per_id, permission_name)
VALUES (13, 'VIEW_DARTS_INBOX');
INSERT INTO security_permission (per_id, permission_name)
VALUES (14, 'VIEW_MY_AUDIOS');
INSERT INTO security_permission (per_id, permission_name)
VALUES (15, 'VIEW_MY_TRANSCRIPTIONS');
INSERT INTO security_permission (per_id, permission_name)
VALUES (16, 'EXPORT_PROCESSED_PLAYBACK_AUDIO');
INSERT INTO security_permission (per_id, permission_name)
VALUES (17, 'EXPORT_PROCESSED_DOWNLOAD_AUDIO');
INSERT INTO security_permission (per_id, permission_name)
VALUES (18, 'ADD_DOCUMENT');
INSERT INTO security_permission (per_id, permission_name)
VALUES (19, 'GET_CASES');
INSERT INTO security_permission (per_id, permission_name)
VALUES (20, 'REGISTER_NODE');
INSERT INTO security_permission (per_id, permission_name)
VALUES (21, 'ADD_CASE');
INSERT INTO security_permission (per_id, permission_name)
VALUES (22, 'ADD_LOG_ENTRY');
INSERT INTO security_permission (per_id, permission_name)
VALUES (23, 'ADD_AUDIO');

INSERT INTO security_role (rol_id, role_name, display_name, display_state)
VALUES (1, 'APPROVER', 'Approver', true);
INSERT INTO security_role (rol_id, role_name, display_name, display_state)
VALUES (2, 'REQUESTER', 'Requester', true);
INSERT INTO security_role (rol_id, role_name, display_name, display_state)
VALUES (3, 'JUDGE', 'Judge', true);
INSERT INTO security_role (rol_id, role_name, display_name, display_state)
VALUES (4, 'TRANSCRIBER', 'Transcriber', true);
INSERT INTO security_role (rol_id, role_name, display_name, display_state)
VALUES (5, 'TRANSLATION_QA', 'Translation QA', true);
INSERT INTO security_role (rol_id, role_name, display_name, display_state)
VALUES (6, 'RCJ_APPEALS', 'RCJ Appeals', true);
INSERT INTO security_role (rol_id, role_name, display_name, display_state)
VALUES (7, 'XHIBIT', 'XHIBIT', true);
INSERT INTO security_role (rol_id, role_name, display_name, display_state)
VALUES (8, 'CPP', 'CPP', true);
INSERT INTO security_role (rol_id, role_name, display_name, display_state)
VALUES (9, 'DAR_PC', 'DAR PC', true);
INSERT INTO security_role (rol_id, role_name, display_name, display_state)
VALUES (10, 'MID_TIER', 'Mid Tier', true);
INSERT INTO security_role (rol_id, role_name, display_name, display_state)
VALUES (11, 'ADMIN', 'Admin', true);

INSERT INTO security_role_permission_ae (rol_id, per_id)
VALUES (1, 2);
INSERT INTO security_role_permission_ae (rol_id, per_id)
VALUES (1, 4);
INSERT INTO security_role_permission_ae (rol_id, per_id)
VALUES (1, 6);
INSERT INTO security_role_permission_ae (rol_id, per_id)
VALUES (1, 7);
INSERT INTO security_role_permission_ae (rol_id, per_id)
VALUES (1, 8);
INSERT INTO security_role_permission_ae (rol_id, per_id)
VALUES (1, 9);
INSERT INTO security_role_permission_ae (rol_id, per_id)
VALUES (1, 10);
INSERT INTO security_role_permission_ae (rol_id, per_id)
VALUES (1, 13);
INSERT INTO security_role_permission_ae (rol_id, per_id)
VALUES (1, 14);
INSERT INTO security_role_permission_ae (rol_id, per_id)
VALUES (1, 15);
INSERT INTO security_role_permission_ae (rol_id, per_id)
VALUES (1, 16);
INSERT INTO security_role_permission_ae (rol_id, per_id)
VALUES (2, 4);
INSERT INTO security_role_permission_ae (rol_id, per_id)
VALUES (2, 6);
INSERT INTO security_role_permission_ae (rol_id, per_id)
VALUES (2, 7);
INSERT INTO security_role_permission_ae (rol_id, per_id)
VALUES (2, 8);
INSERT INTO security_role_permission_ae (rol_id, per_id)
VALUES (2, 9);
INSERT INTO security_role_permission_ae (rol_id, per_id)
VALUES (2, 10);
INSERT INTO security_role_permission_ae (rol_id, per_id)
VALUES (2, 13);
INSERT INTO security_role_permission_ae (rol_id, per_id)
VALUES (2, 14);
INSERT INTO security_role_permission_ae (rol_id, per_id)
VALUES (2, 15);
INSERT INTO security_role_permission_ae (rol_id, per_id)
VALUES (2, 16);
INSERT INTO security_role_permission_ae (rol_id, per_id)
VALUES (3, 4);
INSERT INTO security_role_permission_ae (rol_id, per_id)
VALUES (3, 5);
INSERT INTO security_role_permission_ae (rol_id, per_id)
VALUES (3, 6);
INSERT INTO security_role_permission_ae (rol_id, per_id)
VALUES (3, 7);
INSERT INTO security_role_permission_ae (rol_id, per_id)
VALUES (3, 8);
INSERT INTO security_role_permission_ae (rol_id, per_id)
VALUES (3, 9);
INSERT INTO security_role_permission_ae (rol_id, per_id)
VALUES (3, 10);
INSERT INTO security_role_permission_ae (rol_id, per_id)
VALUES (3, 11);
INSERT INTO security_role_permission_ae (rol_id, per_id)
VALUES (3, 13);
INSERT INTO security_role_permission_ae (rol_id, per_id)
VALUES (3, 14);
INSERT INTO security_role_permission_ae (rol_id, per_id)
VALUES (3, 15);
INSERT INTO security_role_permission_ae (rol_id, per_id)
VALUES (3, 16);
INSERT INTO security_role_permission_ae (rol_id, per_id)
VALUES (4, 1);
INSERT INTO security_role_permission_ae (rol_id, per_id)
VALUES (4, 3);
INSERT INTO security_role_permission_ae (rol_id, per_id)
VALUES (4, 4);
INSERT INTO security_role_permission_ae (rol_id, per_id)
VALUES (4, 6);
INSERT INTO security_role_permission_ae (rol_id, per_id)
VALUES (4, 7);
INSERT INTO security_role_permission_ae (rol_id, per_id)
VALUES (4, 10);
INSERT INTO security_role_permission_ae (rol_id, per_id)
VALUES (4, 12);
INSERT INTO security_role_permission_ae (rol_id, per_id)
VALUES (4, 13);
INSERT INTO security_role_permission_ae (rol_id, per_id)
VALUES (4, 14);
INSERT INTO security_role_permission_ae (rol_id, per_id)
VALUES (4, 15);
INSERT INTO security_role_permission_ae (rol_id, per_id)
VALUES (4, 16);
INSERT INTO security_role_permission_ae (rol_id, per_id)
VALUES (4, 17);
INSERT INTO security_role_permission_ae (rol_id, per_id)
VALUES (5, 4);
INSERT INTO security_role_permission_ae (rol_id, per_id)
VALUES (5, 6);
INSERT INTO security_role_permission_ae (rol_id, per_id)
VALUES (5, 7);
INSERT INTO security_role_permission_ae (rol_id, per_id)
VALUES (5, 10);
INSERT INTO security_role_permission_ae (rol_id, per_id)
VALUES (5, 13);
INSERT INTO security_role_permission_ae (rol_id, per_id)
VALUES (5, 14);
INSERT INTO security_role_permission_ae (rol_id, per_id)
VALUES (5, 16);
INSERT INTO security_role_permission_ae (rol_id, per_id)
VALUES (6, 4);
INSERT INTO security_role_permission_ae (rol_id, per_id)
VALUES (6, 6);
INSERT INTO security_role_permission_ae (rol_id, per_id)
VALUES (6, 7);
INSERT INTO security_role_permission_ae (rol_id, per_id)
VALUES (6, 10);
INSERT INTO security_role_permission_ae (rol_id, per_id)
VALUES (6, 13);
INSERT INTO security_role_permission_ae (rol_id, per_id)
VALUES (6, 14);
INSERT INTO security_role_permission_ae (rol_id, per_id)
VALUES (6, 16);
INSERT INTO security_role_permission_ae (rol_id, per_id)
VALUES (7, 16);
INSERT INTO security_role_permission_ae (rol_id, per_id)
VALUES (8, 16);
INSERT INTO security_role_permission_ae (rol_id, per_id)
VALUES (9, 17);
INSERT INTO security_role_permission_ae (rol_id, per_id)
VALUES (9, 18);
INSERT INTO security_role_permission_ae (rol_id, per_id)
VALUES (10, 18);
INSERT INTO security_role_permission_ae (rol_id, per_id)
VALUES (10, 19);
INSERT INTO security_role_permission_ae (rol_id, per_id)
VALUES (10, 20);
INSERT INTO security_role_permission_ae (rol_id, per_id)
VALUES (10, 21);

INSERT INTO security_group (grp_id, rol_id, group_name, group_display_name, global_access, display_state)
VALUES (nextval('grp_seq'), 11, 'ADMIN', 'Admin', true, true);

INSERT INTO transcription_status (trs_id, status_type, display_name)
VALUES (1, 'Requested', 'Requested');
INSERT INTO transcription_status (trs_id, status_type, display_name)
VALUES (2, 'Awaiting Authorisation', 'Awaiting Authorisation');
INSERT INTO transcription_status (trs_id, status_type, display_name)
VALUES (3, 'Approved', 'Approved');
INSERT INTO transcription_status (trs_id, status_type, display_name)
VALUES (4, 'Rejected', 'Rejected');
INSERT INTO transcription_status (trs_id, status_type, display_name)
VALUES (5, 'With Transcriber', 'With Transcriber');
INSERT INTO transcription_status (trs_id, status_type, display_name)
VALUES (6, 'Complete', 'Complete');
INSERT INTO transcription_status (trs_id, status_type, display_name)
VALUES (7, 'Closed', 'Closed');

INSERT INTO transcription_type (trt_id, description, display_state)
VALUES (1, 'Sentencing remarks', true);
INSERT INTO transcription_type (trt_id, description, display_state)
VALUES (2, 'Summing up (including verdict)', true);
INSERT INTO transcription_type (trt_id, description, display_state)
VALUES (3, 'Antecedents', true);
INSERT INTO transcription_type (trt_id, description, display_state)
VALUES (4, 'Argument and submission of ruling', true);
INSERT INTO transcription_type (trt_id, description, display_state)
VALUES (5, 'Court Log', true);
INSERT INTO transcription_type (trt_id, description, display_state)
VALUES (6, 'Mitigation', true);
INSERT INTO transcription_type (trt_id, description, display_state)
VALUES (7, 'Proceedings after verdict', true);
INSERT INTO transcription_type (trt_id, description, display_state)
VALUES (8, 'Prosecution opening of facts', true);
INSERT INTO transcription_type (trt_id, description, display_state)
VALUES (9, 'Specified Times', true);
INSERT INTO transcription_type (trt_id, description, display_state)
VALUES (999, 'Other', true);

INSERT INTO transcription_urgency (tru_id, description, display_state, priority_order)
VALUES (1, 'Standard', false, 999);
INSERT INTO transcription_urgency (tru_id, description, display_state, priority_order)
VALUES (2, 'Overnight', true, 1);
INSERT INTO transcription_urgency (tru_id, description, display_state, priority_order)
VALUES (3, 'Other', true, 6);
INSERT INTO transcription_urgency (tru_id, description, display_state, priority_order)
VALUES (4, 'Up to 3 working days', true, 3);
INSERT INTO transcription_urgency (tru_id, description, display_state, priority_order)
VALUES (5, 'Up to 7 working days', true, 4);
INSERT INTO transcription_urgency (tru_id, description, display_state, priority_order)
VALUES (6, 'Up to 12 working days', true, 5);
INSERT INTO transcription_urgency (tru_id, description, display_state, priority_order)
VALUES (7, 'Up to 2 working days', true, 2);
