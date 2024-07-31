DO $$
DECLARE
	recordToBeMigrated record;
	recordToBeDeleted record;
	newId integer;
	oldId integer;
BEGIN
--delete users not used
FOR recordToBeMigrated IN
    SELECT usr_id from user_account where usr_id in (-999, -48, -47, -46, -43, -42, -41, -40, -5, -4, -3, -2, -1)
    order by 1
LOOP
    BEGIN
        delete from security_group_user_account_ae where usr_id=recordToBeMigrated.usr_id;
        delete from user_account where usr_id=recordToBeMigrated.usr_id;
        RAISE NOTICE 'user account % deleted', recordToBeMigrated.usr_id;
    EXCEPTION WHEN OTHERS THEN
        RAISE NOTICE 'unable to delete % because it is in use', recordToBeMigrated.usr_id;
    END;
END LOOP;


FOR recordToBeMigrated IN
    SELECT usr_id from user_account
    order by 1
	LOOP
	select recordToBeMigrated.usr_id+1000000 into newId;
	select recordToBeMigrated.usr_id into oldId;
	RAISE NOTICE 'migrating % to %', oldId, newId;

	--insert copy
	INSERT INTO user_account(
    	usr_id, dm_user_s_object_id, user_name, user_email_address, description, created_ts, last_modified_ts, last_login_ts, last_modified_by, created_by, account_guid, is_system_user, is_active, user_full_name)
    	select newid, dm_user_s_object_id, user_name, user_email_address, description, created_ts, last_modified_ts, last_login_ts, last_modified_by, created_by, account_guid, is_system_user, is_active, user_full_name from user_account where usr_id = oldId;


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
