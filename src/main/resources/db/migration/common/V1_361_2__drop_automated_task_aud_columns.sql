ALTER TABLE automated_task_aud DROP COLUMN IF EXISTS created_ts;
ALTER TABLE automated_task_aud DROP COLUMN IF EXISTS created_by;
ALTER TABLE automated_task_aud DROP COLUMN IF EXISTS last_modified_ts;
ALTER TABLE automated_task_aud DROP COLUMN IF EXISTS last_modified_by;