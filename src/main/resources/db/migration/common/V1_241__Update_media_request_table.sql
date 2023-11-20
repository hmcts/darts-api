ALTER TABLE media_request ADD COLUMN current_owner INTEGER;
UPDATE media_request SET current_owner=requestor;
ALTER TABLE media_request ALTER COLUMN current_owner SET NOT NULL;

ALTER TABLE media_request
ADD CONSTRAINT media_request_created_by_fk
FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

ALTER TABLE media_request
ADD CONSTRAINT media_request_requestor_fk
FOREIGN KEY (requestor) REFERENCES user_account(usr_id);

ALTER TABLE media_request
ADD CONSTRAINT media_request_modified_by_fk
FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);

ALTER TABLE media_request
ADD CONSTRAINT media_request_current_owner_fk
FOREIGN KEY (current_owner) REFERENCES user_account(usr_id);
