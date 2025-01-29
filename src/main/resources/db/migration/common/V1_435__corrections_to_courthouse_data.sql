update security_group_courthouse_ae
set cth_id = (select cth_id from courthouse where courthouse_name = 'ST ALBANS MAGISTRATES COURT')
where grp_id in (select grp_id from security_group where group_name in ('moj_ch_crown_court_si1_appr', 'moj_ch_crown_court_si1_staff'))
  and cth_id = (select cth_id from courthouse where courthouse_name = 'ST ALBANS');



update security_group_courthouse_ae
set cth_id = (select cth_id from courthouse where courthouse_name = 'RCJROLLSCH3')
where grp_id in (select grp_id from security_group where group_name = 'RCJ Rolls All Buildings Approver')
  and cth_id = (select cth_id from courthouse where courthouse_name = 'PRESTON [THE SESSIONS HOUSE]');
update security_group_courthouse_ae
set cth_id = (select cth_id from courthouse where courthouse_name = 'RCJROLLSCH4')
where grp_id in (select grp_id from security_group where group_name = 'RCJ Rolls All Buildings Approver')
  and cth_id = (select cth_id from courthouse where courthouse_name = 'PROSPERO HOUSE');
update security_group_courthouse_ae
set cth_id = (select cth_id from courthouse where courthouse_name = 'RCJROLLSCH3')
where grp_id in (select grp_id from security_group where group_name = 'RCJ Rolls All Buildings Requester')
  and cth_id = (select cth_id from courthouse where courthouse_name = 'PRESTON [THE SESSIONS HOUSE]');
update security_group_courthouse_ae
set cth_id = (select cth_id from courthouse where courthouse_name = 'RCJROLLSCH4')
where grp_id in (select grp_id from security_group where group_name = 'RCJ Rolls All Buildings Requester')
  and cth_id = (select cth_id from courthouse where courthouse_name = 'PROSPERO HOUSE');
