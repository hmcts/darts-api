--3a   to act on records where we have pairs of transcriptions with the same document.
-- pre-check list to be actioned ( expect 7, [42134,43088,43092,43470,48635,48997,58479])
select a.tra_id
from transcription a
   , transcription b 
   , transcription_document tda
   , transcription_document tdb
where a.chronicle_id = b.chronicle_id 
and a.tra_id != b.tra_id                                     -- need different tra_id pairs
and a.is_current                                             -- both tra must be current                 
and b.is_current 
and a.created_ts < b.created_ts                              -- just to ensure we dont get 2 rows in the result for each pair
and a.version_label = '1.0'                                  -- not critical, but we know all candidates are 1.0
and a.tra_id = tda.tra_id                                    -- inner join to its td table
and b.tra_id = tdb.tra_id                                    -- inner join to its td table.
and tda.checksum =tdb.checksum                               -- and their checksums match
order by a.tra_id;


-- fixup  ( expect 7 )
update transcription 
set is_current = false
where tra_id in (
    select a.tra_id
    from transcription a
       , transcription b 
       , transcription_document tda
       , transcription_document tdb
    where a.chronicle_id = b.chronicle_id 
    and a.tra_id != b.tra_id                                     -- need different tra_id pairs
    and a.is_current                                             -- both tra must be current                 
    and b.is_current 
    and a.created_ts < b.created_ts                              -- just to ensure we dont get 2 rows in the result for each pair
    and a.version_label = '1.0'                                  -- not critical, but we know all candidates are 1.0
    and a.tra_id = tda.tra_id                                    -- inner join to its td table
    and b.tra_id = tdb.tra_id                                    -- inner join to its td table.
    and tda.checksum =tdb.checksum                               -- and their checksums match
);

-- fixup ( expect 7 )
delete from hearing_transcription_ae
where tra_id in ( 
    select a.tra_id
    from transcription a
       , transcription b 
       , transcription_document tda
       , transcription_document tdb
    where a.chronicle_id = b.chronicle_id 
    and a.tra_id != b.tra_id                                     -- need different tra_id pairs
    and a.is_current                                             -- both tra must be current                 
    and b.is_current 
    and a.created_ts < b.created_ts                              -- just to ensure we dont get 2 rows in the result for each pair
    and a.version_label = '1.0'                                  -- not critical, but we know all candidates are 1.0
    and a.tra_id = tda.tra_id                                    -- inner join to its td table
    and b.tra_id = tdb.tra_id                                    -- inner join to its td table.
    and tda.checksum =tdb.checksum                               -- and their checksums match
);


-- post-check list to be actioned ( expect 0)
select a.tra_id
from transcription a
   , transcription b 
   , transcription_document tda
   , transcription_document tdb
where a.chronicle_id = b.chronicle_id 
and a.tra_id != b.tra_id                                     -- need different tra_id pairs
and a.is_current                                             -- both tra must be current                 
and b.is_current 
and a.created_ts < b.created_ts                              -- just to ensure we dont get 2 rows in the result for each pair
and a.version_label = '1.0'                                  -- not critical, but we know all candidates are 1.0
and a.tra_id = tda.tra_id                                    -- inner join to its td table
and b.tra_id = tdb.tra_id                                    -- inner join to its td table.
and tda.checksum =tdb.checksum                               -- and their checksums match
order by a.tra_id;

--3a   to act on records where we have pairs of transcriptions with the same document.
-- pre-check list to be actioned ( expect 7, [42134,43088,43092,43470,48635,48997,58479])
select a.tra_id
from transcription a
   , transcription b
   , transcription_document tda
   , transcription_document tdb
where a.chronicle_id = b.chronicle_id
and a.tra_id != b.tra_id                                     -- need different tra_id pairs
and a.is_current                                             -- both tra must be current
and b.is_current
and a.created_ts < b.created_ts                              -- just to ensure we dont get 2 rows in the result for each pair
and a.version_label = '1.0'                                  -- not critical, but we know all candidates are 1.0
and a.tra_id = tda.tra_id                                    -- inner join to its td table
and b.tra_id = tdb.tra_id                                    -- inner join to its td table.
and tda.checksum =tdb.checksum                               -- and their checksums match
order by a.tra_id;


-- fixup  ( expect 7 )
update transcription
set is_current = false
where tra_id in (
    select a.tra_id
    from transcription a
       , transcription b
       , transcription_document tda
       , transcription_document tdb
    where a.chronicle_id = b.chronicle_id
    and a.tra_id != b.tra_id                                     -- need different tra_id pairs
    and a.is_current                                             -- both tra must be current
    and b.is_current
    and a.created_ts < b.created_ts                              -- just to ensure we dont get 2 rows in the result for each pair
    and a.version_label = '1.0'                                  -- not critical, but we know all candidates are 1.0
    and a.tra_id = tda.tra_id                                    -- inner join to its td table
    and b.tra_id = tdb.tra_id                                    -- inner join to its td table.
    and tda.checksum =tdb.checksum                               -- and their checksums match
);

-- fixup ( expect 7 )
delete from hearing_transcription_ae
where tra_id in (
    select a.tra_id
    from transcription a
       , transcription b
       , transcription_document tda
       , transcription_document tdb
    where a.chronicle_id = b.chronicle_id
    and a.tra_id != b.tra_id                                     -- need different tra_id pairs
    and a.is_current                                             -- both tra must be current
    and b.is_current
    and a.created_ts < b.created_ts                              -- just to ensure we dont get 2 rows in the result for each pair
    and a.version_label = '1.0'                                  -- not critical, but we know all candidates are 1.0
    and a.tra_id = tda.tra_id                                    -- inner join to its td table
    and b.tra_id = tdb.tra_id                                    -- inner join to its td table.
    and tda.checksum =tdb.checksum                               -- and their checksums match
);


-- post-check list to be actioned ( expect 0)
select a.tra_id
from transcription a
   , transcription b
   , transcription_document tda
   , transcription_document tdb
where a.chronicle_id = b.chronicle_id
and a.tra_id != b.tra_id                                     -- need different tra_id pairs
and a.is_current                                             -- both tra must be current
and b.is_current
and a.created_ts < b.created_ts                              -- just to ensure we dont get 2 rows in the result for each pair
and a.version_label = '1.0'                                  -- not critical, but we know all candidates are 1.0
and a.tra_id = tda.tra_id                                    -- inner join to its td table
and b.tra_id = tdb.tra_id                                    -- inner join to its td table.
and tda.checksum =tdb.checksum                               -- and their checksums match
order by a.tra_id;

