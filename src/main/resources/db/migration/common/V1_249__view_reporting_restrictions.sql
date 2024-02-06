CREATE VIEW hearing_reporting_restrictions AS
SELECT h.cas_id,
       he.hea_id,
       eh.event_type,
       eh.event_sub_type,
       eh.active,
       e.*
FROM darts.event_handler eh
       JOIN darts.event e ON e.evh_id = eh.evh_id
       JOIN darts.hearing_event_ae he ON he.eve_id = e.eve_id
       JOIN darts.hearing h ON h.hea_id = he.hea_id
WHERE eh.is_reporting_restriction = true
ORDER BY h.cas_id, he.hea_id, e.event_ts;
