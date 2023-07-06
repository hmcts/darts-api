create table if not exists moj_hearing_event_ae
(
    moj_hev_id integer not null
        constraint moj_hearing_event_ae_pk primary key,
    moj_hea_id integer not null
        constraint moj_hearing_event_ae_hearing_fk references moj_hearing(moj_hea_id),
    moj_eve_id integer not null
        constraint moj_hearing_event_ae_event_fk references moj_event(moj_eve_id)
);
create sequence moj_hev_seq cache 20;