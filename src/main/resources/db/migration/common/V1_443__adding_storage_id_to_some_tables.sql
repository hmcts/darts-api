alter table media
    add column storage_id varchar;
alter table transcription_document
    add column storage_id varchar;
alter table daily_list
    add column storage_id varchar;
alter table annotation_document
    add column storage_id varchar;
alter table object_retrieval_queue
    add column storage_id varchar;