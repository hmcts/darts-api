insert into security_group (grp_id, rol_id, global_access, display_state, display_name, dm_group_s_object_id, group_name, use_interpreter, group_display_name,
                            created_ts, created_by, last_modified_ts, last_modified_by)
VALUES (nextval('grp_seq'), 3, false, true, 'Burnley Mags Two Approver', '121707588000cd01', 'moj_ch_burnley_mags_two_appr', false, '0b1707589a44e76a_appr',
        current_timestamp, 0, current_timestamp, 0),
       (nextval('grp_seq'), 2, false, true, 'Burnley Mags Two Requester', '121707588000cd00', 'moj_ch_burnley_mags_two_staff', false, '0b1707589a44e76a_staff',
        current_timestamp, 0, current_timestamp, 0);


insert into security_group_courthouse_ae(grp_id, cth_id)
values ((select grp_id from security_group where group_name = 'moj_ch_burnley_mags_two_appr'),
        (select cth_id from courthouse where courthouse_name = 'BURNLEY MAGS TWO')),
       ((select grp_id from security_group where group_name = 'moj_ch_burnley_mags_two_staff'),
        (select cth_id from courthouse where courthouse_name = 'BURNLEY MAGS TWO')),
       ((select grp_id from security_group where group_name = 'tc_martinwalshcherer'),
        (select cth_id from courthouse where courthouse_name = 'BURNLEY MAGS TWO'));