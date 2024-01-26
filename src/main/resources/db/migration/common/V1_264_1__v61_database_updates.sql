ALTER TABLE audit ALTER COLUMN cas_id DROP NOT NULL;
ALTER TABLE audit DROP COLUMN application_server;

update event set evh_id=1 WHERE evh_id is null;
ALTER TABLE event ALTER COLUMN evh_id SET NOT NULL;


CREATE TABLE case_transcription_ae
(cas_id                      INTEGER                       NOT NULL
,tra_id                      INTEGER                       NOT NULL
);


CREATE TABLE hearing_transcription_ae
(hea_id                      INTEGER                       NOT NULL
,tra_id                      INTEGER                       NOT NULL
);

INSERT INTO case_transcription_ae (cas_id,tra_id) (select cas_id,tra_id from transcription where hea_id is null);
INSERT INTO hearing_transcription_ae (hea_id,tra_id) (select hea_id,tra_id from transcription where hea_id is not null);

ALTER TABLE transcription DROP CONSTRAINT transcription_case_fk;
ALTER TABLE transcription DROP CONSTRAINT transcription_hearing_fk;
ALTER TABLE transcription DROP CONSTRAINT unique_transcription;


ALTER TABLE transcription DROP COLUMN cas_id;
ALTER TABLE transcription DROP COLUMN hea_id;


ALTER TABLE case_transcription_ae
ADD CONSTRAINT case_transcription_ae_case_fk
FOREIGN KEY (cas_id) REFERENCES court_case(cas_id);

ALTER TABLE case_transcription_ae
ADD CONSTRAINT case_transcription_ae_transcription_fk
FOREIGN KEY (tra_id) REFERENCES transcription(tra_id);


ALTER TABLE hearing_transcription_ae
ADD CONSTRAINT hearing_transcription_ae_hearing_fk
FOREIGN KEY (hea_id) REFERENCES hearing(hea_id);

ALTER TABLE hearing_transcription_ae
ADD CONSTRAINT hearing_transcription_ae_transcription_fk
FOREIGN KEY (tra_id) REFERENCES transcription(tra_id);


CREATE VIEW user_roles_courthouses AS
SELECT cth.cth_id,
       usr.usr_id,
       grp.rol_id
FROM darts.user_account usr,
     darts.security_group_user_account_ae sgua,
     darts.security_group grp,
     darts.security_group_courthouse_ae sgrc,
     darts.courthouse cth
WHERE usr.usr_id = sgua.usr_id
  AND sgua.grp_id = grp.grp_id
  AND ((grp.global_access=FALSE
        AND grp.grp_id = sgrc.grp_id
        AND sgrc.cth_id = cth.cth_id)
       OR grp.global_access=TRUE
	   --deliberately not joining courthouse
 )
GROUP BY cth.cth_id,
         usr.usr_id,
         grp.rol_id
ORDER BY cth.cth_id,
         usr.usr_id,
         grp.rol_id;



ALTER TABLE security_group ADD COLUMN created_ts TIMESTAMP WITH TIME ZONE;
ALTER TABLE security_group ADD COLUMN created_by INTEGER;
ALTER TABLE security_group ADD COLUMN last_modified_ts TIMESTAMP WITH TIME ZONE;
ALTER TABLE security_group ADD COLUMN last_modified_by INTEGER;
