CREATE UNIQUE INDEX retention_confidence_category_mapper_pk ON retention_confidence_category_mapper (rcc_id) TABLESPACE pg_default;
ALTER TABLE retention_confidence_category_mapper
    ADD PRIMARY KEY USING INDEX retention_confidence_category_mapper_pk;
