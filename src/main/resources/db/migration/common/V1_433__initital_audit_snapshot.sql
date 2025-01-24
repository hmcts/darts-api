-- Create one revinfo as all tables will be created at the same time, and there is no additional value in giving each table its own revinfo
insert into revinfo (rev, revtstmp)
values (nextval('revinfo_seq'), EXTRACT(EPOCH FROM NOW()));


insert into courthouse_aud
select cth_id,
       courthouse_code,
       courthouse_name,
       display_name,
       currval('revinfo_seq'),
       0 as revtype
from courthouse;

insert into courthouse_region_ae_aud
select cth_id,
       reg_id,
       currval('revinfo_seq'),
       0 as revtype
from courthouse_region_ae;

insert into security_group_aud
select grp_id,
       group_name,
       display_name,
       description,
       currval('revinfo_seq'),
       0 as revtype
from security_group;

insert into security_group_courthouse_ae_aud
select cth_id,
       grp_id,
       currval('revinfo_seq'),
       0 as revtype
from security_group_courthouse_ae;

insert into event_handler_aud
select evh_id,
       event_type,
       event_sub_type,
       event_name,
       handler,
       active,
       currval('revinfo_seq'),
       is_reporting_restriction,
       0 as revtype
from event_handler;

insert into retention_policy_type_aud
select rpt_id,
       fixed_policy_key,
       policy_name,
       duration,
       display_name,
       description,
       policy_start_ts,
       policy_end_ts,
       currval('revinfo_seq'),
       0 as revtype
from retention_policy_type;

insert into automated_task_aud
select aut_id,
       task_name,
       task_description,
       cron_expression,
       cron_editable,
       batch_size,
       task_enabled,
       currval('revinfo_seq'),
       0 as revtype
from automated_task;

insert into arm_automated_task_aud
select aat_id,
       aut_id,
       rpo_csv_start_hour,
       rpo_csv_end_hour,
       arm_replay_start_ts,
       arm_replay_end_ts,
       arm_attribute_type,
       currval('revinfo_seq'),
       0 as revtype
from arm_automated_task;