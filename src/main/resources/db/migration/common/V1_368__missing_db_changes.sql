update annotation set created_ts = current_timestamp where created_ts is null;
ALTER TABLE IF EXISTS annotation ALTER COLUMN created_ts SET NOT NULL;

update annotation set last_modified_ts = current_timestamp where last_modified_ts is null;
ALTER TABLE IF EXISTS annotation ALTER COLUMN last_modified_ts SET NOT NULL;

update defendant set created_ts = current_timestamp where created_ts is null;
ALTER TABLE IF EXISTS defendant ALTER COLUMN created_ts SET NOT NULL;

update defendant set last_modified_ts = current_timestamp where last_modified_ts is null;
ALTER TABLE IF EXISTS defendant ALTER COLUMN last_modified_ts SET NOT NULL;

ALTER TABLE notification ADD CONSTRAINT notification_created_by_fk FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

ALTER TABLE notification ADD CONSTRAINT notification_modified_by_fk FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);

ALTER TABLE object_retrieval_queue rename CONSTRAINT object_retrieval_queue_modified_by_fk to object_retrieval_queue_last_modified_by_fk;

update security_group n set created_by = 0 where created_by is null or not exists (select 1 from user_account ua where ua.usr_id = n.created_by);;
ALTER TABLE IF EXISTS security_group ALTER COLUMN created_by SET NOT NULL;

update security_group n set last_modified_by = 0 where last_modified_by is null or not exists (select 1 from user_account ua where ua.usr_id = n.last_modified_by);;
ALTER TABLE IF EXISTS security_group ALTER COLUMN last_modified_by SET NOT NULL;

ALTER TABLE transcription_document rename CONSTRAINT transcription_document_modified_by_fk to transcription_document_last_modified_by_fk;




COMMENT ON COLUMN transcription_document.tra_id IS 'foreign key from transcription';

COMMENT ON COLUMN transcription_document.trd_id IS 'primary key of transcription_document';
COMMENT ON COLUMN audit.aua_id IS 'primary key of audit_activity';
COMMENT ON COLUMN case_transcription_ae.cas_id IS 'foreign key from case, part of composite natural key and PK';

COMMENT ON COLUMN case_transcription_ae.tra_id IS 'foreign key from transcription, part of composite natural key and PK';
COMMENT ON COLUMN transcription_comment.trw_id IS 'foreign key from transcription_workflow';

COMMENT ON COLUMN node_register.node_id IS 'primary key of node_register';
COMMENT ON COLUMN hearing_transcription_ae.hea_id IS 'foreign key from hearing, part of composite natural key and PK';

COMMENT ON COLUMN hearing_transcription_ae.tra_id IS 'foreign key from transcription, part of composite natural key and PK';
COMMENT ON COLUMN hearing_annotation_ae.ann_id IS 'foreign key from annotation, part of composite natural key and PK';

COMMENT ON COLUMN hearing_annotation_ae.hea_id IS 'foreign key from hearing, part of composite natural key and PK';
COMMENT ON COLUMN event_handler.handler IS 'to indicate if the event pertains to reporting restrictions, both application of, and lifting, in order to provide timeline of RR as applied to a case';
COMMENT ON COLUMN external_object_directory.trd_id IS 'foreign key from transcription_document';
COMMENT ON COLUMN annotation_document.ado_id IS 'primary key of annotation_document';

COMMENT ON COLUMN annotation_document.ann_id IS 'foreign key from annotation';
COMMENT ON COLUMN transcription.hearing_date IS 'directly sourced from moj_transcription_s';

COMMENT ON COLUMN transcription.tru_id IS 'foreign key from transcription_urgency';

COMMENT ON COLUMN hearing_media_ae.hea_id IS 'foreign key from hearing, part of composite natural key and PK';
COMMENT ON COLUMN media_request.request_status IS 'status of the media request';
COMMENT ON TABLE transformed_media IS 'to accommodate the possibility that a single media_request may be fulfilled as more than one resulting piece of media';

COMMENT ON COLUMN transformed_media.output_filename IS 'filename of the requested media object, possibly migrated from moj_transformation_request_s';

COMMENT ON COLUMN transformed_media.output_format IS 'format of the requested media object, possibly migrated from moj_transformation_s';

COMMENT ON TABLE event_linked_case IS 'content is to be populated via migration';
COMMENT ON TABLE media_linked_case IS 'content is to be populated via migration';
COMMENT ON COLUMN external_object_directory.ado_id IS 'foreign key from annotation_document';