-- V47
ALTER TABLE courthouse_region_ae
  DROP COLUMN cra_id;
CREATE UNIQUE INDEX courthouse_region_ae_pk ON courthouse_region_ae (cth_id, reg_id);


