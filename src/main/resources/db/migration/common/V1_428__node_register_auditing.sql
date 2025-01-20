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