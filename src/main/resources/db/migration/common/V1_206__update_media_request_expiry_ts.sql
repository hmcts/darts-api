ALTER TABLE media_request ADD expiry_ts TIMESTAMP WITH TIME ZONE;
ALTER TABLE media_request ALTER COLUMN request_status SET NOT NULL;
ALTER TABLE media_request ALTER COLUMN request_type SET NOT NULL;
ALTER TABLE media_request ALTER COLUMN start_ts SET NOT NULL;
ALTER TABLE media_request ALTER COLUMN end_ts SET NOT NULL;
ALTER TABLE media_request ALTER COLUMN created_ts SET NOT NULL;
ALTER TABLE media_request ALTER COLUMN created_by SET NOT NULL;
ALTER TABLE media_request ALTER COLUMN last_modified_ts SET NOT NULL;
ALTER TABLE media_request ALTER COLUMN last_modified_by SET NOT NULL;

ALTER TABLE media_request DROP CONSTRAINT media_hearing_fk;

ALTER TABLE media_request
ADD CONSTRAINT media_request_hearing_fk
FOREIGN KEY (hea_id) REFERENCES hearing(hea_id);
