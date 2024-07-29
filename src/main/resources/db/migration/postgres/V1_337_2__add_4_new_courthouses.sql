DO $$
DECLARE
	cHouseId integer;
	securityGroupId integer;
BEGIN
    --WOLVERHAMPTON SITTING AT TELFORD
    SELECT 152 into cHouseId;
    INSERT INTO courthouse (cth_id,courthouse_code,courthouse_name,display_name,created_ts,created_by,last_modified_ts,last_modified_by) VALUES
	 (cHouseId,NULL,'WOLVERHAMPTON SITTING AT TELFORD','Wolverhampton sitting at Telford','2024-01-01',0,'2024-01-01',0);

    INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (cHouseId,4);

    SELECT 352 into securityGroupId;
    INSERT INTO security_group(
    grp_id, rol_id, group_name, is_private, description, group_global_unique_id, global_access, display_state, use_interpreter, display_name, dm_group_s_object_id, created_ts, created_by, last_modified_ts, last_modified_by)
    VALUES (securityGroupId, 3, 'moj_ch_wolverhampton_telford_appr', null, null, null, FALSE, TRUE, FALSE, 'Wolverhampton sitting at Telford Approver',null, '2024-01-01', 0, '2024-01-01', 0);
    INSERT INTO darts.security_group_courthouse_ae (grp_id, cth_id) VALUES (securityGroupId,cHouseId);

    SELECT 353 into securityGroupId;
    INSERT INTO security_group(
    grp_id, rol_id, group_name, is_private, description, group_global_unique_id, global_access, display_state, use_interpreter, display_name, dm_group_s_object_id, created_ts, created_by, last_modified_ts, last_modified_by)
    VALUES (securityGroupId, 2, 'moj_ch_wolverhampton_telford_staff', null, null, null, FALSE, TRUE, FALSE, 'Wolverhampton sitting at Telford Requester',null, '2024-01-01', 0, '2024-01-01', 0);
    INSERT INTO darts.security_group_courthouse_ae (grp_id, cth_id) VALUES (securityGroupId,cHouseId);

    INSERT INTO darts.security_group_courthouse_ae (grp_id, cth_id) VALUES (15,cHouseId);--transcription Company



    --WORCESTER SITTING AT REDDITCH JC
    SELECT 153 into cHouseId;
    INSERT INTO courthouse (cth_id,courthouse_code,courthouse_name,display_name,created_ts,created_by,last_modified_ts,last_modified_by) VALUES
	 (cHouseId,NULL,'WORCESTER SITTING AT REDDITCH JC','Worcester Sitting At Redditch JC','2024-01-01',0,'2024-01-01',0);

    INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (cHouseId,4);

    SELECT 354 into securityGroupId;
    INSERT INTO security_group(
    grp_id, rol_id, group_name, is_private, description, group_global_unique_id, global_access, display_state, use_interpreter, display_name, dm_group_s_object_id, created_ts, created_by, last_modified_ts, last_modified_by)
    VALUES (securityGroupId, 3, 'moj_ch_worcester_redditch_appr', null, null, null, FALSE, TRUE, FALSE, 'Worcester Sitting At Redditch JC Approver',null, '2024-01-01', 0, '2024-01-01', 0);
    INSERT INTO darts.security_group_courthouse_ae (grp_id, cth_id) VALUES (securityGroupId,cHouseId);

    SELECT 355 into securityGroupId;
    INSERT INTO security_group(
    grp_id, rol_id, group_name, is_private, description, group_global_unique_id, global_access, display_state, use_interpreter, display_name, dm_group_s_object_id, created_ts, created_by, last_modified_ts, last_modified_by)
    VALUES (securityGroupId, 2, 'moj_ch_worcester_redditch_staff', null, null, null, FALSE, TRUE, FALSE, 'Worcester Sitting At Redditch JC Requester',null, '2024-01-01', 0, '2024-01-01', 0);
    INSERT INTO darts.security_group_courthouse_ae (grp_id, cth_id) VALUES (securityGroupId,cHouseId);

    INSERT INTO darts.security_group_courthouse_ae (grp_id, cth_id) VALUES (15,cHouseId);--transcription Company



    --CHESTERFIELD JUSTICE CENTRE
    SELECT 154 into cHouseId;
    INSERT INTO courthouse (cth_id,courthouse_code,courthouse_name,display_name,created_ts,created_by,last_modified_ts,last_modified_by) VALUES
	 (cHouseId,NULL,'CHESTERFIELD JUSTICE CENTRE','Chesterfield Justice Centre','2024-01-01',0,'2024-01-01',0);

    INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (cHouseId,4);

    SELECT 356 into securityGroupId;
    INSERT INTO security_group(
    grp_id, rol_id, group_name, is_private, description, group_global_unique_id, global_access, display_state, use_interpreter, display_name, dm_group_s_object_id, created_ts, created_by, last_modified_ts, last_modified_by)
    VALUES (securityGroupId, 3, 'moj_ch_chesterfield_appr', null, null, null, FALSE, TRUE, FALSE, 'Chesterfield Justice Centre Approver',null, '2024-01-01', 0, '2024-01-01', 0);
    INSERT INTO darts.security_group_courthouse_ae (grp_id, cth_id) VALUES (securityGroupId,cHouseId);

    SELECT 357 into securityGroupId;
    INSERT INTO security_group(
    grp_id, rol_id, group_name, is_private, description, group_global_unique_id, global_access, display_state, use_interpreter, display_name, dm_group_s_object_id, created_ts, created_by, last_modified_ts, last_modified_by)
    VALUES (securityGroupId, 2, 'moj_ch_chesterfield_staff', null, null, null, FALSE, TRUE, FALSE, 'Chesterfield Justice Centre Requester',null, '2024-01-01', 0, '2024-01-01', 0);
    INSERT INTO darts.security_group_courthouse_ae (grp_id, cth_id) VALUES (securityGroupId,cHouseId);

    INSERT INTO darts.security_group_courthouse_ae (grp_id, cth_id) VALUES (15,cHouseId);--transcription Company



    --MANSFIELD JUSTICE CENTRE
    SELECT 155 into cHouseId;
    INSERT INTO courthouse (cth_id,courthouse_code,courthouse_name,display_name,created_ts,created_by,last_modified_ts,last_modified_by) VALUES
	 (cHouseId,NULL,'MANSFIELD JUSTICE CENTRE','Mansfield Justice Centre','2024-01-01',0,'2024-01-01',0);

    INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (cHouseId,4);

    SELECT 358 into securityGroupId;
    INSERT INTO security_group(
    grp_id, rol_id, group_name, is_private, description, group_global_unique_id, global_access, display_state, use_interpreter, display_name, dm_group_s_object_id, created_ts, created_by, last_modified_ts, last_modified_by)
    VALUES (securityGroupId, 3, 'moj_ch_mansfield_appr', null, null, null, FALSE, TRUE, FALSE, 'Mansfield Justice Centre Approver',null, '2024-01-01', 0, '2024-01-01', 0);
    INSERT INTO darts.security_group_courthouse_ae (grp_id, cth_id) VALUES (securityGroupId,cHouseId);

    SELECT 359 into securityGroupId;
    INSERT INTO security_group(
    grp_id, rol_id, group_name, is_private, description, group_global_unique_id, global_access, display_state, use_interpreter, display_name, dm_group_s_object_id, created_ts, created_by, last_modified_ts, last_modified_by)
    VALUES (securityGroupId, 2, 'moj_ch_mansfield_staff', null, null, null, FALSE, TRUE, FALSE, 'Mansfield Justice Centre Requester',null, '2024-01-01', 0, '2024-01-01', 0);
    INSERT INTO darts.security_group_courthouse_ae (grp_id, cth_id) VALUES (securityGroupId,cHouseId);

    INSERT INTO darts.security_group_courthouse_ae (grp_id, cth_id) VALUES (15,cHouseId);--transcription Company


END $$;