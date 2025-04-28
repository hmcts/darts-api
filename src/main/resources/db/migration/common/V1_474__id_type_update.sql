alter table darts.audit
    alter column aud_id type bigint;
alter table darts.data_anonymisation
    alter column eve_id type bigint;
alter table darts.event_linked_case
    alter column elc_id type bigint;
alter table darts.event_linked_case
    alter column eve_id type bigint;
alter table darts.external_object_directory
    alter column eod_id type bigint;
alter table darts.external_object_directory
    alter column med_id type bigint;
alter table darts.extobjdir_process_detail
    alter column epd_id type bigint;
alter table darts.extobjdir_process_detail
    alter column eod_id type bigint;
alter table darts.hearing_event_ae
    alter column eve_id type bigint;
alter table darts.hearing_media_ae
    alter column med_id type bigint;
alter table darts.media
    alter column med_id type bigint;
alter table darts.media_linked_case
    alter column mlc_id type bigint;
alter table darts.media_linked_case
    alter column med_id type bigint;
alter table darts.notification
    alter column not_id type bigint;
alter table darts.object_admin_action
    alter column med_id type bigint;
alter table darts.object_retrieval_queue
    alter column med_id type bigint;
alter table darts.object_state_record
    alter column eod_id type bigint;
alter table darts.object_state_record
    alter column arm_eod_id type bigint;


DROP VIEW IF EXISTS hearing_reporting_restrictions;

alter table darts.event
    alter column eve_id type bigint;
alter table darts.hearing_event_ae
    alter column eve_id type bigint;

CREATE VIEW hearing_reporting_restrictions AS
SELECT h.cas_id,
       he.hea_id,
       eh.event_name,
       eh.event_type,
       eh.event_sub_type,
       eh.active,
       e.eve_id,
       e.ctr_id,
       e.evh_id,
       e.event_object_id,
       e.event_id,
       e.event_text,
       e.event_ts,
       e.version_label,
       e.message_id,
       e.created_ts,
       e.created_by,
       e.last_modified_ts,
       e.last_modified_by,
       e.is_log_entry,
       e.chronicle_id,
       e.antecedent_id
FROM darts.event_handler eh
         JOIN darts.event e ON e.evh_id = eh.evh_id
         JOIN darts.hearing_event_ae he ON he.eve_id = e.eve_id
         JOIN darts.hearing h ON h.hea_id = he.hea_id
WHERE eh.is_reporting_restriction = true;