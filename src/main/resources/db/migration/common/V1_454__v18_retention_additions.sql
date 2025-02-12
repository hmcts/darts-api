CREATE SEQUENCE cof_seq CACHE 20;

ALTER TABLE case_overflow
    ADD COLUMN cof_id INTEGER NOT NULL DEFAULT nextval('cof_seq');

ALTER TABLE case_overflow
    ADD COLUMN case_object_id CHARACTER VARYING;

ALTER TABLE case_overflow
    DROP CONSTRAINT case_overflow_pk;
