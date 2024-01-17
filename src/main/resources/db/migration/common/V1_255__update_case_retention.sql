ALTER TABLE case_retention DROP COLUMN submitted_ts;
ALTER TABLE case_retention ADD COLUMN submitted_by             INTEGER                      NOT NULL;
ALTER TABLE case_retention ADD COLUMN last_modified_ts         TIMESTAMP WITH TIME ZONE     NOT NULL;
ALTER TABLE case_retention ADD COLUMN last_modified_by         INTEGER                      NOT NULL;

ALTER TABLE case_retention
ADD CONSTRAINT case_retention_created_by_fk
FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

ALTER TABLE case_retention
ADD CONSTRAINT case_retention_last_modified_by_fk
FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);

ALTER TABLE case_retention
ADD CONSTRAINT case_retention_submitted_by_fk
FOREIGN KEY (submitted_by) REFERENCES user_account(usr_id);
