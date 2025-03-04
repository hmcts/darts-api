-- Had issues with dropping the existing PK on cas_id, so as a workaround, drop the whole column and recreate it
-- with the required FK constraint.
ALTER TABLE case_overflow
    DROP COLUMN cas_id;
ALTER TABLE case_overflow
    ADD COLUMN cas_id INTEGER;
ALTER TABLE case_overflow
    ADD CONSTRAINT case_overflow_court_case_fk
        FOREIGN KEY (cas_id) REFERENCES court_case(cas_id);

ALTER TABLE case_overflow
    ADD CONSTRAINT case_overflow_pk PRIMARY KEY (cof_id);
