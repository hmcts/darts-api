delete from security_role_permission_ae;
INSERT INTO security_role(rol_id, role_name, display_name, display_state) VALUES (-999,'temp','temp',TRUE);
update security_group set rol_id=-999;

delete from security_role where rol_id>-999;
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



update security_group set rol_id=14 where grp_id=-17;
update security_group set rol_id=13 where grp_id=-16;
update security_group set rol_id=12 where grp_id=-15;
update security_group set rol_id=11 where grp_id=-14;
update security_group set rol_id=6 where grp_id=-6;
update security_group set rol_id=5 where grp_id=-5;
update security_group set rol_id=4 where grp_id=-4;
update security_group set rol_id=1 where grp_id=-3;
update security_group set rol_id=2 where grp_id=-2;
update security_group set rol_id=3 where grp_id=-1;
update security_group set rol_id=8 where grp_id=1;
update security_group set rol_id=7 where grp_id=2;
update security_group set rol_id=10 where grp_id=3;
update security_group set rol_id=9 where grp_id=4;


delete from security_role where rol_id=-999;