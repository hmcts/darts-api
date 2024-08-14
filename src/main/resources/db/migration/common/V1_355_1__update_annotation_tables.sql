update annotation theTable set created_by = 0 where created_by is null or not exists (select 1 from user_account ua where ua.usr_id = theTable.created_by);
ALTER TABLE annotation ADD CONSTRAINT annotation_created_by_fk FOREIGN KEY (created_by) REFERENCES user_account(usr_id);
ALTER TABLE annotation ALTER COLUMN created_by SET NOT NULL;

update annotation theTable set last_modified_by = 0 where last_modified_by is null or not exists (select 1 from user_account ua where ua.usr_id = theTable.last_modified_by);
ALTER TABLE annotation ADD CONSTRAINT annotation_modified_by_fk FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);
ALTER TABLE annotation ALTER COLUMN last_modified_by SET NOT NULL;

update annotation_document theTable set last_modified_by = 0 where last_modified_by is null or not exists (select 1 from user_account ua where ua.usr_id = theTable.last_modified_by);
ALTER TABLE annotation_document ADD CONSTRAINT annotation_document_last_modified_by_fk FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);
