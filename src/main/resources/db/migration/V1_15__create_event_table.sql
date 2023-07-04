create table if not exists event
(
    eve_id          integer not null constraint event_pk primary key,
    ctr_id          integer constraint event_courtroom_fk references courtroom(ctr_id),
    evt_id          integer constraint event_event_type_fk references event_type(evt_id),
    r_event_object_id   character varying(16),
    c_event_id          numeric,
    event_name          character varying,
    event_text          character varying,
    c_time_stamp        timestamp with time zone,
    r_version_label     character varying(32),
    message_id          character varying,
    i_superseded        boolean,
    i_version           integer
);
create sequence if not exists eve_seq;

