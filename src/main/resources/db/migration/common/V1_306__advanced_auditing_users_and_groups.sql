create table user_account_aud
(
    usr_id              integer not null,
    user_email_address  varchar,
    description         varchar,
    is_active           boolean,
    user_full_name      varchar,
    rev                 integer not null,
    revtype             smallint,
    primary key (rev, usr_id)
);

create table security_group_aud
(
    grp_id                 integer not null,
    group_name             varchar,
    display_name           varchar,
    description            varchar,
    rev                    integer not null,
    revtype                smallint,
    primary key (rev, grp_id)
);

create table security_group_user_account_ae_aud
(
    usr_id  integer not null,
    grp_id  integer not null,
    rev     integer not null,
    revtype smallint,
    primary key (rev, usr_id, grp_id)
);


alter table user_account_aud
    add constraint user_account_aud_revinfo_fk
        foreign key (rev)
            references revinfo;

alter table security_group_aud
    add constraint security_group_aud_revinfo_fk
        foreign key (rev)
            references revinfo;

alter table security_group_user_account_ae_aud
    add constraint security_group_user_account_ae_aud_revinfo_fk
        foreign key (rev)
            references revinfo;
