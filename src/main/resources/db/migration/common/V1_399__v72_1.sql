--v72.1 add 2 missing FKs on data_anonymisation
--    add missing FK on daily_list
ALTER TABLE daily_list
    ADD CONSTRAINT daily_list_external_location_type_fk
        FOREIGN KEY (elt_id) REFERENCES external_location_type (elt_id);


--    add 2 FKs on event_linked_case
DELETE
FROM event_linked_case -- delete records that conflict with event_linked_case_court_case_fk
WHERE cas_id IN (SELECT elc.cas_id
                 FROM event_linked_case elc
                          LEFT JOIN court_case cc ON elc.cas_id = cc.cas_id
                 WHERE cc.cas_id IS NULL);

ALTER TABLE event_linked_case
    ADD CONSTRAINT event_linked_case_court_case_fk
        FOREIGN KEY (cas_id) REFERENCES court_case (cas_id);

DELETE
FROM event_linked_case -- delete records that conflict with event_linked_case_event_fk
WHERE eve_id IN (SELECT elc.eve_id
                 FROM event_linked_case elc
                          LEFT JOIN event eve ON elc.eve_id = eve.eve_id
                 WHERE eve.eve_id IS NULL);

ALTER TABLE event_linked_case
    ADD CONSTRAINT event_linked_case_event_fk
        FOREIGN KEY (eve_id) REFERENCES event (eve_id);


--    add 2 FKs on media_linked_case
ALTER TABLE media_linked_case
    ADD CONSTRAINT media_linked_case_court_case_fk
        FOREIGN KEY (cas_id) REFERENCES court_case (cas_id);

ALTER TABLE media_linked_case
    ADD CONSTRAINT media_linked_case_media_fk
        FOREIGN KEY (med_id) REFERENCES media (med_id);


--    add 2 FKs on object_retrieval_queue
ALTER TABLE object_retrieval_queue
    ADD CONSTRAINT object_retrieval_queue_media_fk
        FOREIGN KEY (med_id) REFERENCES media (med_id);

ALTER TABLE object_retrieval_queue
    ADD CONSTRAINT object_retrieval_queue_transcription_document_fk
        FOREIGN KEY (trd_id) REFERENCES transcription_document (trd_id);


-- Additional changes not captured in change notes
ALTER TABLE data_anonymisation
    ADD CONSTRAINT data_anonymisation_requested_by_fk
        FOREIGN KEY (requested_by) REFERENCES user_account (usr_id);

ALTER TABLE data_anonymisation
    ADD CONSTRAINT data_anonymisation_approved_by_fk
        FOREIGN KEY (approved_by) REFERENCES user_account (usr_id);
