create table transcription_aud
(
    tra_id              integer not null,
    ctr_id              integer,
    trt_id              integer,
    start_ts            timestamp with time zone,
    end_ts              timestamp with time zone,
    hearing_date        date,
    rev                 integer not null,
    revtype             smallint,
    primary key (rev, tra_id)
);

create table transcription_workflow_aud
(
    trw_id         integer not null,
    tra_id         integer,
    trs_id         integer,
    workflow_actor integer,
    rev            integer not null,
    revtype        smallint,
    primary key (rev, trw_id)
);

create table transcription_comment_aud
(
    trc_id                  integer not null,
    tra_id                  integer,
    transcription_object_id varchar(16),
    transcription_comment   varchar,
    comment_ts              timestamp with time zone,
    author                  integer,
    trw_id                  integer,
    rev                     integer not null,
    revtype                 smallint,
    primary key (rev, trc_id)
);

alter table transcription_aud
    add constraint transcription_aud_revinfo_fk
        foreign key (rev)
            references revinfo;

alter table transcription_workflow_aud
    add constraint transcription_workflow_aud_revinfo_fk
        foreign key (rev)
            references revinfo;

alter table transcription_comment_aud
    add constraint transcription_comment_aud_revinfo_fk
        foreign key (rev)
            references revinfo;