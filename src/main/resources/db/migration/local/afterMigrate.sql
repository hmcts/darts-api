INSERT INTO darts.security_group_user_account_ae (usr_id, grp_id)
SELECT -999, sg.grp_id
FROM darts.security_group sg
where sg.grp_id not in (select sgua.grp_id from darts.security_group_user_account_ae sgua where sgua.usr_id = -999)
ORDER BY 2;

INSERT INTO darts.security_group_user_account_ae (usr_id, grp_id)
SELECT -4, sg.grp_id
FROM darts.security_group sg
where sg.grp_id not in (select sgua.grp_id from darts.security_group_user_account_ae sgua where sgua.usr_id = -4)
ORDER BY 2;

INSERT INTO darts.security_group_user_account_ae (usr_id, grp_id)
SELECT -3, sg.grp_id
FROM darts.security_group sg
where sg.grp_id not in (select sgua.grp_id from darts.security_group_user_account_ae sgua where sgua.usr_id = -3)
ORDER BY 2;

INSERT INTO darts.security_group_user_account_ae (usr_id, grp_id)
SELECT -2, sg.grp_id
FROM darts.security_group sg
where sg.grp_id not in (select sgua.grp_id from darts.security_group_user_account_ae sgua where sgua.usr_id = -2)
ORDER BY 2;

INSERT INTO darts.security_group_user_account_ae (usr_id, grp_id)
SELECT -1, sg.grp_id
FROM darts.security_group sg
where sg.grp_id not in (select sgua.grp_id from darts.security_group_user_account_ae sgua where sgua.usr_id = -1)
ORDER BY 2;

