CREATE TABLE case_linked_case
(
    clc_id           INTEGER                  NOT NULL,
    case_1_id        INTEGER                  NOT NULL,
    case_2_id        INTEGER                  NOT NULL,
    created_ts       TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by       INTEGER                  NOT NULL,
    last_modified_ts TIMESTAMP WITH TIME ZONE NOT NULL,
    last_modified_by INTEGER                  NOT NULL
);

ALTER TABLE case_linked_case
    ADD CONSTRAINT case_linked_case_pk PRIMARY KEY (clc_id);

ALTER TABLE case_linked_case
    ADD CONSTRAINT case_linked_case_case_1_fk
        FOREIGN KEY (case_1_id) REFERENCES court_case (cas_id);

ALTER TABLE case_linked_case
    ADD CONSTRAINT case_linked_case_case_2_fk
        FOREIGN KEY (case_2_id) REFERENCES court_case (cas_id);

ALTER TABLE case_linked_case
    ADD CONSTRAINT case_linked_case_created_by_fk
        FOREIGN KEY (created_by) REFERENCES user_account (usr_id);

ALTER TABLE case_linked_case
    ADD CONSTRAINT case_linked_case_last_modified_by_fk
        FOREIGN KEY (last_modified_by) REFERENCES user_account (usr_id);

CREATE SEQUENCE clc_seq CACHE 1;

CREATE INDEX clc_case_1_fk ON case_linked_case (case_1_id);
CREATE INDEX clc_case_2_fk ON case_linked_case (case_2_id);
CREATE INDEX clc_cre_by_fk ON case_linked_case (created_by);
CREATE INDEX clc_lst_mod_by_fk ON case_linked_case (last_modified_by);
