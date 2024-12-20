UPDATE annotation_document SET ret_conf_score=0 WHERE ret_conf_score=1;
UPDATE annotation_document SET ret_conf_score=1 WHERE ret_conf_score=2;

UPDATE case_document SET ret_conf_score=0 WHERE ret_conf_score=1;
UPDATE case_document SET ret_conf_score=1 WHERE ret_conf_score=2;

UPDATE court_case SET ret_conf_score=0 WHERE ret_conf_score=1;
UPDATE court_case SET ret_conf_score=1 WHERE ret_conf_score=2;

UPDATE media SET ret_conf_score=0 WHERE ret_conf_score=1;
UPDATE media SET ret_conf_score=1 WHERE ret_conf_score=2;

UPDATE retention_confidence_category_mapper SET ret_conf_score=0 WHERE ret_conf_score=1;
UPDATE retention_confidence_category_mapper SET ret_conf_score=1 WHERE ret_conf_score=2;

UPDATE transcription_document SET ret_conf_score=0 WHERE ret_conf_score=1;
UPDATE transcription_document SET ret_conf_score=1 WHERE ret_conf_score=2;
