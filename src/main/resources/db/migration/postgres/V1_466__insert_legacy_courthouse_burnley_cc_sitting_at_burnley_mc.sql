INSERT INTO courthouse (cth_id, courthouse_name, display_name, folder_path, courthouse_object_id, created_by, created_ts, last_modified_by, last_modified_ts)
VALUES (nextVal('cth_seq'), 'BURNLEY CC SITTING AT BURNLEY MC', 'Burnley CC Sitting at Burnley MC', '/Area 2/Burnley CC sitting at Burnley MC', '0b1707589afa4018', 0, current_timestamp, 0, current_timestamp);

INSERT INTO revinfo (rev, revtstmp, audit_user)
    VALUES (nextval('revinfo_seq'), EXTRACT(EPOCH FROM NOW()), 0);

INSERT INTO courthouse_aud (cth_id, courthouse_code, courthouse_name, display_name, rev, revtype)
    SELECT cth_id, courthouse_code, courthouse_name, display_name, currval('revinfo_seq'), 0 as revtype
    FROM courthouse
    WHERE cth_id = (SELECT cth_id FROM courthouse WHERE courthouse_name = 'BURNLEY CC SITTING AT BURNLEY MC');


INSERT INTO courthouse_region_ae (cth_id, reg_id)
VALUES(
    (SELECT cth_id FROM courthouse WHERE courthouse_name = 'BURNLEY CC SITTING AT BURNLEY MC'),
    (SELECT reg_id FROM region WHERE region_name = 'North West'));

INSERT INTO revinfo (rev, revtstmp, audit_user)
    VALUES (nextval('revinfo_seq'), EXTRACT(EPOCH FROM NOW()), 0);

INSERT INTO courthouse_region_ae_aud
    SELECT cth_id, reg_id, currval('revinfo_seq'), 0 as revtype
    FROM courthouse_region_ae
    WHERE cth_id = (SELECT cth_id FROM courthouse WHERE courthouse_name = 'BURNLEY CC SITTING AT BURNLEY MC');


INSERT INTO security_group (grp_id, rol_id, group_name, display_name, global_access, display_state, use_interpreter, dm_group_s_object_id, group_display_name, created_by, created_ts, last_modified_by, last_modified_ts)
VALUES (nextval('grp_seq'), 2, 'moj_ch_burnley_cc_sitt_staff', 'Burnley CC sitting at Burnley MC Requestor', false, true, false, '121707588000d500', '0b1707589afa4018_staff', 0, current_timestamp, 0, current_timestamp);

INSERT INTO revinfo (rev, revtstmp, audit_user)
    VALUES (nextval('revinfo_seq'), EXTRACT(EPOCH FROM NOW()), 0);

INSERT INTO security_group_aud (grp_id, group_name, display_name, description, rev, revtype)
    SELECT grp_id, group_name, display_name, null, currval('revinfo_seq'), 0
    FROM security_group
    WHERE group_name = 'moj_ch_burnley_cc_sitt_staff';


INSERT INTO security_group (grp_id, rol_id, group_name, display_name, global_access, display_state, use_interpreter, dm_group_s_object_id, group_display_name, created_by, created_ts, last_modified_by, last_modified_ts)
VALUES (nextval('grp_seq'), 3, 'moj_ch_burnley_cc_sitt_appr', 'Burnley CC sitting at Burnley MC Approver', false, true, false, '121707588000d501', '0b1707589afa4018_appr', 0, current_timestamp, 0, current_timestamp);

INSERT INTO revinfo (rev, revtstmp, audit_user)
    VALUES (nextval('revinfo_seq'), EXTRACT(EPOCH FROM NOW()), 0);

INSERT INTO security_group_aud (grp_id, group_name, display_name, description, rev, revtype)
    SELECT grp_id, group_name, display_name, null, currval('revinfo_seq'), 0
    FROM security_group
    WHERE group_name = 'moj_ch_burnley_cc_sitt_appr';


INSERT INTO security_group_courthouse_ae (grp_id, cth_id)
VALUES (
    (SELECT grp_id FROM security_group WHERE group_name = 'moj_ch_burnley_cc_sitt_staff'),
    (SELECT cth_id FROM courthouse WHERE courthouse_name = 'BURNLEY CC SITTING AT BURNLEY MC'));

INSERT INTO revinfo (rev, revtstmp, audit_user)
    VALUES (nextval('revinfo_seq'), EXTRACT(EPOCH FROM NOW()), 0);

INSERT INTO security_group_courthouse_ae_aud
    SELECT grp_id, cth_id, currval('revinfo_seq'), 0 as revtype
    FROM security_group_courthouse_ae
    WHERE grp_id = (SELECT grp_id FROM security_group WHERE group_name = 'moj_ch_burnley_cc_sitt_staff');


INSERT INTO security_group_courthouse_ae (grp_id, cth_id)
VALUES (
    (SELECT grp_id FROM security_group WHERE group_name = 'moj_ch_burnley_cc_sitt_appr'),
    (SELECT cth_id FROM courthouse WHERE courthouse_name = 'BURNLEY CC SITTING AT BURNLEY MC'));

INSERT INTO revinfo (rev, revtstmp, audit_user)
    VALUES (nextval('revinfo_seq'), EXTRACT(EPOCH FROM NOW()), 0);

INSERT INTO security_group_courthouse_ae_aud
    SELECT grp_id, cth_id, currval('revinfo_seq'), 0 as revtype
    FROM security_group_courthouse_ae
    WHERE grp_id = (SELECT grp_id FROM security_group WHERE group_name = 'moj_ch_burnley_cc_sitt_appr');


INSERT INTO security_group_courthouse_ae (grp_id, cth_id)
VALUES (
    (SELECT grp_id FROM security_group WHERE group_name = 'rasso_users'),
    (SELECT cth_id FROM courthouse WHERE courthouse_name = 'BURNLEY CC SITTING AT BURNLEY MC'));

INSERT INTO revinfo (rev, revtstmp, audit_user)
    VALUES (nextval('revinfo_seq'), EXTRACT(EPOCH FROM NOW()), 0);

INSERT INTO security_group_courthouse_ae_aud
    SELECT grp_id, cth_id, currval('revinfo_seq'), 0 as revtype
    FROM security_group_courthouse_ae
    WHERE grp_id = (SELECT grp_id FROM security_group WHERE group_name = 'rasso_users');


INSERT INTO security_group_courthouse_ae (grp_id, cth_id)
VALUES (
    (SELECT grp_id FROM security_group WHERE group_name = 'tc_martinwalshcherer'),
    (SELECT cth_id FROM courthouse WHERE courthouse_name = 'BURNLEY CC SITTING AT BURNLEY MC'));

INSERT INTO revinfo (rev, revtstmp, audit_user)
    VALUES (nextval('revinfo_seq'), EXTRACT(EPOCH FROM NOW()), 0);

INSERT INTO security_group_courthouse_ae_aud
    SELECT grp_id, cth_id, currval('revinfo_seq'), 0 as revtype
    FROM security_group_courthouse_ae
    WHERE grp_id = (SELECT grp_id FROM security_group WHERE group_name = 'tc_martinwalshcherer');