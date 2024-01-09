ALTER TABLE daily_list ADD COLUMN listing_courthouse CHARACTER VARYING;

UPDATE daily_list dl
SET listing_courthouse = (
  SELECT ch.courthouse_name
  FROM courthouse ch
  WHERE dl.cth_id = ch.cth_id
);

ALTER TABLE daily_list ALTER COLUMN listing_courthouse SET NOT NULL;

ALTER TABLE daily_list DROP COLUMN cth_id;
