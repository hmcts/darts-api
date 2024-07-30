ALTER TABLE annotation_document ADD COLUMN is_deleted BOOLEAN NOT NULL default false;
ALTER TABLE annotation_document ADD COLUMN deleted_by INTEGER;
ALTER TABLE annotation_document ADD COLUMN deleted_ts TIMESTAMP WITH TIME ZONE;
ALTER TABLE annotation_document ADD COLUMN ret_conf_score INTEGER;
ALTER TABLE annotation_document ADD COLUMN ret_conf_reason CHARACTER VARYING;

ALTER TABLE case_document ADD COLUMN is_deleted BOOLEAN NOT NULL default false;
ALTER TABLE case_document ADD COLUMN deleted_by INTEGER;
ALTER TABLE case_document ADD COLUMN deleted_ts TIMESTAMP WITH TIME ZONE;
ALTER TABLE case_document ADD COLUMN ret_conf_score INTEGER;
ALTER TABLE case_document ADD COLUMN ret_conf_reason CHARACTER VARYING;

ALTER TABLE court_case RENAME column ret_conf_level to ret_conf_score;
ALTER TABLE court_case ADD COLUMN ret_conf_updated_ts TIMESTAMP WITH TIME ZONE;

ALTER TABLE daily_list ADD COLUMN content_object_id CHARACTER VARYING;
ALTER TABLE daily_list ADD COLUMN clip_id CHARACTER VARYING;
ALTER TABLE daily_list ADD COLUMN external_location UUID;
ALTER TABLE daily_list ADD COLUMN elt_id INTEGER;

ALTER TABLE event ADD COLUMN is_data_anonymised BOOLEAN NOT NULL DEFAULT false;

ALTER TABLE external_object_directory ADD COLUMN data_ingestion_ts TIMESTAMP WITH TIME ZONE;

ALTER TABLE media ADD COLUMN ret_conf_score INTEGER;
ALTER TABLE media ADD COLUMN ret_conf_reason CHARACTER VARYING;

ALTER TABLE transcription ADD COLUMN is_current BOOLEAN;

ALTER TABLE transcription_comment ADD COLUMN is_data_anonymised BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE transcription_comment_aud ADD COLUMN is_data_anonymised BOOLEAN NOT NULL DEFAULT false;

ALTER TABLE transcription_document ADD COLUMN is_deleted BOOLEAN NOT NULL default false;
ALTER TABLE transcription_document ADD COLUMN deleted_by INTEGER;
ALTER TABLE transcription_document ADD COLUMN deleted_ts TIMESTAMP WITH TIME ZONE;
ALTER TABLE transcription_document ADD COLUMN ret_conf_score INTEGER;
ALTER TABLE transcription_document ADD COLUMN ret_conf_reason CHARACTER VARYING;

ALTER TABLE case_retention ADD COLUMN confidence_category INTEGER;

CREATE UNIQUE INDEX rol_rol_nm_idx       ON security_role(role_name);
CREATE UNIQUE INDEX grp_grp_nm_idx       ON security_group(group_name);