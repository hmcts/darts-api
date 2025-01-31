update security_group_courthouse_ae
set grp_id = (select grp_id from security_group where group_name = 'moj_ch_wolverhampton_1_appr')
where grp_id in (select grp_id from security_group where group_name = 'moj_ch_wolverhampton_c_appr')
  and cth_id = (select cth_id from courthouse where courthouse_name = 'WOLVERHAMPTON CC AT PARK HALL');

update security_group_courthouse_ae
set grp_id = (select grp_id from security_group where group_name = 'moj_ch_wolverhampton_1_staff')
where grp_id in (select grp_id from security_group where group_name = 'moj_ch_wolverhampton_c_staff')
  and cth_id = (select cth_id from courthouse where courthouse_name = 'WOLVERHAMPTON CC AT PARK HALL');

update security_group_courthouse_ae
set grp_id = (select grp_id from security_group where group_name = 'moj_ch_wolverhampton_c_appr')
where grp_id in (select grp_id from security_group where group_name = 'moj_ch_wolverhampton_1_appr')
  and cth_id = (select cth_id from courthouse where courthouse_name = 'WOLVERHAMPTON');

update security_group_courthouse_ae
set grp_id = (select grp_id from security_group where group_name = 'moj_ch_wolverhampton_c_staff')
where grp_id in (select grp_id from security_group where group_name = 'moj_ch_wolverhampton_1_staff')
  and cth_id = (select cth_id from courthouse where courthouse_name = 'WOLVERHAMPTON');