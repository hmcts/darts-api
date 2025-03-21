CREATE UNIQUE INDEX case_overflow_pk ON case_overflow(cof_id);
ALTER TABLE case_overflow ADD PRIMARY KEY USING INDEX case_overflow_pk;
