update event_handler set "handler" = 'StopAndCloseHandler' where evh_id in (214);
update event_handler set "handler" = 'DarStopHandler' where evh_id in (215,216);

ALTER TABLE case_management_retention DROP COLUMN is_manual_override;
ALTER TABLE case_retention ALTER COLUMN retain_until_applied_on_ts DROP NOT NULL;
