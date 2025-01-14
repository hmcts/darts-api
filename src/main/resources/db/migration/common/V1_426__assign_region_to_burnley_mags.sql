INSERT INTO courthouse_region_ae (cth_id, reg_id)
VALUES ((SELECT cth_id FROM courthouse WHERE courthouse_name = 'BURNLEY MAGS TWO'),
        (SELECT reg_id FROM region WHERE region_name = 'North West'));
