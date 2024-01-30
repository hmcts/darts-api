ALTER TABLE annotation_document ADD COLUMN clip_id                     CHARACTER VARYING(54);

ALTER TABLE courthouse ALTER COLUMN courthouse_code DROP NOT NULL;

ALTER TABLE event ALTER COLUMN ctr_id SET NOT NULL;
ALTER TABLE event ALTER COLUMN event_ts SET NOT NULL;
ALTER TABLE event ADD COLUMN chronicle_id                CHARACTER VARYING(16);
ALTER TABLE event ADD COLUMN antecedent_id                CHARACTER VARYING(16);

ALTER TABLE external_object_directory ADD COLUMN error_code                  CHARACTER VARYING;
ALTER TABLE external_object_directory ADD COLUMN is_response_cleaned         BOOLEAN                       NOT NULL  DEFAULT false;

update media set ctr_id=1 where ctr_id is null;
ALTER TABLE media ALTER COLUMN ctr_id SET NOT NULL;
ALTER TABLE media ADD COLUMN clip_id                     CHARACTER VARYING(54);
ALTER TABLE media ADD COLUMN chronicle_id                CHARACTER VARYING(16);
ALTER TABLE media ADD COLUMN antecedent_id                CHARACTER VARYING(16);

ALTER TABLE transcription ADD COLUMN chronicle_id                CHARACTER VARYING(16);
ALTER TABLE transcription ADD COLUMN antecedent_id                CHARACTER VARYING(16);

ALTER TABLE transcription_document ADD COLUMN clip_id                     CHARACTER VARYING(54);



ALTER TABLE case_management_retention DROP COLUMN event_ts;
ALTER TABLE case_management_retention ALTER COLUMN eve_id SET NOT NULL;


ALTER TABLE security_group DROP COLUMN group_class;
ALTER TABLE security_group DROP COLUMN group_display_name;
