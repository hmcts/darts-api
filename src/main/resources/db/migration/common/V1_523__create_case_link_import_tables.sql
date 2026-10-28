CREATE SEQUENCE cli_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE clr_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE case_link_import
(
    cli_id                  BIGINT                   NOT NULL PRIMARY KEY,
    original_filename       TEXT,
    file_checksum           CHARACTER VARYING(64),
    status                  CHARACTER VARYING(32)    NOT NULL,
    processing_started_ts   TIMESTAMP WITH TIME ZONE,
    processing_completed_ts TIMESTAMP WITH TIME ZONE,
    error_code              CHARACTER VARYING(255),
    error_message           TEXT,
    created_ts              TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by              INTEGER                  NOT NULL REFERENCES user_account (usr_id),
    last_modified_ts        TIMESTAMP WITH TIME ZONE NOT NULL,
    last_modified_by        INTEGER                  NOT NULL REFERENCES user_account (usr_id),
    CONSTRAINT cli_status_ck CHECK (status IN ('RECEIVED', 'PROCESSING', 'COMPLETED', 'COMPLETED_WITH_ERRORS', 'REJECTED', 'FAILED'))
);

CREATE TABLE case_link_import_result
(
    clr_id             BIGINT                   NOT NULL PRIMARY KEY,
    cli_id             BIGINT                   NOT NULL REFERENCES case_link_import (cli_id),
    row_number         INTEGER                  NOT NULL,
    case_1_case_number TEXT,
    case_1_courthouse  TEXT,
    case_2_case_number TEXT,
    column_count       INTEGER                  NOT NULL,
    case_1_id          INTEGER REFERENCES court_case (cas_id) ON DELETE SET NULL,
    case_2_id          INTEGER REFERENCES court_case (cas_id) ON DELETE SET NULL,
    case_1_created     BOOLEAN                  NOT NULL DEFAULT FALSE,
    case_2_created     BOOLEAN                  NOT NULL DEFAULT FALSE,
    case_1_deleted     BOOLEAN                  NOT NULL DEFAULT FALSE,
    case_2_deleted     BOOLEAN                  NOT NULL DEFAULT FALSE,
    case_1_anonymised  BOOLEAN                  NOT NULL DEFAULT FALSE,
    case_2_anonymised  BOOLEAN                  NOT NULL DEFAULT FALSE,
    clc_id             INTEGER REFERENCES case_linked_case (clc_id) ON DELETE SET NULL,
    status             CHARACTER VARYING(16)    NOT NULL,
    error_code         CHARACTER VARYING(255),
    error_message      TEXT,
    warning_codes      TEXT,
    created_ts         TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by         INTEGER                  NOT NULL REFERENCES user_account (usr_id),
    last_modified_ts   TIMESTAMP WITH TIME ZONE NOT NULL,
    last_modified_by   INTEGER                  NOT NULL REFERENCES user_account (usr_id),
    CONSTRAINT clr_import_row_unq UNIQUE (cli_id, row_number),
    CONSTRAINT clr_row_number_ck CHECK (row_number >= 2),
    CONSTRAINT clr_status_ck CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED'))
);

CREATE INDEX cli_cre_by_fk ON case_link_import (created_by);
CREATE INDEX cli_mod_by_fk ON case_link_import (last_modified_by);
CREATE INDEX clr_import_status_idx ON case_link_import_result (cli_id, status, row_number);
CREATE INDEX clr_case_1_fk ON case_link_import_result (case_1_id);
CREATE INDEX clr_case_2_fk ON case_link_import_result (case_2_id);
CREATE INDEX clr_clc_fk ON case_link_import_result (clc_id);
CREATE INDEX clr_cre_by_fk ON case_link_import_result (created_by);
CREATE INDEX clr_mod_by_fk ON case_link_import_result (last_modified_by);
