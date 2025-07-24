update retention_confidence_category_mapper
set ret_conf_score = 0
where confidence_category in (3, 32, 33, 34, 12, 13, 14, 62, 63, 64, 5, 6, 7, 8);

update darts.retention_confidence_category_mapper
set ret_conf_score = 1
where confidence_category in (1, 2, 21, 22, 23, 42, 43, 31, 11, 61, 4);