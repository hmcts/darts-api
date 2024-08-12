CREATE UNIQUE INDEX Courthouse_courthouse_name_key ON courthouse(courthouse_name);

update courthouse set courthouse_name = upper(courthouse_name);
ALTER TABLE courthouse ADD CONSTRAINT courthouse_name_ck CHECK (courthouse_name = UPPER(courthouse_name));

update courtroom set courtroom_name = upper(courtroom_name);
ALTER TABLE courtroom ADD CONSTRAINT courtroom_name_ck CHECK (courtroom_name = UPPER(courtroom_name));
