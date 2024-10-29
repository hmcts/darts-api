update media_linked_case set source = 0 where source is null:
ALTER TABLE media_linked_case ALTER COLUMN source SET NOT NULL;

update media_linked_case set created_ts = current_timestamp where created_ts is null:
ALTER TABLE media_linked_case ALTER COLUMN created_ts SET NOT NULL;

update media_linked_case set created_by = 0 where created_by is null:
ALTER TABLE media_linked_case ALTER COLUMN created_by SET NOT NULL;

INSERT INTO user_account VALUES (-5, NULL, 'AudioLinkingAutomatedTask', NULL, 'AudioLinkingAutomatedTask, '2024-01-01', '2024-01-01', NULL, NULL, NULL, NULL, true, true, 'AudioLinkingAutomatedTask');



