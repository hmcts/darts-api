--v1    taken from Prod database 22/07/2025

CREATE VIEW hearing_reporting_restrictions AS
SELECT h.cas_id,
       he.hea_id,
       eh.event_name,
       eh.event_type,
       eh.event_sub_type,
       eh.active,
       e.eve_id,
       e.ctr_id,
       e.evh_id,
       e.event_object_id,
       e.event_id,
       e.event_text,
       e.event_ts,
       e.version_label,
       e.message_id,
       e.created_ts,
       e.created_by,
       e.last_modified_ts,
       e.last_modified_by,
       e.is_log_entry,
       e.chronicle_id,
       e.antecedent_id
FROM darts.event_handler eh
JOIN darts.event e ON e.evh_id = eh.evh_id
JOIN darts.hearing_event_ae he ON he.eve_id = e.eve_id
JOIN darts.hearing h ON h.hea_id = he.hea_id
WHERE eh.is_reporting_restriction = true;

CREATE VIEW user_roles_courthouses AS
SELECT DISTINCT cth.cth_id,
                usr.usr_id,
                grp.rol_id
FROM user_account usr
JOIN security_group_user_account_ae gua ON usr.usr_id = gua.usr_id
JOIN security_group grp ON grp.grp_id = gua.grp_id
JOIN security_group_courthouse_ae grc ON grc.grp_id = grp.grp_id
JOIN courthouse cth ON grc.cth_id = cth.cth_id
WHERE grp.global_access = false
UNION
SELECT DISTINCT cth.cth_id,
                usr.usr_id,
                grp.rol_id
FROM user_account usr
JOIN security_group_user_account_ae gua ON usr.usr_id = gua.usr_id
JOIN security_group grp ON grp.grp_id = gua.grp_id
CROSS JOIN courthouse cth
WHERE grp.global_access = true
ORDER BY 1, 2, 3;