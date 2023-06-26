create table if not exists darts.moj_reporting_restrictions
(
  moj_rer_id      integer,
  rer_description character varying,
  constraint reporting_restrictions_type_pkey primary key (moj_rer_id)
);
create sequence if not exists moj_rer_seq;


create table if not exists darts.moj_case
(
  moj_cas_id         integer not null primary key,
  moj_rer_id         integer
    constraint moj_case_reporting_restriction_fk references darts.moj_reporting_restrictions (moj_rer_id),
  r_case_object_id   varchar(16),
  c_case_id          varchar,
  c_closed           boolean,
  c_interpreter_used boolean,
  c_case_closed_ts   timestamp with time zone,
  c_defendant        character varying array,
  c_prosecutor       character varying array,
  c_defence          character varying array,
  retain_until_ts    timestamp with time zone,
  r_version_label    varchar(32)
);
create sequence if not exists darts.moj_cas_seq cache 20;


create table if not exists darts.moj_courtroom
(
  moj_ctr_id     integer not null primary key,
  moj_cth_id     integer not null
    constraint moj_courtroom_courthouse_fk references darts.moj_courthouse (moj_cth_id),
  courtroom_name varchar not null
);
create sequence if not exists moj_ctr_seq;


create table if not exists darts.moj_hearing
(
  moj_hea_id             integer,
  moj_ctr_id             integer references darts.moj_courtroom (moj_ctr_id),
  c_judges               character varying array,
  c_judge_hearing_date   date,
  c_hearing_date         date,
  c_scheduled_start_time timestamp with time zone,
  hearing_is_actual      boolean,
  constraint moj_hearing_pkey primary key (moj_hea_id)
);
create sequence if not exists moj_hea_seq;


create table if not exists darts.moj_case_hearing_ae
(
  moj_cha_id serial,
  moj_cas_id integer references darts.moj_case (moj_cas_id),
  moj_hea_id integer references darts.moj_hearing (moj_hea_id),
  constraint moj_case_hearing_ae_pkey primary key (moj_cha_id)
);
