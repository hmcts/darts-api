CREATE OR REPLACE VIEW user_roles_courthouses AS
SELECT DISTINCT cth.cth_id,
                usr.usr_id,
                grp.rol_id
FROM       darts.user_account usr
INNER JOIN darts.security_group_user_account_ae gua on usr.usr_id = gua.usr_id
INNER JOIN darts.security_group grp on grp.grp_id = gua.grp_id
INNER JOIN darts.security_group_courthouse_ae grc on grc.grp_id = grp.grp_id
INNER JOIN darts.courthouse cth on grc.cth_id = cth.cth_id
WHERE grp.global_access = FALSE
UNION
SELECT DISTINCT cth.cth_id,
                usr.usr_id,
                grp.rol_id
FROM       darts.user_account usr
INNER JOIN darts.security_group_user_account_ae gua on usr.usr_id = gua.usr_id
INNER JOIN darts.security_group grp on grp.grp_id = gua.grp_id
CROSS JOIN darts.courthouse cth
WHERE grp.global_access=TRUE
ORDER BY cth_id,
         usr_id,
         rol_id;
