create table media_request_aud
(
    mer_id          integer not null,
    hea_id          integer,
    requestor       integer,
    current_owner   integer,
    request_type    varchar(255),
    start_ts        TIMESTAMP WITH TIME ZONE,
    end_ts          TIMESTAMP WITH TIME ZONE,
    rev             integer not null,
    revtype         smallint,
    primary key (rev, mer_id)
);

alter table media_request_aud
    add constraint media_request_aud_revinfo_fk
        foreign key (rev)
            references revinfo;