DELETE
FROM retention_confidence_category_mapper
where confidence_category between 11 and 99;

UPDATE retention_confidence_category_mapper
SET ret_conf_score = 0
WHERE confidence_category in (4, 9, 10);
