create table retention_policy_type_aud
(
    rpt_id              integer not null,
    fixed_policy_key    varchar,
    policy_name         varchar,
    duration            varchar,
    display_name        varchar,
    description         varchar,
    policy_start_ts     timestamp with time zone,
    policy_end_ts       timestamp with time zone,
    rev                 integer not null,
    revtype             smallint,
    primary key (rev, rpt_id)
);

alter table retention_policy_type_aud
    add constraint retention_policy_type_aud_revinfo_fk
        foreign key (rev)
            references revinfo;
