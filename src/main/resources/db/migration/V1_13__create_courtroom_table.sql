CREATE TABLE IF NOT EXISTS moj_courtroom
(moj_ctr_id                 INTEGER                  NOT NULL
,moj_cth_id                 INTEGER                  NOT NULL
,courtroom_name             CHARACTER VARYING        NOT NULL
);

ALTER TABLE moj_courtroom                 ADD PRIMARY KEY (moj_ctr_id);
ALTER TABLE moj_courtroom                 ADD CONSTRAINT moj_courtroom_courthouse_fk
FOREIGN KEY (moj_cth_id) REFERENCES moj_courthouse(moj_cth_id);

CREATE SEQUENCE IF NOT EXISTS moj_ctr_seq;
