INSERT INTO courthouse (cth_id, courthouse_name, display_name, folder_path, courthouse_object_id, created_by, created_ts, last_modified_by, last_modified_ts)
VALUES (nextVal('cth_seq'), 'HARROW CC AT HENDON MC', 'Harrow CC at Hendon MC', '/Area 12/Harrow CC at Hendon MC', '0b1707589ae8caf3', 0, current_timestamp, 0, current_timestamp);

INSERT INTO revinfo (rev, revtstmp, audit_user)
    VALUES (nextval('revinfo_seq'), EXTRACT(EPOCH FROM NOW()), 0);

INSERT INTO courthouse_aud (cth_id, courthouse_code, courthouse_name, display_name, rev, revtype)
    SELECT cth_id, courthouse_code, courthouse_name, display_name, currval('revinfo_seq'), 0 as revtype
    FROM courthouse
    WHERE cth_id = (SELECT cth_id FROM courthouse WHERE courthouse_name = 'HARROW CC AT HENDON MC');


INSERT INTO courthouse_region_ae (cth_id, reg_id)
VALUES(
    (SELECT cth_id FROM courthouse WHERE courthouse_name = 'HARROW CC AT HENDON MC'),
    (SELECT reg_id FROM region WHERE region_name = 'London'));

INSERT INTO revinfo (rev, revtstmp, audit_user)
    VALUES (nextval('revinfo_seq'), EXTRACT(EPOCH FROM NOW()), 0);

INSERT INTO courthouse_region_ae_aud
    SELECT cth_id, reg_id, currval('revinfo_seq'), 0 as revtype
    FROM courthouse_region_ae
    WHERE cth_id = (SELECT cth_id FROM courthouse WHERE courthouse_name = 'HARROW CC AT HENDON MC');


INSERT INTO security_group (grp_id, rol_id, group_name, display_name, global_access, display_state, use_interpreter, dm_group_s_object_id, group_display_name, created_by, created_ts, last_modified_by, last_modified_ts)
VALUES (nextval('grp_seq'), 2, 'moj_ch_harrow_cc_at_he_staff', 'Harrow CC at Hendon MC Requester', false, true, false, '121707588000d100', '0b1707589ae8caf3_staff', 0, current_timestamp, 0, current_timestamp);

INSERT INTO revinfo (rev, revtstmp, audit_user)
    VALUES (nextval('revinfo_seq'), EXTRACT(EPOCH FROM NOW()), 0);

INSERT INTO security_group_aud (grp_id, group_name, display_name, description, rev, revtype)
    SELECT grp_id, group_name, display_name, null, currval('revinfo_seq'), 0
    FROM security_group
    WHERE group_name = 'moj_ch_harrow_cc_at_he_staff';


INSERT INTO security_group (grp_id, rol_id, group_name, display_name, global_access, display_state, use_interpreter, dm_group_s_object_id, group_display_name, created_by, created_ts, last_modified_by, last_modified_ts)
VALUES (nextval('grp_seq'), 3, 'moj_ch_harrow_cc_at_he_appr', 'Harrow CC at Hendon MC Approver', false, true, false, '121707588000d101', '0b1707589ae8caf3_appr', 0, current_timestamp, 0, current_timestamp);

INSERT INTO revinfo (rev, revtstmp, audit_user)
    VALUES (nextval('revinfo_seq'), EXTRACT(EPOCH FROM NOW()), 0);

INSERT INTO security_group_aud (grp_id, group_name, display_name, description, rev, revtype)
    SELECT grp_id, group_name, display_name, null, currval('revinfo_seq'), 0
    FROM security_group
    WHERE group_name = 'moj_ch_harrow_cc_at_he_appr';


INSERT INTO security_group_courthouse_ae (grp_id, cth_id)
VALUES (
    (SELECT grp_id FROM security_group WHERE group_name = 'moj_ch_harrow_cc_at_he_staff'),
    (SELECT cth_id FROM courthouse WHERE courthouse_name = 'HARROW CC AT HENDON MC'));

INSERT INTO revinfo (rev, revtstmp, audit_user)
    VALUES (nextval('revinfo_seq'), EXTRACT(EPOCH FROM NOW()), 0);

INSERT INTO security_group_courthouse_ae_aud
    SELECT grp_id, cth_id, currval('revinfo_seq'), 0 as revtype
    FROM security_group_courthouse_ae
    WHERE grp_id = (SELECT grp_id FROM security_group WHERE group_name = 'moj_ch_harrow_cc_at_he_staff');


INSERT INTO security_group_courthouse_ae (grp_id, cth_id)
VALUES (
    (SELECT grp_id FROM security_group WHERE group_name = 'moj_ch_harrow_cc_at_he_appr'),
    (SELECT cth_id FROM courthouse WHERE courthouse_name = 'HARROW CC AT HENDON MC'));

INSERT INTO revinfo (rev, revtstmp, audit_user)
    VALUES (nextval('revinfo_seq'), EXTRACT(EPOCH FROM NOW()), 0);

INSERT INTO security_group_courthouse_ae_aud
    SELECT grp_id, cth_id, currval('revinfo_seq'), 0 as revtype
    FROM security_group_courthouse_ae
    WHERE grp_id = (SELECT grp_id FROM security_group WHERE group_name = 'moj_ch_harrow_cc_at_he_appr');


INSERT INTO security_group_courthouse_ae (grp_id, cth_id)
VALUES (
    (SELECT grp_id FROM security_group WHERE group_name = 'rasso_users'),
    (SELECT cth_id FROM courthouse WHERE courthouse_name = 'HARROW CC AT HENDON MC'));

INSERT INTO revinfo (rev, revtstmp, audit_user)
    VALUES (nextval('revinfo_seq'), EXTRACT(EPOCH FROM NOW()), 0);

INSERT INTO security_group_courthouse_ae_aud
    SELECT grp_id, cth_id, currval('revinfo_seq'), 0 as revtype
    FROM security_group_courthouse_ae
    WHERE grp_id = (SELECT grp_id FROM security_group WHERE group_name = 'rasso_users');


INSERT INTO security_group_courthouse_ae (grp_id, cth_id)
VALUES (
    (SELECT grp_id FROM security_group WHERE group_name = 'tc_ubiqus'),
    (SELECT cth_id FROM courthouse WHERE courthouse_name = 'HARROW CC AT HENDON MC'));

INSERT INTO revinfo (rev, revtstmp, audit_user)
    VALUES (nextval('revinfo_seq'), EXTRACT(EPOCH FROM NOW()), 0);

INSERT INTO security_group_courthouse_ae_aud
    SELECT grp_id, cth_id, currval('revinfo_seq'), 0 as revtype
    FROM security_group_courthouse_ae
    WHERE grp_id = (SELECT grp_id FROM security_group WHERE group_name = 'tc_ubiqus');