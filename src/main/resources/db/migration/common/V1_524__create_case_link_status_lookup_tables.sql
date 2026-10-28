CREATE TABLE case_link_import_status
(
    cis_id       INTEGER               NOT NULL PRIMARY KEY,
    status_type  CHARACTER VARYING(32)  NOT NULL UNIQUE,
    display_name CHARACTER VARYING(64)  NOT NULL
);

INSERT INTO case_link_import_status (cis_id, status_type, display_name)
VALUES (1, 'RECEIVED', 'Received'),
       (2, 'PROCESSING', 'Processing'),
       (3, 'COMPLETED', 'Completed'),
       (4, 'COMPLETED_WITH_ERRORS', 'Completed with errors'),
       (5, 'REJECTED', 'Rejected'),
       (6, 'FAILED', 'Failed');

CREATE TABLE case_link_import_result_status
(
    crs_id       INTEGER               NOT NULL PRIMARY KEY,
    status_type  CHARACTER VARYING(16)  NOT NULL UNIQUE,
    display_name CHARACTER VARYING(64)  NOT NULL
);

INSERT INTO case_link_import_result_status (crs_id, status_type, display_name)
VALUES (1, 'PENDING', 'Pending'),
       (2, 'SUCCESS', 'Successful'),
       (3, 'FAILED', 'Failed');

ALTER TABLE case_link_import ADD COLUMN cis_id INTEGER;
ALTER TABLE case_link_import_result ADD COLUMN crs_id INTEGER;

UPDATE case_link_import
SET cis_id = (SELECT cis_id FROM case_link_import_status WHERE status_type = case_link_import.status);

UPDATE case_link_import_result
SET crs_id = (SELECT crs_id FROM case_link_import_result_status WHERE status_type = case_link_import_result.status);

ALTER TABLE case_link_import ALTER COLUMN cis_id SET NOT NULL;
ALTER TABLE case_link_import_result ALTER COLUMN crs_id SET NOT NULL;

ALTER TABLE case_link_import
    ADD CONSTRAINT cli_status_fk FOREIGN KEY (cis_id) REFERENCES case_link_import_status (cis_id);
ALTER TABLE case_link_import_result
    ADD CONSTRAINT clr_status_fk FOREIGN KEY (crs_id) REFERENCES case_link_import_result_status (crs_id);

ALTER TABLE case_link_import DROP CONSTRAINT cli_status_ck;
ALTER TABLE case_link_import_result DROP CONSTRAINT clr_status_ck;
DROP INDEX clr_import_status_idx;
ALTER TABLE case_link_import DROP COLUMN status;
ALTER TABLE case_link_import_result DROP COLUMN status;

CREATE INDEX cli_status_fk ON case_link_import (cis_id);
CREATE INDEX clr_status_fk ON case_link_import_result (crs_id);
CREATE INDEX clr_import_status_idx ON case_link_import_result (cli_id, crs_id, row_number);
