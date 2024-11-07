CREATE UNIQUE INDEX arm_automated_task_pk ON arm_automated_task (aat_id);
ALTER TABLE arm_automated_task
    ADD PRIMARY KEY USING INDEX arm_automated_task_pk;

CREATE UNIQUE INDEX arm_rpo_execution_detail_pk ON arm_rpo_execution_detail (ard_id);
ALTER TABLE arm_rpo_execution_detail
    ADD PRIMARY KEY USING INDEX arm_rpo_execution_detail_pk;

CREATE UNIQUE INDEX arm_rpo_state_pk ON arm_rpo_state (are_id);
ALTER TABLE arm_rpo_state
    ADD PRIMARY KEY USING INDEX arm_rpo_state_pk;

CREATE UNIQUE INDEX arm_rpo_status_pk ON arm_rpo_status (aru_id);
ALTER TABLE arm_rpo_status
    ADD PRIMARY KEY USING INDEX arm_rpo_status_pk;

CREATE UNIQUE INDEX data_anonymisation_pk ON data_anonymisation(dan_id);
ALTER TABLE data_anonymisation
    ADD PRIMARY KEY USING INDEX data_anonymisation_pk;
