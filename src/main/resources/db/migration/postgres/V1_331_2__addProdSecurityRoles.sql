update security_role set role_name='REQUESTER' where role_name='REQUESTOR';
update security_role set role_name='Requester' where role_name='Requestor';
INSERT INTO security_role(rol_id, role_name, display_name, display_state) VALUES (1,'JUDICIARY','Judiciary',TRUE);
INSERT INTO security_role(rol_id, role_name, display_name, display_state) VALUES (2,'REQUESTER','Requester',TRUE);
INSERT INTO security_role(rol_id, role_name, display_name, display_state) VALUES (3,'APPROVER','Approver',TRUE);
INSERT INTO security_role(rol_id, role_name, display_name, display_state) VALUES (4,'TRANSCRIBER','Transcriber',TRUE);
INSERT INTO security_role(rol_id, role_name, display_name, display_state) VALUES (5,'TRANSLATION_QA','Translation QA',TRUE);
INSERT INTO security_role(rol_id, role_name, display_name, display_state) VALUES (6,'RCJ_APPEALS','RCJ Appeals',TRUE);
INSERT INTO security_role(rol_id, role_name, display_name, display_state) VALUES (7,'SUPER_USER','Super User',TRUE);
INSERT INTO security_role(rol_id, role_name, display_name, display_state) VALUES (8,'SUPER_ADMIN','Super Admin',TRUE);
INSERT INTO security_role(rol_id, role_name, display_name, display_state) VALUES (9,'MEDIA_ACCESSOR','Media Accessor',TRUE);
INSERT INTO security_role(rol_id, role_name, display_name, display_state) VALUES (10,'DARTS','DARTS',FALSE);
INSERT INTO security_role(rol_id, role_name, display_name, display_state) VALUES (11,'XHIBIT','XHIBIT',FALSE);
INSERT INTO security_role(rol_id, role_name, display_name, display_state) VALUES (12,'CPP','CPP',FALSE);
INSERT INTO security_role(rol_id, role_name, display_name, display_state) VALUES (13,'DAR_PC','DAR PC',FALSE);
INSERT INTO security_role(rol_id, role_name, display_name, display_state) VALUES (14,'MID_TIER','Mid Tier',FALSE);

