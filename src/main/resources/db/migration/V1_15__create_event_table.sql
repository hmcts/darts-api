create table if not exists moj_event
(
    moj_eve_id          integer not null constraint moj_event_pk primary key,
    moj_ctr_id          integer constraint moj_event_courtroom_fk references moj_courtroom(moj_ctr_id),
    moj_evt_id          integer constraint moj_event_event_type_fk references moj_event_type(moj_evt_id),
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
create sequence if not exists moj_eve_seq;

