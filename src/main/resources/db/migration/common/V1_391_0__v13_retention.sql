-- v13 add table retention_confidence_category_mapper
CREATE TABLE retention_confidence_category_mapper
(
    rcc_id              INTEGER                  NOT NULL,
    ret_conf_score      INTEGER,
    ret_conf_reason     CHARACTER VARYING,
    confidence_category INTEGER, -- effectively a master list of valid categories found on case_retention
    description         CHARACTER VARYING,
    created_ts          TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by          INTEGER                  NOT NULL,
    last_modified_ts    TIMESTAMP WITH TIME ZONE NOT NULL,
    last_modified_by    INTEGER                  NOT NULL
);

CREATE SEQUENCE rcc_seq CACHE 20;
