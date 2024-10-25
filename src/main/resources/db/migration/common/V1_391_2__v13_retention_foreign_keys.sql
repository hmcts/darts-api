ALTER TABLE retention_confidence_category_mapper
    ADD CONSTRAINT retention_confidence_category_mapper_created_by_fk
        FOREIGN KEY (created_by) REFERENCES user_account(usr_id);

ALTER TABLE retention_confidence_category_mapper
    ADD CONSTRAINT retention_confidence_category_mapper_last_modified_by_fk
        FOREIGN KEY (last_modified_by) REFERENCES user_account(usr_id);
