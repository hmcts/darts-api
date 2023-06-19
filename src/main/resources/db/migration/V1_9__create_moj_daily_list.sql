CREATE TABLE moj_daily_list
(moj_dal_id                 INTEGER					 NOT NULL
,moj_cth_id                 INTEGER					 NOT NULL
,r_daily_list_object_id     text
,c_unique_id                text
--,c_crown_court_name       text        -- removed, normalised to courthouses, but note that in legacy there is mismatch between moj_courthouse_s.c_id and moj_daily_list_s.c_crown_court_name to be resolved
,c_job_status               text        -- one of "New","Partially Processed","Processed","Ignored","Invalid"
,c_published_time           TIMESTAMP WITH TIME ZONE
,c_daily_list_id            NUMERIC                  -- all 0
,c_start_ts                 TIMESTAMP WITH TIME ZONE
,c_end_ts                   TIMESTAMP WITH TIME ZONE -- all values match c_start_date
,c_daily_list_id_s          text        -- non unique integer in legacy
,c_daily_list_source        text        -- one of CPP,XHB ( live also sees nulls and spaces)
,daily_list_content         text
,created_ts                 TIMESTAMP WITH TIME ZONE
,last_modified_ts           TIMESTAMP WITH TIME ZONE
,r_version_label            text(32)
,i_superseded               BOOLEAN
,i_version                  SMALLINT);
