CREATE OR REPLACE VIEW user_account_court_case AS
select distinct cth_id, usr_id, rol_id, cas_id from (SELECT ccase.cth_id,
    usr.usr_id,
    grp.rol_id,
	ccase.cas_id
   FROM user_account usr
     JOIN security_group_user_account_ae gua ON usr.usr_id = gua.usr_id
     JOIN security_group grp ON grp.grp_id = gua.grp_id
     JOIN security_group_courthouse_ae grc ON grc.grp_id = grp.grp_id
     JOIN courthouse cth ON grc.cth_id = cth.cth_id
	 JOIN court_case ccase ON cth.cth_id = ccase.cth_id
  WHERE grp.global_access = false
  and grp.use_interpreter = false
  UNION
   SELECT ccase.cth_id,
    usr.usr_id,
    grp.rol_id,
	ccase.cas_id
   FROM user_account usr
     JOIN security_group_user_account_ae gua ON usr.usr_id = gua.usr_id
     JOIN security_group grp ON grp.grp_id = gua.grp_id
     JOIN security_group_courthouse_ae grc ON grc.grp_id = grp.grp_id
     JOIN courthouse cth ON grc.cth_id = cth.cth_id
	 JOIN court_case ccase ON cth.cth_id = ccase.cth_id
  WHERE grp.global_access = false
  and grp.use_interpreter = true
  and ccase.interpreter_used = true
  and grp.rol_id in (SELECT DISTINCT rol_id FROM security_role WHERE role_name = 'TRANSLATION_QA')
UNION
 SELECT ccase.cth_id,
    usr.usr_id,
    grp.rol_id,
	ccase.cas_id
   FROM user_account usr
     JOIN security_group_user_account_ae gua ON usr.usr_id = gua.usr_id
     JOIN security_group grp ON grp.grp_id = gua.grp_id
     CROSS JOIN court_case ccase
  WHERE grp.global_access = true							  
  and grp.use_interpreter = false
UNION
 SELECT ccase.cth_id,
    usr.usr_id,
    grp.rol_id,
	ccase.cas_id
   FROM user_account usr
     JOIN security_group_user_account_ae gua ON usr.usr_id = gua.usr_id
     JOIN security_group grp ON grp.grp_id = gua.grp_id
     CROSS JOIN court_case ccase
  WHERE grp.global_access = true							  
  and grp.use_interpreter = true
  and ccase.interpreter_used = true
  and grp.rol_id in (SELECT DISTINCT rol_id FROM security_role WHERE role_name = 'TRANSLATION_QA')
  ) sub_query