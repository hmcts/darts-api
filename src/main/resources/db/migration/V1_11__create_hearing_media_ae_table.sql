CREATE TABLE IF NOT EXISTS moj_hearing_media_ae (
  moj_hma_id                integer                     NOT NULL
, moj_hea_id                integer                     NOT NULL DEFAULT 0
, moj_med_id                integer                     NOT NULL
, CONSTRAINT moj_hearing_media_ae_pkey PRIMARY KEY (moj_hma_id)
, CONSTRAINT moj_media_fk FOREIGN KEY (moj_med_id) REFERENCES moj_media (moj_med_id)
)
