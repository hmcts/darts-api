DROP VIEW IF EXISTS hearing_reporting_restrictions;

CREATE VIEW hearing_reporting_restrictions AS
SELECT
            row_number() OVER () AS id,
            h.cas_id,
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
            e.case_number,
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