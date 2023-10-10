CREATE OR REPLACE FUNCTION tra_trw_sync_fnc()
RETURNS trigger AS
$$
BEGIN
UPDATE darts.transcription SET trs_id = NEW.trs_id WHERE tra_id = NEW.tra_id;
RETURN NEW;
END;
$$
LANGUAGE 'plpgsql';
