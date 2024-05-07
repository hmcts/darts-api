create sequence revinfo_seq start with 1 increment by 50;

create table revinfo
(
    rev      serial not null,
    revtstmp bigint,
    primary key (rev)
);

create table courthouse_aud
(
    cth_id          integer not null,
    courthouse_code integer,
    courthouse_name varchar(255),
    display_name    varchar(255),
    rev             integer not null,
    revtype         smallint,
    primary key (rev, cth_id)
);

create table courthouse_region_ae_aud
(
    cth_id  integer not null,
    reg_id  integer not null,
    rev     integer not null,
    revtype smallint,
    primary key (cth_id, rev, reg_id)
);

create table security_group_courthouse_ae_aud
(
    cth_id  integer not null,
    grp_id  integer not null,
    rev     integer not null,
    revtype smallint,
    primary key (cth_id, rev, grp_id)
);

alter table courthouse_aud
    add constraint courthouse_aud_revinfo_fk
        foreign key (rev)
            references revinfo;

alter table courthouse_region_ae_aud
    add constraint courthouse_region_ae_aud_revinfo_fk
        foreign key (rev)
            references revinfo;

alter table security_group_courthouse_ae_aud
    add constraint security_group_courthouse_ae_aud_revinfo_fk
        foreign key (rev)
            references revinfo;