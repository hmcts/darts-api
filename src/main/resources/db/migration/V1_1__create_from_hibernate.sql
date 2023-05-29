create sequence audio_request_seq start with 1 increment by 50;
create sequence notification_seq start with 1 increment by 50;
create sequence revinfo_seq start with 1 increment by 50;

    create table audio_request (
       request_id integer not null,
        attempts integer,
        case_id varchar(255),
        created_date_time timestamp(6),
        end_time timestamp(6),
        last_updated_date_time timestamp(6),
        outbound_location varchar(255),
        request_type varchar(255),
        requester varchar(255),
        start_time timestamp(6),
        status varchar(255),
        primary key (request_id)
    );

    create table moj_annotation (
       moj_ann_id serial not null,
        c_courthouse varchar(64),
        c_courtroom varchar(64),
        c_end date,
        r_case_object_id varchar(16),
        r_annotation_object_id varchar(16),
        r_version_label varchar(32),
        c_reporting_restrictions integer,
        c_start date,
        i_superseded boolean,
        c_text varchar(2000),
        c_time_stamp timestamp(6),
        i_version_label smallint,
        moj_cas_id integer,
        primary key (moj_ann_id)
    );

    create table moj_cached_media (
       moj_med_id serial not null,
        c_channel integer,
        c_courthouse varchar(64),
        c_courtroom varchar(64),
        c_end date,
        c_last_accessed timestamp(6),
        r_case_object_id varchar(32),
        r_cached_media_object_id varchar(16),
        r_version_label varchar(32),
        c_log_id varchar(16),
        c_reference_id varchar(32),
        c_reporting_restrictions integer,
        c_start date,
        i_superseded boolean,
        c_total_channels integer,
        i_version_label smallint,
        moj_cas_id integer,
        primary key (moj_med_id)
    );

    create table moj_case (
       moj_cas_id serial not null,
        c_case_closed_date date,
        c_case_id varchar(32),
        c_closed integer,
        c_courthouse varchar(64),
        c_courtroom varchar(64),
        c_interpreter_used smallint,
        r_case_object_id varchar(16),
        r_courthouse_object_id varchar(16),
        r_version_label varchar(32),
        c_reporting_restrictions varchar(128),
        c_scheduled_start date,
        i_superseded boolean,
        c_type varchar(32),
        c_upload_priority integer,
        i_version_label smallint,
        moj_crt_id integer not null,
        primary key (moj_cas_id)
    );

    create table moj_case_event_ae (
       case_moj_cas_id integer not null,
        the_events_moj_eve_id integer not null,
        primary key (case_moj_cas_id, the_events_moj_eve_id)
    );

    create table moj_case_media_ae (
       case_moj_cas_id integer not null,
        the_medias_moj_med_id integer not null,
        primary key (case_moj_cas_id, the_medias_moj_med_id)
    );

    create table moj_courthouse (
       moj_crt_id serial not null,
        c_alias_set_id varchar(16),
        c_code varchar(32),
        c_id varchar(32),
        r_courthouse_object_id varchar(16),
        r_version_label varchar(32),
        i_superseded boolean,
        i_version_label smallint,
        primary key (moj_crt_id)
    );

    create table moj_daily_list (
       moj_dal_id serial not null,
        c_crown_court_code varchar(100),
        c_crown_court_name varchar(200),
        c_daily_list_id integer,
        c_daily_list_id_s varchar(100),
        c_daily_list_source varchar(3),
        c_end_date date,
        c_job_status varchar(20),
        r_courthouse_object_id varchar(16),
        r_daily_list_object_id varchar(16),
        r_version_label varchar(32),
        c_start_date date,
        i_superseded boolean,
        c_timestamp timestamp(6),
        c_unique_id varchar(200),
        i_version_label smallint,
        moj_crt_id integer,
        primary key (moj_dal_id)
    );

    create table moj_event (
       moj_eve_id serial not null,
        c_courthouse varchar(64),
        c_courtroom varchar(64),
        c_end date,
        c_event_id integer,
        r_event_object_id varchar(16),
        r_version_label varchar(32),
        c_reporting_restrictions integer,
        c_start date,
        i_superseded boolean,
        c_text varchar(2000),
        c_time_stamp timestamp(6),
        i_version_label smallint,
        primary key (moj_eve_id)
    );

    create table moj_hearing (
       moj_hea_id serial not null,
        c_defence varchar(2000),
        c_defendant varchar(2000),
        c_hearing_date date,
        c_judge varchar(2000),
        c_judge_hearing_date varchar(2000),
        r_case_object_id varchar(16),
        c_prosecutor varchar(2000),
        i_superseded boolean,
        i_version_label smallint,
        moj_cas_id integer,
        primary key (moj_hea_id)
    );

    create table moj_media (
       moj_med_id serial not null,
        c_case_id varchar(32),
        r_case_object_id varchar(16),
        c_channel integer,
        moj_crt_id integer,
        c_courtroom varchar(64),
        c_end date,
        r_media_object_id varchar(16),
        r_version_label varchar(32),
        c_reference_id varchar(32),
        c_reporting_restrictions integer,
        c_start date,
        i_superseded boolean,
        c_total_channels integer,
        i_version_label smallint,
        primary key (moj_med_id)
    );

    create table moj_report (
       moj_rep_id serial not null,
        r_report_object_id varchar(16),
        r_version_label varchar(32),
        c_name varchar(32),
        c_query varchar(2048),
        c_recipients varchar(1024),
        c_subject varchar(256),
        i_superseded boolean,
        c_text varchar(1024),
        i_version_label smallint,
        primary key (moj_rep_id)
    );

    create table moj_transcription (
       moj_tra_id serial not null,
        c_company varchar(64),
        c_courthouse varchar(64),
        c_courtroom varchar(64),
        c_current_state varchar(32),
        c_end date,
        c_hearing_date date,
        r_case_object_id varchar(16),
        r_transcription_object_id varchar(16),
        r_version_label varchar(32),
        c_notification_type varchar(64),
        c_reporting_restrictions integer,
        c_requestor varchar(32),
        c_start date,
        i_superseded boolean,
        c_type integer,
        c_urgency varchar(32),
        c_urgent integer,
        i_version_label smallint,
        moj_cas_id integer,
        primary key (moj_tra_id)
    );

    create table moj_transcription_comment (
       moj_trc_id serial not null,
        c_comment varchar(1024),
        r_transcription_comment_object_id varchar(16),
        r_transcription_object_id varchar(16),
        i_superseded boolean,
        i_version_label smallint,
        moj_tra_id integer not null,
        primary key (moj_trc_id)
    );

    create table moj_transformation_log (
       moj_trl_id serial not null,
        c_case_id varchar(32),
        c_courthouse varchar(64),
        r_case_object_id varchar(16),
        r_transformation_log_object_id varchar(16),
        r_version_label varchar(32),
        c_received_date date,
        c_requested_date date,
        i_superseded boolean,
        i_version_label smallint,
        moj_cas_id integer,
        primary key (moj_trl_id)
    );

    create table moj_transformation_request (
       moj_trr_id serial not null,
        c_audio_folder_id varchar(16),
        c_channel integer,
        c_court_log_id varchar(16),
        c_courthouse varchar(64),
        c_courtroom varchar(64),
        c_end date,
        r_case_object_id varchar(16),
        r_transformation_request_object_id varchar(16),
        r_version_label varchar(32),
        c_output_file varchar(100),
        c_output_format varchar(12),
        c_priority integer,
        c_reference_id varchar(32),
        c_reporting_restrictions integer,
        c_requestor varchar(32),
        c_start date,
        i_superseded boolean,
        c_total_channels integer,
        c_type varchar(12),
        i_version_label smallint,
        moj_cas_id integer not null,
        primary key (moj_trr_id)
    );

    create table notification (
       id integer not null,
        attempts integer default 0,
        case_id varchar(255),
        created_date_time timestamp(6),
        email_address varchar(255),
        event_id varchar(255),
        last_updated_date_time timestamp(6),
        status varchar(255),
        template_values varchar(255),
        primary key (id)
    );

    create table notification_audit (
       id integer not null,
        rev integer not null,
        revtype smallint,
        attempts integer,
        case_id varchar(255),
        created_date_time timestamp(6),
        email_address varchar(255),
        event_id varchar(255),
        last_updated_date_time timestamp(6),
        status varchar(255),
        template_values varchar(255),
        primary key (rev, id)
    );

    create table revinfo (
       rev integer not null,
        revtstmp bigint,
        primary key (rev)
    );

    alter table if exists moj_event
       add constraint UK_hrahc5l45uooqxfam2te5tyhs unique (r_event_object_id);

    alter table if exists moj_annotation
       add constraint FKchxdel3cimudqlx1a4bym7otx
       foreign key (moj_cas_id)
       references moj_case;

    alter table if exists moj_cached_media
       add constraint FKml4k9ovrvymhfsiufs2250waf
       foreign key (moj_cas_id)
       references moj_case;

    alter table if exists moj_case
       add constraint FKp70ili3gam1heoxl68lp155ed
       foreign key (moj_crt_id)
       references moj_courthouse;

    alter table if exists moj_case_event_ae
       add constraint FKshy7d3st7ak0nlvo955qb56fh
       foreign key (the_events_moj_eve_id)
       references moj_event;

    alter table if exists moj_case_event_ae
       add constraint FKbwtjhns2168bm3w1vsiw84s8i
       foreign key (case_moj_cas_id)
       references moj_case;

    alter table if exists moj_case_media_ae
       add constraint FKavd6x73r6dnj61j1re8b13sef
       foreign key (the_medias_moj_med_id)
       references moj_media;

    alter table if exists moj_case_media_ae
       add constraint FKdk1khfb83l2cj1ig9tf7n0b59
       foreign key (case_moj_cas_id)
       references moj_case;

    alter table if exists moj_daily_list
       add constraint FKlhi6ifop5b9e2an4a13d9c1u7
       foreign key (moj_crt_id)
       references moj_courthouse;

    alter table if exists moj_hearing
       add constraint FK5de1ayk6j8tob6xa0uxo6qo00
       foreign key (moj_cas_id)
       references moj_case;

    alter table if exists moj_transcription
       add constraint FKis8jwsmw6w57dfjgmky488c93
       foreign key (moj_cas_id)
       references moj_case;

    alter table if exists moj_transcription_comment
       add constraint FKqxql34dfqweow3p9fest8df2y
       foreign key (moj_tra_id)
       references moj_transcription;

    alter table if exists moj_transformation_log
       add constraint FKe5e4b73gntieveiqebofv8rnb
       foreign key (moj_cas_id)
       references moj_case;

    alter table if exists moj_transformation_request
       add constraint FKjqpy3fd8op8csfgxmrcyqpd31
       foreign key (moj_cas_id)
       references moj_case;

    alter table if exists notification_audit
       add constraint FKktuxpn3dgfrn7afnqcu1l3ge7
       foreign key (rev)
       references revinfo;
