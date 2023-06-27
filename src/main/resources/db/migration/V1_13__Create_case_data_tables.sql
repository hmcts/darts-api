create table if not exists  moj_reporting_restrictions
(
  moj_rer_id      integer,
  rer_description character varying,
  constraint reporting_restrictions_type_pkey primary key (moj_rer_id)
);
create sequence if not exists moj_rer_seq;


create table if not exists  moj_case
(
  moj_cas_id         integer not null primary key,
  moj_rer_id         integer
    constraint moj_case_reporting_restriction_fk references  moj_reporting_restrictions (moj_rer_id),
  r_case_object_id   varchar(16),
  c_type                    CHARACTER VARYING,
  c_case_id          varchar,
  c_closed           boolean,
  c_interpreter_used boolean,
  c_case_closed_ts   timestamp with time zone,
  c_defendant        character varying array,
  c_prosecutor       character varying array,
  c_defence          character varying array,
  retain_until_ts    timestamp with time zone,
  r_version_label    varchar(32),
  i_superseded              BOOLEAN,
  i_version                 SMALLINT);
);
create sequence if not exists  moj_cas_seq cache 20;


create table if not exists  moj_courtroom
(
  moj_ctr_id     integer not null primary key,
  moj_cth_id     integer not null
    constraint moj_courtroom_courthouse_fk references  moj_courthouse (moj_cth_id),
  courtroom_name varchar not null
);
create sequence if not exists moj_ctr_seq;


create table if not exists  moj_hearing
(
  moj_hea_id             integer,
  moj_cas_id                 INTEGER                    NOT NULL,
  moj_ctr_id             integer references  moj_courtroom (moj_ctr_id),
  c_judges               character varying array,
  c_hearing_date         date,
  c_scheduled_start_time time,
  hearing_is_actual      boolean,
  c_judge_hearing_date       CHARACTER VARYING,
  constraint moj_hearing_pkey primary key (moj_hea_id)
);
create sequence if not exists moj_hea_seq;


