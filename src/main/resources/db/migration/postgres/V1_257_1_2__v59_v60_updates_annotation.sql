CREATE UNIQUE INDEX hearing_annotation_ae_pk ON hearing_annotation_ae(hea_id,ann_id);
ALTER TABLE hearing_annotation_ae        ADD PRIMARY KEY USING INDEX hearing_annotation_ae_pk;

