--1
-- pre-check list to be actioned ( expect 41)
select a.tra_id
from transcription a
   , transcription b 
where a.chronicle_id = b.chronicle_id 
and a.tra_id != b.tra_id                                     -- need different tra_id pairs
and a.is_current                                             -- both tra must be current                 
and b.is_current 
and a.created_ts < b.created_ts                              -- just to ensure we dont get 2 rows in the result for each pair
and a.version_label = '1.0'                                  -- we know all candidates are initial
and 0 = (select count(*) 
         from transcription_document 
         where tra_id = a.tra_id)                            -- pseudo left join, checking transcription being updated has no document
order by a.tra_id;

-- fixup  ( expect 41)
update transcription 
set is_current = false
where tra_id in (
    select a.tra_id
    from transcription a
       , transcription b 
    where a.chronicle_id = b.chronicle_id 
    and a.tra_id != b.tra_id                                 -- need different tra_id pairs
    and a.is_current                                         -- both tra must be current                 
    and b.is_current
    and a.created_ts < b.created_ts                          -- just to ensure we dont get 2 rows in the result for each pair
    and a.version_label = '1.0'                              -- we know all candidates are initial
    and 0 = (select count(*) 
             from transcription_document 
             where tra_id = a.tra_id)                        -- pseudo left join, checking transcription being updated has no document
);


-- fixup ( expect 41 )
delete from hearing_transcription_ae 
where tra_id in (
    select a.tra_id
    from transcription a
       , transcription b 
    where a.chronicle_id = b.chronicle_id 
    and a.tra_id != b.tra_id                                 -- need different tra_id pairs
    and a.is_current                                         -- both tra must be current                 
    and b.is_current
    and a.created_ts < b.created_ts                          -- just to ensure we dont get 2 rows in the result for each pair
    and a.version_label = '1.0'                              -- we know all candidates are initial
    and 0 = (select count(*) 
             from transcription_document 
             where tra_id = a.tra_id)                        -- pseudo left join, checking transcription being updated has no document
);

-- post-check list to be actioned ( expect 0)
select a.tra_id
from transcription a
   , transcription b 
where a.chronicle_id = b.chronicle_id 
and a.tra_id != b.tra_id                                     -- need different tra_id pairs
and a.is_current                                             -- both tra must be current                 
and b.is_current 
and a.created_ts < b.created_ts                              -- just to ensure we dont get 2 rows in the result for each pair
and a.version_label = '1.0'                                  -- we know all candidates are initial
and 0 = (select count(*) 
         from transcription_document 
         where tra_id = a.tra_id)                            -- pseudo left join, checking transcription being updated has no document
order by a.tra_id;
