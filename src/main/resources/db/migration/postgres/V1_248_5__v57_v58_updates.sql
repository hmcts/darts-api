CREATE UNIQUE INDEX transformed_media_pk ON transformed_media (trm_id);
ALTER TABLE transformed_media
  ADD PRIMARY KEY USING INDEX transformed_media_pk;

SELECT setval('ors_seq', COALESCE((SELECT MAX(ors_id) + 1 FROM object_record_status), 1), false);



