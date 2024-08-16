alter table external_object_directory alter column osr_uuid type bigint USING osr_uuid::bigint;
alter table object_state_record alter column osr_uuid type bigint USING osr_uuid::bigint;
