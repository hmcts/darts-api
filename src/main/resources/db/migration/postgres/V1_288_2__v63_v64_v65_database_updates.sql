DO
$do$
DECLARE rec RECORD;
BEGIN
FOR rec IN select cas_id, tra_id, count(*) thecount from case_transcription_ae
group by cas_id, tra_id
having count(*) >1
LOOP
delete from case_transcription_ae where cas_id = rec.cas_id and tra_id = rec.tra_id;
INSERT INTO darts.case_transcription_ae(cas_id, tra_id)	VALUES (rec.cas_id, rec.tra_id);
END LOOP;
END;
$do$;

CREATE UNIQUE INDEX case_transcription_ae_pk ON case_transcription_ae(cas_id,tra_id);
ALTER TABLE case_transcription_ae        ADD PRIMARY KEY USING INDEX case_transcription_ae_pk;




DO
$do$
DECLARE rec RECORD;
BEGIN
FOR rec IN select hea_id, tra_id, count(*) thecount from hearing_transcription_ae
group by hea_id, tra_id
having count(*) >1
LOOP
delete from hearing_transcription_ae where hea_id = rec.hea_id and tra_id = rec.tra_id;
INSERT INTO darts.hearing_transcription_ae(hea_id, tra_id)	VALUES (rec.hea_id, rec.tra_id);
END LOOP;
END;
$do$;

CREATE UNIQUE INDEX hearing_transcription_ae_pk ON hearing_transcription_ae(hea_id,tra_id);
ALTER TABLE hearing_transcription_ae        ADD PRIMARY KEY USING INDEX hearing_transcription_ae_pk;

CREATE UNIQUE INDEX object_hidden_reason_pk ON object_hidden_reason(ohr_id);
ALTER TABLE object_hidden_reason ADD PRIMARY KEY USING INDEX object_hidden_reason_pk;

