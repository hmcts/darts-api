create table if not exists  reporting_restrictions
(
  rer_id      integer,
  rer_description character varying,
  constraint reporting_restrictions_type_pkey primary key (rer_id)
);
create sequence if not exists rer_seq;



create table if not exists  courtroom
(
  ctr_id     integer not null primary key,
  cth_id     integer not null
    constraint courtroom_courthouse_fk references  courthouse (cth_id),
  courtroom_name varchar not null,
  CONSTRAINT ctr_cth_name_unique UNIQUE (cth_id, courtroom_name)
);
create sequence if not exists ctr_seq;


create table if not exists  hearing
(
  hea_id             integer,
  cas_id                 INTEGER                    NOT NULL,
  ctr_id             integer references  courtroom (ctr_id),
  c_judges               character varying array,
  c_hearing_date         date,
  c_scheduled_start_time time,
  hearing_is_actual      boolean,
  c_judge_hearing_date       CHARACTER VARYING,
  constraint hearing_pkey primary key (hea_id),
  CONSTRAINT hearing_unique UNIQUE (cas_id, ctr_id, c_hearing_date)
);
create sequence if not exists hea_seq;


