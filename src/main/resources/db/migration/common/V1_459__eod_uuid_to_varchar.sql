alter table external_object_directory
    alter column external_location type CHARACTER VARYING;
alter table transient_object_directory
    alter column external_location type CHARACTER VARYING;
alter table daily_list
    alter column external_location type CHARACTER VARYING;
alter table daily_list
    add column osr_uuid BIGINT;
alter table object_state_record
    add column dal_id int;

