ALTER TABLE event_linked_case ADD CONSTRAINT event_linked_case_pk PRIMARY KEY (elc_id);
ALTER TABLE media_linked_case ADD CONSTRAINT media_linked_case_pk PRIMARY KEY (mlc_id);
ALTER TABLE object_admin_action ADD CONSTRAINT object_admin_action_pk PRIMARY KEY (oaa_id);
ALTER TABLE object_retrieval_queue ADD CONSTRAINT object_retrieval_queue_pk PRIMARY KEY (orq_id);
