CREATE TABLE IF NOT EXISTS hearing_media_ae (
  hma_id                integer                     NOT NULL
, hea_id                integer                     NOT NULL DEFAULT 0
, med_id                integer                     NOT NULL
, CONSTRAINT hearing_media_ae_pkey PRIMARY KEY (hma_id)
, CONSTRAINT media_fk FOREIGN KEY (med_id) REFERENCES media (med_id)
)
