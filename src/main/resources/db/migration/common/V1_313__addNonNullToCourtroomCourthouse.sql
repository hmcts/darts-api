update courtroom
set created_by = 0
where created_by is null;
ALTER TABLE courtroom ALTER COLUMN created_by SET NOT NULL;

update courthouse
set created_by = 0
where created_by is null;
ALTER TABLE courthouse ALTER COLUMN created_by SET NOT NULL;

update courthouse
set last_modified_by = 0
where last_modified_by is null;
ALTER TABLE courthouse ALTER COLUMN last_modified_by SET NOT NULL;
