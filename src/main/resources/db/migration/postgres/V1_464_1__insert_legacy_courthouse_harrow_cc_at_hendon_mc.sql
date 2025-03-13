-- rasso_users and tc_ubiqus are only available on postgres
INSERT INTO security_group_courthouse_ae (grp_id, cth_id)
VALUES (
    (SELECT grp_id FROM darts.security_group WHERE group_name = 'rasso_users'),
    (SELECT cth_id FROM courthouse WHERE courthouse_name = 'HARROW CC AT HENDON MC'));

INSERT INTO revinfo (rev, revtstmp, audit_user)
    VALUES (nextval('revinfo_seq'), EXTRACT(EPOCH FROM NOW()), 0);

INSERT INTO security_group_courthouse_ae_aud
    SELECT grp_id, cth_id, currval('revinfo_seq'), 0 as revtype
    FROM security_group_courthouse_ae
    WHERE grp_id = (SELECT grp_id FROM darts.security_group WHERE group_name = 'rasso_users');


INSERT INTO security_group_courthouse_ae (grp_id, cth_id)
VALUES (
    (SELECT grp_id FROM darts.security_group WHERE group_name = 'tc_ubiqus'),
    (SELECT cth_id FROM courthouse WHERE courthouse_name = 'HARROW CC AT HENDON MC'));

INSERT INTO revinfo (rev, revtstmp, audit_user)
    VALUES (nextval('revinfo_seq'), EXTRACT(EPOCH FROM NOW()), 0);

INSERT INTO security_group_courthouse_ae_aud
    SELECT grp_id, cth_id, currval('revinfo_seq'), 0 as revtype
    FROM security_group_courthouse_ae
    WHERE grp_id = (SELECT grp_id FROM darts.security_group WHERE group_name = 'tc_ubiqus');