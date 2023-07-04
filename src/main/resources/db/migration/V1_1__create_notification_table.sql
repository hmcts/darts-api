CREATE TABLE IF NOT EXISTS notification (
  id                     integer                     NOT NULL
, event_id               text                        NOT NULL
, case_id                text                        NOT NULL
, email_address          text                        NOT NULL
, status                 text                        NOT NULL
, attempts               integer                     NOT NULL DEFAULT 0
, template_values        text
, created_date_time      timestamp without time zone NOT NULL
, last_updated_date_time timestamp without time zone NOT NULL
, CONSTRAINT notification_pkey PRIMARY KEY (id)
);

alter table daily_list
ALTER COLUMN daily_list_content TYPE text,
ALTER COLUMN c_start_ts TYPE date,
ALTER COLUMN c_end_ts TYPE date;
alter table daily_list
RENAME COLUMN c_start_ts TO c_start_date;
alter table daily_list
RENAME COLUMN c_end_ts TO c_end_date;

alter table hearing
RENAME COLUMN c_judge TO c_judges;

alter table media
ALTER COLUMN c_channel TYPE integer,
ALTER COLUMN c_total_channels TYPE integer;


--Temporary workaround, as Notification logic needs to be changed to take into account Case table.
--This will be done in a separate ticket.
alter table notification
drop constraint notification_case_fk;
alter table notification
ALTER COLUMN cas_id TYPE text;
