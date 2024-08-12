update annotation_document theTable set uploaded_by = 0 where uploaded_by is null or not exists (select 1 from user_account ua where ua.usr_id = theTable.uploaded_by);
ALTER TABLE annotation_document ADD CONSTRAINT annotation_document_uploaded_by_fk FOREIGN KEY (uploaded_by) REFERENCES user_account(usr_id);

update annotation_document theTable set deleted_by = 0 where deleted_by is null or not exists (select 1 from user_account ua where ua.usr_id = theTable.deleted_by);
ALTER TABLE annotation_document ADD CONSTRAINT annotation_document_deleted_by_fk FOREIGN KEY (deleted_by) REFERENCES user_account(usr_id);


update case_document theTable set deleted_by = 0 where deleted_by is null or not exists (select 1 from user_account ua where ua.usr_id = theTable.deleted_by);
ALTER TABLE case_document ADD CONSTRAINT case_document_deleted_by_fk FOREIGN KEY (deleted_by) REFERENCES user_account(usr_id);


update court_case theTable set data_anonymised_by = 0 where data_anonymised_by is null or not exists (select 1 from user_account ua where ua.usr_id = theTable.data_anonymised_by);
ALTER TABLE court_case ADD CONSTRAINT court_case_data_anonymised_by_fk FOREIGN KEY (data_anonymised_by) REFERENCES user_account(usr_id);


ALTER TABLE courthouse_region_ae RENAME CONSTRAINT courthouse__region_courthouse_fk TO courthouse_region_courthouse_fk;
ALTER TABLE courthouse_region_ae RENAME CONSTRAINT courthouse__region_region_fk TO courthouse_region_region_fk;


update transcription_document theTable set uploaded_by = 0 where uploaded_by is null or not exists (select 1 from user_account ua where ua.usr_id = theTable.uploaded_by);
ALTER TABLE transcription_document ADD CONSTRAINT transcription_document_uploaded_by_fk FOREIGN KEY (uploaded_by) REFERENCES user_account(usr_id);

update transcription_document theTable set deleted_by = 0 where deleted_by is null or not exists (select 1 from user_account ua where ua.usr_id = theTable.deleted_by);
ALTER TABLE transcription_document ADD CONSTRAINT transcription_document_deleted_by_fk FOREIGN KEY (deleted_by) REFERENCES user_account(usr_id);

