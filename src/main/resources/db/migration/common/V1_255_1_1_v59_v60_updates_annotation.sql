ALTER TABLE annotation ADD COLUMN is_deleted BOOLEAN NOT NULL default false;

CREATE TABLE hearing_annotation_ae
(hea_id                      INTEGER                       NOT NULL
,ann_id                      INTEGER                       NOT NULL
);

ALTER TABLE annotation DROP CONSTRAINT annotation_case_fk;
ALTER TABLE annotation DROP CONSTRAINT annotation_courtroom_fk;
ALTER TABLE annotation DROP CONSTRAINT annotation_hearing_fk;
ALTER TABLE daily_list DROP CONSTRAINT daily_list_courthouse_fk;

INSERT INTO darts.hearing_annotation_ae(
	   hea_id, ann_id)
	(select hea_id, ann_id from darts.annotation);

ALTER TABLE annotation DROP COLUMN cas_id;
ALTER TABLE annotation DROP COLUMN ctr_id;
ALTER TABLE annotation DROP COLUMN hea_id;

ALTER TABLE hearing_annotation_ae
ADD CONSTRAINT hearing_annotation_ae_hearing_fk
FOREIGN KEY (hea_id) REFERENCES hearing(hea_id);

ALTER TABLE hearing_annotation_ae
ADD CONSTRAINT hearing_annotation_ae_annotation_fk
FOREIGN KEY (ann_id) REFERENCES annotation(ann_id);
