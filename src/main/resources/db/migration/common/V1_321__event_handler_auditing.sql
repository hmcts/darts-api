create table event_handler_aud
(
    evh_id                       INTEGER					 NOT NULL
    ,event_type                  CHARACTER VARYING           NOT NULL
    ,event_sub_type              CHARACTER VARYING
    ,event_name                  CHARACTER VARYING           NOT NULL
    ,handler                     CHARACTER VARYING
    ,active                      BOOLEAN                     NOT NULL
    ,rev                         INTEGER                     NOT NULL
    ,is_reporting_restriction boolean NOT NULL DEFAULT FALSE
    ,revtype smallint
    ,primary key (rev, evh_id)
);