CREATE UNIQUE INDEX case_document_pk ON case_document (cad_id);
ALTER TABLE case_document
  ADD PRIMARY KEY USING INDEX case_document_pk;

CREATE UNIQUE INDEX case_overflow_pk ON case_overflow (cas_id);
ALTER TABLE case_overflow
  ADD PRIMARY KEY USING INDEX case_overflow_pk;
