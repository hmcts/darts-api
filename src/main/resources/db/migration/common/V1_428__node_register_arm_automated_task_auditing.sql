create table node_register_aud
(
    node_id             integer not null,
    hostname            varchar,
    ip_address          varchar,
    mac_address         varchar,
    node_type           varchar,
    rev                 integer not null,
    revtype             smallint,
    primary key (rev, node_id)
);

alter table node_register_aud
    add constraint node_register_aud_revinfo_fk
        foreign key (rev)
            references revinfo;


create table arm_automated_task_aud
(
    aat_id                  integer not null,
    aut_id                  integer not null,
    rpo_csv_start_hour      integer,
    rpo_csv_end_hour        integer,
    arm_replay_start_ts     TIMESTAMP WITH TIME ZONE,
    arm_replay_end_ts       TIMESTAMP WITH TIME ZONE,
    arm_attribute_type      varchar,
    rev                     integer not null,
    revtype                 smallint,
    primary key (rev, aat_id)
);

alter table arm_automated_task_aud
    add constraint arm_automated_task_aud_revinfo_fk
        foreign key (rev)
            references revinfo;