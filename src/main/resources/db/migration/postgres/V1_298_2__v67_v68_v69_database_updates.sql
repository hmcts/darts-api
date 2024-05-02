CREATE INDEX usr_upea_idx       ON USER_ACCOUNT(UPPER(user_email_address));
CREATE INDEX usr_ag_idx         ON USER_ACCOUNT(account_guid)             ;

CREATE UNIQUE INDEX event_linked_case_pk ON event_linked_case(elc_id);
ALTER TABLE event_linked_case  ADD PRIMARY KEY USING INDEX event_linked_case_pk;

CREATE UNIQUE INDEX media_linked_case_pk ON media_linked_case(mlc_id);
ALTER TABLE media_linked_case  ADD PRIMARY KEY USING INDEX media_linked_case_pk;

CREATE UNIQUE INDEX object_admin_action_pk ON object_admin_action(oaa_id);
ALTER TABLE object_admin_action ADD PRIMARY KEY USING INDEX object_admin_action_pk;

CREATE UNIQUE INDEX object_retrieval_queue_pk ON object_retrieval_queue(orq_id);
ALTER TABLE object_retrieval_queue ADD PRIMARY KEY USING INDEX object_retrieval_queue_pk;