CREATE OR REPLACE FUNCTION tra_trw_sync_fnc()
RETURNS trigger AS
$$
BEGIN
UPDATE transcription SET trs_id = NEW.trs_id WHERE tra_id = NEW.tra_id;
RETURN NEW;
END;
$$
LANGUAGE 'plpgsql';

CREATE OR REPLACE TRIGGER trw_ar_trg
AFTER INSERT ON transcription_workflow
FOR EACH ROW
EXECUTE PROCEDURE tra_trw_sync_fnc();
