CREATE TABLE moj_reporting_restrictions
(moj_rer_id                 INTEGER                  NOT null
,rer_description            CHARACTER VARYING);



ALTER TABLE moj_reporting_restrictions    ADD PRIMARY KEY (moj_rer_id);

CREATE SEQUENCE moj_rer_seq CACHE 20;
