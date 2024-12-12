-- add courthouse_object_id and folder_path to courthouse
ALTER TABLE courthouse
    ADD COLUMN courthouse_object_id CHARACTER VARYING(16);
ALTER TABLE courthouse
    ADD COLUMN folder_path CHARACTER VARYING;

-- add subcontent_object_id and subcontent_position to annotation_document
ALTER TABLE annotation_document
    ADD COLUMN subcontent_object_id CHARACTER VARYING(16);
ALTER TABLE annotation_document
    ADD COLUMN subcontent_position INTEGER;

-- add subcontent_object_id and subcontent_position to daily_list
ALTER TABLE daily_list
    ALTER COLUMN content_object_id TYPE CHARACTER VARYING(16);
ALTER TABLE daily_list
    ADD COLUMN subcontent_object_id CHARACTER VARYING(16);
ALTER TABLE daily_list
    ADD COLUMN subcontent_position INTEGER;

-- add subcontent_object_id and subcontent_position to media
ALTER TABLE media
    ADD COLUMN subcontent_object_id CHARACTER VARYING(16);
ALTER TABLE media
    ADD COLUMN subcontent_position INTEGER;

-- add subcontent_object_id and subcontent_position to transcription_document
ALTER TABLE transcription_document
    ADD COLUMN subcontent_object_id CHARACTER VARYING(16);
ALTER TABLE transcription_document
    ADD COLUMN subcontent_position INTEGER;

-- remove user_name from user_account
UPDATE user_account
SET user_full_name = user_name
WHERE user_full_name IS NULL
  AND user_name IS NOT NULL;
ALTER TABLE user_account
    DROP COLUMN user_name;

-- amend user_account.user_full_name to not null
ALTER TABLE user_account
    ALTER COLUMN user_full_name SET NOT NULL;

-- reintroduce user_login_name,user_os_name,user_login_domain,user_global_unique_id,user_ldap_dn to user_account
ALTER TABLE user_account
    ADD COLUMN user_os_name CHARACTER VARYING;
ALTER TABLE user_account
    ADD COLUMN user_ldap_dn CHARACTER VARYING;
ALTER TABLE user_account
    ADD COLUMN user_global_unique_id CHARACTER VARYING;
ALTER TABLE user_account
    ADD COLUMN user_login_name CHARACTER VARYING;
ALTER TABLE user_account
    ADD COLUMN user_login_domain CHARACTER VARYING;

-- reinstate numeric user_state to user_account
ALTER TABLE user_account
    ADD COLUMN user_state SMALLINT;

-- add extobjdir_process_detail as 1:1 with external_object_directory ( c.f.case_overflow )
CREATE TABLE extobjdir_process_detail
(
    epd_id           INTEGER                  NOT NULL,
    eod_id           INTEGER                  NOT NULL UNIQUE,
    event_date_ts    TIMESTAMP WITH TIME ZONE,
    update_retention BOOLEAN                  NOT NULL,
    created_ts       TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by       INTEGER                  NOT NULL,
    last_modified_ts TIMESTAMP WITH TIME ZONE NOT NULL,
    last_modified_by INTEGER                  NOT NULL
);

ALTER TABLE extobjdir_process_detail
    ADD CONSTRAINT epd_external_object_directory_fk
        FOREIGN KEY (eod_id) REFERENCES external_object_directory (eod_id);

-- Note: FKs for created_by and last_modified_by are expected to be added in the next migration (v72_3)

CREATE SEQUENCE epd_seq CACHE 20;

-- add table transcription_linked_case, as per event_linked_case
CREATE TABLE transcription_linked_case
(
    tlc_id          INTEGER NOT NULL,
    tra_id          INTEGER NOT NULL,
    cas_id          INTEGER,
    courthouse_name CHARACTER VARYING,
    case_number     CHARACTER VARYING
);

ALTER TABLE transcription_linked_case
    ADD CONSTRAINT transcription_linked_case_court_case_fk
        FOREIGN KEY (cas_id) REFERENCES court_case (cas_id);

ALTER TABLE transcription_linked_case
    ADD CONSTRAINT transcription_linked_case_transcription_fk
        FOREIGN KEY (tra_id) REFERENCES transcription (tra_id);

ALTER TABLE transcription_linked_case
    ADD CONSTRAINT tlc_modern_or_legacy_case_nn
        CHECK ((cas_id is not null) or (courthouse_name is not null and case_number is not null));

CREATE SEQUENCE tlc_seq CACHE 20;

-- v10 amend index on user_account from user_name to user_full_name
DROP INDEX IF EXISTS usr_un_idx;

--v24 add group_display_name to security_group, for direct mapping from legacy
ALTER TABLE security_group
    ADD COLUMN group_display_name CHARACTER VARYING;
