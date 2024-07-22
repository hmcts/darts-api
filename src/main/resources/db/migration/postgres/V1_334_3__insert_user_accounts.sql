DO $$
DECLARE
	recordToBeMigrated record;
	newId integer;
	oldId integer;
	--Prod data will have an id below this value, a deliberate gap will be left between prod data and test data.
	sequenceRestartValue integer = 100;
BEGIN
perform setval('usr_seq', sequenceRestartValue, false);
RAISE NOTICE 'migrating duplicate records';
FOR recordToBeMigrated IN
    SELECT table1.usr_id newId, table2.user_name name, table2.account_guid, table2.usr_id oldId FROM user_account table1, user_account table2
	where (
	upper(table1.user_name) = upper(table2.user_name) OR
	(table1.user_name = 'viq' AND table2.user_name = 'Mid Tier') OR
	(table1.user_name = 'viq' AND table2.user_name = 'Dar Pc') OR
	(table1.user_name = 'cp' AND table2.user_name = 'cpp_user') OR
	(table1.user_name = 'xhibit' AND table2.user_name = 'xhibit_user')
	)
    and table1.usr_id < table2.usr_id
     and table1.usr_id < sequenceRestartValue
     and table2.usr_id >= sequenceRestartValue
     order by 1
	LOOP
		select recordToBeMigrated.oldId into oldId;
    	select recordToBeMigrated.newID into newID;
	RAISE NOTICE 'migrating % from % to %', recordToBeMigrated.name, oldId, newId;

	update user_account set account_guid = recordToBeMigrated.account_guid where usr_id = newID and recordToBeMigrated.account_guid is not null;

	--move entries
    update annotation set current_owner = newId where current_owner = oldId;
    update annotation set deleted_by = newId where deleted_by = oldId;
    update audit set usr_id = newId where usr_id = oldId;
    update case_document set created_by = newId where created_by = oldId;
    update case_document set last_modified_by = newId where last_modified_by = oldId;
    update case_retention set created_by = newId where created_by = oldId;
    update case_retention set last_modified_by = newId where last_modified_by = oldId;
    update case_retention set submitted_by = newId where submitted_by = oldId;
    update court_case set deleted_by = newId where deleted_by = oldId;
    update external_object_directory set created_by = newId where created_by = oldId;
    update external_object_directory set last_modified_by = newId where last_modified_by = oldId;
    update media set deleted_by = newId where deleted_by = oldId;
    update media_request set created_by = newId where created_by = oldId;
    update media_request set current_owner = newId where current_owner = oldId;
    update media_request set last_modified_by = newId where last_modified_by = oldId;
    update media_request set requestor = newId where requestor = oldId;
    update node_register set created_by = newId where created_by = oldId;
    update object_admin_action set hidden_by = newId where hidden_by = oldId;
    update object_admin_action set marked_for_manual_del_by = newId where marked_for_manual_del_by = oldId;
    update retention_policy_type set created_by = newId where created_by = oldId;
    update retention_policy_type set last_modified_by = newId where last_modified_by = oldId;
    BEGIN update security_group_user_account_ae set usr_id = newId where usr_id = oldId; EXCEPTION WHEN OTHERS THEN delete from security_group_user_account_ae where usr_id = oldId; END;
    update transcription set created_by = newId where created_by = oldId;
    update transcription set deleted_by = newId where deleted_by = oldId;
    update transcription set last_modified_by = newId where last_modified_by = oldId;
    update transcription set requested_by = newId where requested_by = oldId;
    update transcription_comment set author = newId where author = oldId;
    update transcription_comment set created_by = newId where created_by = oldId;
    update transcription_comment set last_modified_by = newId where last_modified_by = oldId;
    update transcription_workflow set workflow_actor = newId where workflow_actor = oldId;
    update transformed_media set created_by = newId where created_by = oldId;
    update transformed_media set last_modified_by = newId where last_modified_by = oldId;
    update transient_object_directory set created_by = newId where created_by = oldId;
    update transient_object_directory set last_modified_by = newId where last_modified_by = oldId;

	--delete old record
	delete from user_account where usr_id = oldId;

    END LOOP;
RAISE NOTICE 'migrating other records';
FOR recordToBeMigrated IN
    SELECT usr_id, user_name name FROM user_account
	where usr_id >= sequenceRestartValue
     order by 1
	LOOP
	select nextval('usr_seq') into newId;
	select recordToBeMigrated.usr_id into oldId;
	RAISE NOTICE 'migrating % from % to %', recordToBeMigrated.name, oldId, newId;

	--insert copy
	INSERT INTO user_account(
    	usr_id, dm_user_s_object_id, user_name, user_email_address, description, created_ts, last_modified_ts, last_login_ts, last_modified_by, created_by, account_guid, is_system_user, is_active, user_full_name)
    	select newid, dm_user_s_object_id, user_name, user_email_address, description, created_ts, last_modified_ts, last_login_ts, last_modified_by, created_by, account_guid, is_system_user, is_active, user_full_name from user_account where usr_id = oldId;


	--move foreign key references
    update annotation set current_owner = newId where current_owner = oldId;
    update annotation set deleted_by = newId where deleted_by = oldId;
    update audit set usr_id = newId where usr_id = oldId;
    update case_document set created_by = newId where created_by = oldId;
    update case_document set last_modified_by = newId where last_modified_by = oldId;
    update case_retention set created_by = newId where created_by = oldId;
    update case_retention set last_modified_by = newId where last_modified_by = oldId;
    update case_retention set submitted_by = newId where submitted_by = oldId;
    update court_case set deleted_by = newId where deleted_by = oldId;
    update external_object_directory set created_by = newId where created_by = oldId;
    update external_object_directory set last_modified_by = newId where last_modified_by = oldId;
    update media set deleted_by = newId where deleted_by = oldId;
    update media_request set created_by = newId where created_by = oldId;
    update media_request set current_owner = newId where current_owner = oldId;
    update media_request set last_modified_by = newId where last_modified_by = oldId;
    update media_request set requestor = newId where requestor = oldId;
    update node_register set created_by = newId where created_by = oldId;
    update object_admin_action set hidden_by = newId where hidden_by = oldId;
    update object_admin_action set marked_for_manual_del_by = newId where marked_for_manual_del_by = oldId;
    update retention_policy_type set created_by = newId where created_by = oldId;
    update retention_policy_type set last_modified_by = newId where last_modified_by = oldId;
    update security_group_user_account_ae set usr_id = newId where usr_id = oldId;
    update transcription set created_by = newId where created_by = oldId;
    update transcription set deleted_by = newId where deleted_by = oldId;
    update transcription set last_modified_by = newId where last_modified_by = oldId;
    update transcription set requested_by = newId where requested_by = oldId;
    update transcription_comment set author = newId where author = oldId;
    update transcription_comment set created_by = newId where created_by = oldId;
    update transcription_comment set last_modified_by = newId where last_modified_by = oldId;
    update transcription_workflow set workflow_actor = newId where workflow_actor = oldId;
    update transformed_media set created_by = newId where created_by = oldId;
    update transformed_media set last_modified_by = newId where last_modified_by = oldId;
    update transient_object_directory set created_by = newId where created_by = oldId;
    update transient_object_directory set last_modified_by = newId where last_modified_by = oldId;

	--delete old record
	delete from user_account where usr_id = oldId;

    END LOOP;

END $$;
