ALTER TABLE IF EXISTS object_state_record ADD COLUMN loaded_to_osr_ts timestamp with time zone NOT NULL DEFAULT now();
ALTER TABLE IF EXISTS object_state_record ADD CONSTRAINT object_state_record_pkey PRIMARY KEY (osr_uuid);
ALTER TABLE IF EXISTS object_state_record ADD CONSTRAINT osr_clip_case_unq UNIQUE (id_clip, id_case);