CREATE TABLE moj_hearing
(moj_hea_id                 INTEGER					   NOT NULL
,moj_cas_id                 INTEGER                    NOT NULL
,moj_ctr_id                 INTEGER                    NOT NULL
,c_judge                    CHARACTER VARYING[]
,c_hearing_date             DATE     -- to record only DATE component of hearings, both scheduled and actual
,c_scheduled_start_time     TIME     -- to record only TIME component of hearings, while they are scheduled only
,hearing_is_actual          BOOLEAN  -- TRUE for actual hearings, FALSE for scheduled hearings
,c_judge_hearing_date       CHARACTER VARYING);

ALTER TABLE moj_hearing                   ADD PRIMARY KEY (moj_hea_id);
ALTER TABLE moj_hearing                   ADD CONSTRAINT moj_hearing_case_fk
FOREIGN KEY (moj_cas_id) REFERENCES moj_case(moj_cas_id);
ALTER TABLE moj_hearing                   ADD CONSTRAINT moj_hearing_courtroom_fk
FOREIGN KEY (moj_ctr_id) REFERENCES moj_courtroom(moj_ctr_id);

CREATE SEQUENCE IF NOT EXISTS moj_hea_seq;
