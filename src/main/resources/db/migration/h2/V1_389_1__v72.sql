ALTER TABLE arm_automated_task
    ADD CONSTRAINT arm_automated_task_pk PRIMARY KEY (aat_id);
ALTER TABLE arm_rpo_execution_detail
    ADD CONSTRAINT arm_rpo_execution_detail_pk PRIMARY KEY (ard_id);
ALTER TABLE arm_rpo_state
    ADD CONSTRAINT arm_rpo_state_pk PRIMARY KEY (are_id);
ALTER TABLE arm_rpo_status
    ADD CONSTRAINT arm_rpo_status_pk PRIMARY KEY (aru_id);

ALTER TABLE automated_task
    ADD CONSTRAINT automated_task_pk PRIMARY KEY (aut_id);

ALTER TABLE data_anonymisation
    ADD CONSTRAINT data_anonymisation_pk PRIMARY KEY (dan_id);
