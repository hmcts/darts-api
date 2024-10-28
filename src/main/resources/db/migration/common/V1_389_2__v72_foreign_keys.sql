ALTER TABLE arm_automated_task
    ADD CONSTRAINT arm_automated_task_automated_task_fk
        FOREIGN KEY (aut_id) REFERENCES automated_task(aut_id);

ALTER TABLE arm_rpo_execution_detail
    ADD CONSTRAINT arm_rpo_execution_detail_arm_state_fk
        FOREIGN KEY (are_id) REFERENCES arm_rpo_state(are_id);

ALTER TABLE arm_rpo_execution_detail
    ADD CONSTRAINT arm_rpo_execution_detail_arm_status_fk
        FOREIGN KEY (aru_id) REFERENCES arm_rpo_status(aru_id);

ALTER TABLE arm_rpo_execution_detail
    ADD CONSTRAINT arm_rpo_execution_detail_created_by_fk
        FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

ALTER TABLE arm_rpo_execution_detail
    ADD CONSTRAINT arm_rpo_execution_detail_last_modified_by_fk
        FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);

ALTER TABLE media_linked_case
    ADD CONSTRAINT media_linked_case_created_by_fk
        FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

ALTER TABLE data_anonymisation
    ADD CONSTRAINT data_anonymisation_event_fk
        FOREIGN KEY (eve_id) REFERENCES event(eve_id);

ALTER TABLE data_anonymisation
    ADD CONSTRAINT data_anonymisation_transcription_comment_fk
        FOREIGN KEY (trc_id) REFERENCES transcription_comment(trc_id);
