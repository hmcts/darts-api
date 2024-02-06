ALTER TABLE transcription
  ADD COLUMN hearing_date_tmp DATE;
UPDATE transcription
SET hearing_date_tmp = CAST(hearing_date AS DATE);
ALTER TABLE transcription
  DROP COLUMN hearing_date;
ALTER TABLE transcription
  RENAME COLUMN hearing_date_tmp TO hearing_date;
ALTER TABLE transcription
  ADD COLUMN is_manual boolean NOT NULL DEFAULT FALSE;

UPDATE transcription
set is_manual= true
WHERE trt_id = 999;

CREATE TABLE IF NOT EXISTS media_type
(
  met_id     INTEGER           NOT null,
  media_type CHARACTER VARYING NOT null,
  CONSTRAINT media_type_pk PRIMARY KEY (met_id)
);

INSERT INTO media_type
VALUES (1, 'AUDIO');

ALTER TABLE media
  ADD COLUMN media_file CHARACTER VARYING;
UPDATE media
SET media_file='a media file';
ALTER TABLE media
  ALTER COLUMN media_file SET NOT NULL;

ALTER TABLE media
  ADD COLUMN media_format CHARACTER VARYING;
UPDATE media
SET media_format='media format';
ALTER TABLE media
  ALTER COLUMN media_format SET NOT NULL;

ALTER TABLE media
  ADD COLUMN file_size INTEGER;
UPDATE media
SET file_size=1000;
ALTER TABLE media
  ALTER COLUMN file_size SET NOT NULL;

ALTER TABLE media
  ADD COLUMN checksum CHARACTER VARYING;
UPDATE media
SET checksum='achecksum';

ALTER TABLE media
  ADD COLUMN met_id INTEGER DEFAULT 1;
ALTER TABLE media
  ADD CONSTRAINT media_media_type_fk
    FOREIGN KEY (met_id) REFERENCES media_type (met_id);
ALTER TABLE courthouse
  ADD COLUMN display_name CHARACTER VARYING;
UPDATE courthouse
SET display_name=courthouse_name;
ALTER TABLE courthouse
  ALTER COLUMN display_name SET NOT NULL;
